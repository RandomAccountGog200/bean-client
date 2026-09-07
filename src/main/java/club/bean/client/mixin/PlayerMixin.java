/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.mixin;

import club.bean.client.module.Settings;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Safe Walk: keeps you on the block you are standing on.
 *
 * <p>This is the test vanilla uses to decide whether to stop you walking off
 * an edge - normally true only while sneaking. Forcing it true gives the
 * sneak-edge behaviour without the sneak, which is what the module's other
 * mode could only approximate by pressing the key for real.
 */
@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void beanclient$safeWalk(CallbackInfoReturnable<Boolean> info) {
        if (Settings.enabled("safe_walk")
                && !Settings.flag("safe_walk", "Visible sneak", false)) {
            info.setReturnValue(true);
        }
    }
}
