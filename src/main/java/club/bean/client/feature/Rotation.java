/*
 * Derived from Wurst 7 (net.wurstclient.util.Rotation).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: dropped the RotationFaker integration and the quaternion helper,
 * which this project has no use for. The look-vector and angle maths are
 * upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A yaw/pitch pair, and the maths for comparing two of them.
 *
 * <p>Worth having as a type rather than two loose floats, because angle
 * arithmetic is where aiming code goes wrong. Yaw wraps at 180, so the
 * difference between 179 and -179 is two degrees, not 358 - and getting that
 * backwards produces an aim that spins the long way round.
 *
 * @param yaw   horizontal angle, degrees
 * @param pitch vertical angle, degrees; negative is up
 */
public record Rotation(float yaw, float pitch) {

    /** Wraps both angles into -180..180. */
    public static Rotation wrapped(float yaw, float pitch) {
        return new Rotation(Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch));
    }

    /** Angular distance to another rotation, in degrees. */
    public double angleTo(Rotation other) {
        float diffYaw = Mth.wrapDegrees(Mth.wrapDegrees(yaw) - Mth.wrapDegrees(other.yaw));
        float diffPitch = Mth.wrapDegrees(Mth.wrapDegrees(pitch) - Mth.wrapDegrees(other.pitch));
        return Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
    }

    /** The unit vector this rotation looks along. */
    public Vec3 toLookVec() {
        float adjustedYaw = -Mth.wrapDegrees(yaw) * Mth.DEG_TO_RAD - Mth.PI;
        float cosYaw = Mth.cos(adjustedYaw);
        float sinYaw = Mth.sin(adjustedYaw);

        float adjustedPitch = -Mth.wrapDegrees(pitch) * Mth.DEG_TO_RAD;
        float nCosPitch = -Mth.cos(adjustedPitch);
        float sinPitch = Mth.sin(adjustedPitch);

        return new Vec3(sinYaw * nCosPitch, sinPitch, cosYaw * nCosPitch);
    }
}
