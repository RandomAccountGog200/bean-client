/*
 * Derived from Wurst 7 (net.wurstclient.util.BlockUtils).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: reduced to the subset the ported modules actually use, and
 * rewritten to take the Minecraft instance rather than reading a static one.
 * The interactive-block list is upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Block queries the ported modules share.
 *
 * <p>Nothing here is clever - it is the same handful of questions asked over
 * and over by Scaffold, Nuker and the block-placing path, and having one answer
 * to each is what keeps them agreeing with one another.
 */
public final class Blocks {
    private Blocks() {
    }

    public static BlockState state(Minecraft mc, BlockPos pos) {
        return mc.level.getBlockState(pos);
    }

    public static VoxelShape shape(Minecraft mc, BlockPos pos) {
        return state(mc, pos).getShape(mc.level, pos);
    }

    /** Whether the block has a shape you could actually point at. */
    public static boolean canBeClicked(Minecraft mc, BlockPos pos) {
        return !shape(mc, pos).isEmpty();
    }

    /** How fast the player's current tool breaks this, per tick. */
    public static float hardness(Minecraft mc, BlockPos pos) {
        return state(mc, pos).getDestroyProgress(mc.player, mc.level, pos);
    }

    /** True when nothing solid sits between the two points. */
    public static boolean hasLineOfSight(Minecraft mc, Vec3 from, Vec3 to) {
        ClipContext context = new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
        return mc.level.clip(context).getType() == HitResult.Type.MISS;
    }

    /**
     * Whether right-clicking this block would open something.
     *
     * <p>It matters when placing: clicking a chest places nothing and opens the
     * chest instead, so the placer prefers a different face. Upstream notes that
     * a few blocks change interactivity with context - a lectern only with a
     * book on it, command blocks only for an operator - so this is a good guess
     * rather than a guarantee.
     */
    public static boolean isInteractive(BlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        return block instanceof AbstractChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof AnvilBlock
                || block instanceof CartographyTableBlock
                || block instanceof CraftingTableBlock
                || block instanceof EnchantingTableBlock
                || block instanceof GrindstoneBlock
                || block instanceof LoomBlock
                || block instanceof StonecutterBlock;
    }

}
