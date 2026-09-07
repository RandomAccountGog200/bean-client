/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Freecam: reaches the camera's own positioning methods.
 *
 * <p>An invoker rather than an injection - it adds no behaviour, it only makes
 * two protected methods callable so Freecam can move the camera using the same
 * calls vanilla uses rather than reimplementing them.
 */
@Mixin(Camera.class)
public interface CameraInvoker {
    @Invoker("setRotation")
    void beanclient$setRotation(float yaw, float pitch);

    @Invoker("setPosition")
    void beanclient$setPosition(Vec3 position);
}
