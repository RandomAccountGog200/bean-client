/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Shows only the blocks worth seeing, and hides everything else.
 *
 * <p>The rendering decision itself lives in a mixin on the face-culling test;
 * this is the list and the bookkeeping around it.
 *
 * <p>The bookkeeping matters more than it sounds. Chunk geometry is built once
 * and cached, so changing the answer to "should this face be drawn" does
 * nothing until the chunks are rebuilt - which is why toggling asks the
 * renderer to redo them. Forgetting that produces a module that appears not to
 * work until you walk far enough for chunks to reload on their own.
 */
public final class XRay {
    private static final Set<Block> WANTED = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS, Blocks.SPAWNER, Blocks.CHEST,
            Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL,
            Blocks.SHULKER_BOX, Blocks.LAVA, Blocks.WATER);

    private XRay() {
    }

    /**
     * Whether to force a face on or off.
     *
     * @return true to always draw, false to always hide, or null to leave the
     *         decision to vanilla
     */
    public static Boolean shouldRenderFace(BlockState state) {
        if (!Settings.enabled("x_ray")) {
            return null;
        }
        return WANTED.contains(state.getBlock()) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Rebuilds the world so a toggle takes effect immediately.
     *
     * <p>Called from the module's toggle listener. Without it the change only
     * appears as chunks happen to reload.
     */
    public static void onToggle(boolean enabled) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.levelRenderer == null || mc.level == null
                || mc.gameRenderer == null) {
            return;
        }
        mc.levelRenderer.invalidateCompiledGeometry(mc.level, mc.options,
                mc.gameRenderer.mainCamera(), mc.getBlockColors());
    }
}
