/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.mixin;

import club.bean.client.feature.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freecam: detaches the camera from the player.
 *
 * <p>Injected at the tail, after vanilla has finished positioning the camera
 * on the player, so this simply moves it again. Your body stays where it was
 * and keeps taking damage - the camera is the only thing that leaves.
 */
@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void beanclient$freecam(DeltaTracker delta, CallbackInfo info) {
        Freecam.applyTo((Camera) (Object) this);
    }
}
