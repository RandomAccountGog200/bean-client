/*
 * Derived from Wurst 7 (net.wurstclient.util.RotationUtils).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: the server-look and RotationFaker paths are dropped, since this
 * project never reports a heading different from the one it draws. Upstream
 * measures a turn from the last rotation reported to the server; those fields
 * are private in LocalPlayer and Wurst reaches them through an access widener,
 * which this project does not have, so the current rotation is used instead -
 * see slowlyTurnTowards. The angle maths is upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Working out where to look, and how fast to get there.
 *
 * <p>The second half is the interesting one. Snapping the camera straight onto
 * a target is trivial and is what this client used to do; the result is a
 * player whose head teleports between angles, which looks nothing like a hand
 * on a mouse. {@link #slowlyTurnTowards} instead moves a bounded number of
 * degrees per tick, and splits that budget between yaw and pitch in proportion
 * to how far each has to travel - so the aim arrives on both axes at the same
 * time and traces a straight line rather than an L-shape.
 */
public final class Rotations {
    private Rotations() {
    }

    /** The player's eye position, which is what aiming is measured from. */
    public static Vec3 eyes(LocalPlayer player) {
        return player.position().add(0, player.getEyeHeight(player.getPose()), 0);
    }

    /** The rotation that would point the player's eyes at {@code target}. */
    public static Rotation needed(LocalPlayer player, Vec3 target) {
        Vec3 eyes = eyes(player);

        double diffX = target.x - eyes.x;
        double diffZ = target.z - eyes.z;
        double yaw = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;

        double diffY = target.y - eyes.y;
        double flat = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double pitch = -Math.toDegrees(Math.atan2(diffY, flat));

        return Rotation.wrapped((float) yaw, (float) pitch);
    }

    /** How far the player currently is from facing {@code target}, in degrees. */
    public static double angleTo(LocalPlayer player, Vec3 target) {
        return current(player).angleTo(needed(player, target));
    }

    public static Rotation current(LocalPlayer player) {
        return new Rotation(player.getYRot(), player.getXRot());
    }

    /**
     * The next rotation on the way to {@code end}, moving at most
     * {@code maxChange} degrees this tick.
     *
     * <p>The budget split is the part worth understanding. Giving yaw and pitch
     * a full {@code maxChange} each would let whichever axis has less distance
     * to cover finish early, so the aim swings across and then tilts - two
     * straight segments. Scaling each axis by its share of the total distance
     * makes them finish together.
     *
     * <p>Upstream measures the start angle from the last rotation reported to
     * the server; this uses the current one, which differs by at most a tick.
     */
    public static Rotation slowlyTurnTowards(LocalPlayer player, Rotation end, float maxChange) {
        float startYaw = player.getYRot();
        float startPitch = player.getXRot();

        float yawChange = Math.abs(Mth.wrapDegrees(end.yaw() - startYaw));
        float pitchChange = Math.abs(Mth.wrapDegrees(end.pitch() - startPitch));

        float maxYaw = pitchChange == 0 ? maxChange
                : Math.min(maxChange, maxChange * yawChange / pitchChange);
        float maxPitch = yawChange == 0 ? maxChange
                : Math.min(maxChange, maxChange * pitchChange / yawChange);

        return new Rotation(
                limitAngleChange(startYaw, end.yaw(), maxYaw),
                limitAngleChange(startPitch, end.pitch(), maxPitch));
    }

    /**
     * Moves {@code current} towards {@code intended} by at most
     * {@code maxChange} degrees, the short way round.
     *
     * <p>Do not wrap {@code current} before calling this. The whole point is
     * that the result stays on the same revolution as the input, so repeated
     * calls do not jump by 360 when the angle crosses the wrap point.
     */
    public static float limitAngleChange(float current, float intended, float maxChange) {
        float change = Mth.wrapDegrees(Mth.wrapDegrees(intended) - Mth.wrapDegrees(current));
        return current + Mth.clamp(change, -maxChange, maxChange);
    }

    /** Removes the needless 358-degree spin when crossing the wrap point. */
    public static float limitAngleChange(float current, float intended) {
        return current + Mth.wrapDegrees(Mth.wrapDegrees(intended) - Mth.wrapDegrees(current));
    }

    /** Applies a rotation to the player, taking the short way round on yaw. */
    public static void apply(LocalPlayer player, Rotation rotation) {
        player.setYRot(limitAngleChange(player.getYRot(), rotation.yaw()));
        player.setXRot(rotation.pitch());
    }

    /** Convenience for the common case: turn towards a point in the world. */
    public static void turnTowards(Minecraft mc, LocalPlayer player, Vec3 target, float maxChange) {
        Rotation end = needed(player, target);
        apply(player, maxChange >= 180 ? end : slowlyTurnTowards(player, end, maxChange));
    }
}
