package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * The Movement tab: Sprint, Step, Fly, Speed, Velocity and No Fall.
 *
 * <p>Two different mechanisms are at work here, and the difference is the whole
 * reason this file is worth reading.
 *
 * <h2>Attributes</h2>
 *
 * <p>Fly goes through {@link AttributeHold}: gravity is an entity attribute, so
 * changing it is a matter of hanging a transient modifier on the player - no
 * mixin, and the physics that follow are vanilla's own. {@link Step} works the
 * same way in its Simple mode. This is the clean half.
 *
 * <h2>Writing motion directly</h2>
 *
 * <p>Speed, Velocity and the vertical half of Fly instead overwrite
 * {@code deltaMovement} after vanilla has finished computing it. That is much
 * blunter, and it comes with a caveat worth understanding: the server runs its
 * own copy of the movement code and compares. Every one of these is a client
 * telling the server where it went, and a server that checks will notice a
 * position that its own physics could not have produced. That is not a bug to
 * be tuned out - it is what a movement anticheat is.
 *
 * <p>No Fall is the clearest example of the same idea. It does not stop fall
 * damage; it tells the server you are standing on the ground while you are not,
 * and fall damage is computed from what the server believes. Nothing here can
 * make the server believe something it has decided to check for itself.
 */
public final class Movement {
    /** Roughly a vanilla sprint, in blocks per tick - the ceiling Speed scales. */
    private static final double BASE_WALK_SPEED = 0.2806;

    /** ADD_MULTIPLIED_TOTAL with -1 scales the final value to zero. */
    private static final AttributeHold GRAVITY = new AttributeHold(
            "gravity", Attributes.GRAVITY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private Movement() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        Step.tick(mc, player);
        sprint(player);
        // Fly first: it owns the whole delta while it is on, and Speed would
        // otherwise scale the flight velocity it just wrote.
        boolean flying = fly(player);
        if (!flying) {
            speed(player);
        }
        velocity(player);
        noFall(mc, player);
    }

    /** Hands back both attribute holds, for a world change or shutdown. */
    public static void reset(Minecraft mc) {
        if (mc != null && mc.player != null) {
            Step.reset(mc.player);
            GRAVITY.clear(mc.player);
        }
    }

    // ---- Sprint -----------------------------------------------------------

    private static void sprint(LocalPlayer player) {
        if (!Settings.enabled("sprint") || player.input == null) {
            return;
        }
        Input keys = player.input.keyPresses;
        // Only while actually walking forward and not sneaking, which is the
        // same condition vanilla uses for double-tap sprinting. Forcing it in
        // any other state produces a player who is sprinting while standing
        // still, which is both useless and extremely obvious.
        if (keys.forward() && !keys.shift()) {
            player.setSprinting(true);
        }
    }

    // ---- Fly --------------------------------------------------------------

    /** @return true while flight is driving the player's motion */
    private static boolean fly(LocalPlayer player) {
        if (!Settings.enabled("fly")) {
            GRAVITY.clear(player);
            return false;
        }
        GRAVITY.set(player, -1.0);

        double speed = Settings.number("fly", "Speed", 0.5);
        Input keys = player.input == null ? Input.EMPTY : player.input.keyPresses;

        double vertical = 0;
        if (keys.jump()) {
            vertical += speed;
        }
        if (keys.shift()) {
            vertical -= speed;
        }

        Vec3 horizontal = horizontalInput(player, speed);
        player.setDeltaMovement(horizontal.x, vertical, horizontal.z);
        // Without this the moment flight ends you are credited with the whole
        // descent, and the ground kills you.
        player.fallDistance = 0;
        return true;
    }

    /**
     * The player's movement input rotated into world space.
     *
     * <p>Minecraft's forward axis at yaw 0 is {@code (0, +1)} in {@code (x, z)}
     * and yaw increases clockwise, which is why forward contributes
     * {@code -sin} to x and strafe contributes {@code +cos}.
     */
    private static Vec3 horizontalInput(LocalPlayer player, double speed) {
        if (player.input == null) {
            return Vec3.ZERO;
        }
        Vec2 move = player.input.getMoveVector();
        if (move.x == 0 && move.y == 0) {
            return Vec3.ZERO;
        }
        double yaw = Math.toRadians(player.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double x = move.x * cos - move.y * sin;
        double z = move.y * cos + move.x * sin;
        return new Vec3(x * speed, 0, z * speed);
    }

    // ---- Speed ------------------------------------------------------------

    private static void speed(LocalPlayer player) {
        if (!Settings.enabled("speed") || player.input == null) {
            return;
        }
        Vec2 move = player.input.getMoveVector();
        if (move.x == 0 && move.y == 0) {
            return;
        }
        double multiplier = Settings.number("speed", "Multiplier", 1.4);
        Vec3 delta = player.getDeltaMovement();
        double current = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (current < 1.0e-4) {
            return;
        }
        // Clamp to a ceiling rather than scaling the delta.
        //
        // Multiplying every tick looked equivalent and was not: vanilla derives
        // each tick's delta from the previous one through friction, so a x1.4
        // applied repeatedly feeds its own output back in and settles at a far
        // higher speed than the slider asks for - which is why it felt
        // uncontrollable rather than fast. Scaling to a target leaves vanilla's
        // acceleration curve intact and actually honours the number.
        double ceiling = BASE_WALK_SPEED * multiplier;
        if (current <= ceiling) {
            return;
        }
        double scale = ceiling / current;
        player.setDeltaMovement(delta.x * scale, delta.y, delta.z * scale);
    }

    // ---- Velocity ---------------------------------------------------------

    /**
     * Scales knockback down after a hit.
     *
     * <p>{@code hurtTime} counts down from 10, so the first few ticks after
     * damage are when the knockback the server sent is still in the delta.
     * Damping it there is enough without needing to intercept the packet.
     */
    private static void velocity(LocalPlayer player) {
        if (!Settings.enabled("velocity") || player.hurtTime < 8) {
            return;
        }
        double horizontal = Settings.number("velocity", "Horizontal", 0) / 100.0;
        double vertical = Settings.number("velocity", "Vertical", 100) / 100.0;
        Vec3 delta = player.getDeltaMovement();
        player.setDeltaMovement(delta.x * horizontal, delta.y * vertical, delta.z * horizontal);
    }

    // ---- No Fall ----------------------------------------------------------

    private static void noFall(Minecraft mc, LocalPlayer player) {
        if (!Settings.enabled("no_fall") || mc.getConnection() == null) {
            return;
        }
        // Below a couple of blocks there is no damage to avoid, and claiming to
        // be grounded on every airborne tick is a far louder signal than it is
        // worth.
        if (!player.onGround() && player.fallDistance > 2.0) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
        }
    }
}
