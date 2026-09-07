/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.mixin;

import club.bean.client.feature.InventoryMove;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inventory Move: lets movement keys work while a screen is open.
 *
 * <p>Vanilla clears every key's pressed state when a screen opens, which is
 * why you stop dead when you open a chest. This answers "is it down" from the
 * raw key state instead, but only for the movement bindings and only while a
 * screen is actually up - so typing in chat still types.
 */
@Mixin(KeyMapping.class)
public class KeyMappingMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void beanclient$inventoryMove(CallbackInfoReturnable<Boolean> info) {
        Boolean forced = InventoryMove.isDown((KeyMapping) (Object) this);
        if (forced != null) {
            info.setReturnValue(forced);
        }
    }
}
