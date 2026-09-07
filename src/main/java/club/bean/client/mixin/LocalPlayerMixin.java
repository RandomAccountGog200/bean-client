/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.mixin;

import club.bean.client.module.Settings;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No Slow: removes the movement penalty for using an item.
 *
 * <p>Vanilla scales your input by this multiplier while you are eating,
 * drawing a bow or holding a shield. Returning 1 removes the penalty
 * entirely.
 *
 * <p>Targeting a whole method rather than an instruction inside one is
 * deliberate: an injection anchored to a specific instruction breaks the
 * moment Mojang reorders that method, and fails at startup rather than
 * degrading.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "itemUseSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void beanclient$noSlow(CallbackInfoReturnable<Float> info) {
        if (Settings.enabled("no_slow")) {
            info.setReturnValue(1.0f);
        }
    }
}
