/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.mixin;

import club.bean.client.module.Settings;
import net.minecraft.client.renderer.Lightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fullbright: reports every light level as maximum.
 *
 * <p>This is the real one, as opposed to the Brightness module, which only
 * moves the vanilla gamma slider and therefore stops where the game's own
 * "Bright" setting stops. Here the lightmap itself is told that every light
 * level is full, so an unlit cave renders as if it were daylight.
 */
@Mixin(Lightmap.class)
public class LightmapMixin {
    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private static void beanclient$fullbright(CallbackInfoReturnable<Float> info) {
        if (Settings.enabled("fullbright")) {
            info.setReturnValue(1.0f);
        }
    }
}
