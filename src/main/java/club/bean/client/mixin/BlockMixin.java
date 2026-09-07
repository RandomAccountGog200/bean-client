/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.mixin;

import club.bean.client.feature.XRay;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * X-Ray: decides which block faces the world renderer draws.
 *
 * <p>Vanilla calls this to cull faces you could not see anyway. Answering it
 * differently is what makes stone vanish and ores hang in the air: a face is
 * drawn only when its block is on the wanted list.
 *
 * <p>Changing the answer does not redraw anything by itself - the chunks are
 * already built - which is why toggling the module also asks the renderer to
 * rebuild them.
 */
@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void beanclient$xray(BlockState state, BlockState neighbour, Direction side,
                                        CallbackInfoReturnable<Boolean> info) {
        Boolean forced = XRay.shouldRenderFace(state);
        if (forced != null) {
            info.setReturnValue(forced);
        }
    }
}
