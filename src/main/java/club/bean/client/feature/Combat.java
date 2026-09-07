package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Combat tab: Killaura, Trigger Bot, Criticals, Auto Clicker and Reach.
 *
 * <p>All of them end up in the same place - {@link #attack} - which is the only
 * method that actually swings. Routing every module through one call is what
 * keeps the attack cooldown, the crit hop and the swing animation consistent no
 * matter which module decided it was time.
 *
 * <h2>What these actually do</h2>
 *
 * <p>Worth being precise, because it is easy to assume more is happening than
 * is. The attack itself is the same call the vanilla mouse handler makes, so the
 * server sees an ordinary attack packet - there is no forged damage. What the
 * modules change is <em>when</em> that call happens and <em>what it is aimed
 * at</em>:
 *
 * <ul>
 *   <li><b>Killaura</b> picks a target and attacks on a timer instead of
 *       waiting for a click.</li>
 *   <li><b>Trigger Bot</b> waits for your own crosshair to be on something,
 *       which is why it is the tamer of the two.</li>
 *   <li><b>Reach</b> lengthens the client's raycast by hanging a modifier on the
 *       interaction-range attribute. The server keeps its own copy of that
 *       attribute and validates against it, so past vanilla range the attack is
 *       simply dropped. It changes what you can aim at, not what lands.</li>
 *   <li><b>Criticals</b> sends two small position packets before the swing, so
 *       the server's view of you is very slightly airborne at the moment of the
 *       hit. Whether that reads as a crit is the server's decision.</li>
 * </ul>
 *
 * <p>Rotation is real rotation: it writes the player's actual yaw and pitch, so
 * your camera turns and the server is told exactly what you are shown. Nothing
 * here reports a different heading to the server than it draws for you.
 */
public final class Combat {
    private static final AttributeHold REACH = new AttributeHold(
            "reach", Attributes.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_VALUE);

    /** Vanilla only counts a swing as full-strength once the meter refills. */
    private static final float COOLDOWN_READY = 0.92f;

    private static long lastAuraAttack;
    private static long lastTriggerAttack;
    private static long lastAutoClick;

    /** Killaura's current target, so ESP can mark it. */
    private static LivingEntity target;

    private Combat() {
    }

    public static LivingEntity target() {
        return target;
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            target = null;
            return;
        }
        reach(player);
        killaura(mc, player);
        triggerBot(mc, player);
        autoClicker(mc, player);
    }

    /** Drops everything borrowed, for a world change or shutdown. */
    public static void reset(Minecraft mc) {
        target = null;
        if (mc != null && mc.player != null) {
            REACH.clear(mc.player);
        }
    }

    // ---- Reach ------------------------------------------------------------

    private static void reach(LocalPlayer player) {
        if (!Settings.enabled("reach")) {
            REACH.clear(player);
            return;
        }
        REACH.set(player, Settings.number("reach", "Extra blocks", 1.0));
    }

    // ---- Killaura ---------------------------------------------------------

    private static void killaura(Minecraft mc, LocalPlayer player) {
        if (!Settings.enabled("killaura")) {
            target = null;
            return;
        }
        double range = Settings.number("killaura", "Range", 4.2);
        Targets.Filter filter = Targets.Filter.parse(Settings.mode("killaura", "Targets", "Players"));

        target = Targets.nearest(mc, range, filter);
        if (target == null) {
            return;
        }
        if (Settings.flag("killaura", "Rotate", true)) {
            face(player, target);
        }
        long delay = (long) Settings.number("killaura", "Delay", 100);
        if (System.currentTimeMillis() - lastAuraAttack < delay) {
            return;
        }
        // Swinging faster than the cooldown recharges means most hits land for a
        // fraction of their damage, so this is on by default - it hits harder.
        if (Settings.flag("killaura", "Wait for cooldown", true)
                && player.getAttackStrengthScale(0) < COOLDOWN_READY) {
            return;
        }
        lastAuraAttack = System.currentTimeMillis();
        attack(mc, player, target);
    }

    // ---- Trigger Bot ------------------------------------------------------

    private static void triggerBot(Minecraft mc, LocalPlayer player) {
        if (!Settings.enabled("trigger_bot")) {
            return;
        }
        Entity aimed = aimedEntity(mc);
        Targets.Filter filter = Targets.Filter.parse(Settings.mode("trigger_bot", "Targets", "All"));
        if (aimed == null || !Targets.valid(mc, aimed, filter)) {
            return;
        }
        long delay = (long) Settings.number("trigger_bot", "Delay", 150);
        if (System.currentTimeMillis() - lastTriggerAttack < delay) {
            return;
        }
        if (player.getAttackStrengthScale(0) < COOLDOWN_READY) {
            return;
        }
        lastTriggerAttack = System.currentTimeMillis();
        attack(mc, player, aimed);
    }

    // ---- Auto Clicker -----------------------------------------------------

    private static void autoClicker(Minecraft mc, LocalPlayer player) {
        if (!Settings.enabled("auto_clicker")) {
            return;
        }
        // isDown() is false whenever a screen has focus, so this cannot fire
        // while you are in an inventory or in the Bean menu.
        if (!mc.options.keyAttack.isDown()) {
            return;
        }
        double cps = Math.max(1, Settings.number("auto_clicker", "CPS", 10));
        long interval = (long) (1000.0 / cps);
        if (Settings.flag("auto_clicker", "Jitter", true)) {
            // A perfectly even interval is the one thing a hand never produces.
            interval += (long) ((Math.random() - 0.5) * interval * 0.4);
        }
        if (System.currentTimeMillis() - lastAutoClick < interval) {
            return;
        }
        lastAutoClick = System.currentTimeMillis();

        Entity aimed = aimedEntity(mc);
        if (aimed != null) {
            attack(mc, player, aimed);
        } else {
            // Nothing under the crosshair: swing anyway, which is what holding
            // the button does in vanilla.
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    // ---- the one place that swings ----------------------------------------

    /**
     * Attacks {@code victim}, hopping first if Criticals is on.
     *
     * <p>{@code gameMode.attack} is the same entry point the vanilla mouse
     * handler uses; the swing is a separate call because vanilla animates and
     * attacks from two different places.
     */
    private static void attack(Minecraft mc, LocalPlayer player, Entity victim) {
        if (Settings.enabled("criticals")) {
            crit(mc, player);
        }
        mc.gameMode.attack(player, victim);
        player.swing(InteractionHand.MAIN_HAND);
    }

    /**
     * Two position packets that leave the server's copy of you very slightly
     * airborne, which is the state vanilla wants for a critical hit.
     *
     * <p>Only worth doing from the ground - in the air you are already falling,
     * and mid-swim or in a vehicle the extra packets are just noise.
     */
    private static void crit(Minecraft mc, LocalPlayer player) {
        if (!player.onGround() || player.isInWater() || player.isPassenger()) {
            return;
        }
        if (mc.getConnection() == null) {
            return;
        }
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, false));
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
    }

    // ---- helpers ----------------------------------------------------------

    /** The entity your crosshair is actually on, or null. */
    private static Entity aimedEntity(Minecraft mc) {
        HitResult hit = mc.hitResult;
        return hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    /** Points the player at the middle of the entity's box. */
    private static void face(LocalPlayer player, Entity entity) {
        Vec3 from = player.getEyePosition();
        Vec3 to = entity.getBoundingBox().getCenter();
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double flat = Math.sqrt(dx * dx + dz * dz);

        player.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        player.setXRot((float) -Math.toDegrees(Math.atan2(dy, flat)));
    }
}
