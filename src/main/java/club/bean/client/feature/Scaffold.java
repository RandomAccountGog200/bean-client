/*
 * Derived from Wurst 7 (net.wurstclient.hacks.ScaffoldWalkHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor, and built on this project's BlockPlacer rather than Wurst's. The
 * under-the-feet placement approach is upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Puts a block under your feet as you walk off an edge.
 *
 * <p>The interesting part is not the placing - {@link BlockPlacer} does that -
 * but choosing what to hold. Scaffolding with the wrong item is how people
 * accidentally place their shulker boxes into the void, so the search skips
 * anything that is not a plain building block.
 *
 * <p>It also has to be a <em>full</em> block. Placing a torch or a slab under
 * yourself does not hold you up, and the module would keep placing while you
 * fall past it.
 */
public final class Scaffold {
    private static final String MODULE_ID = "scaffold";

    private Scaffold() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!Settings.enabled(MODULE_ID) || player == null || mc.level == null) {
            return;
        }
        BlockPos below = player.blockPosition().below();
        // Something already there: nothing to do.
        if (!Blocks.state(mc, below).canBeReplaced()) {
            return;
        }
        InteractionHand hand = selectBlock(mc);
        if (hand == null) {
            return;
        }
        float turnSpeed = Settings.flag(MODULE_ID, "Rotate", true)
                ? (float) Settings.number(MODULE_ID, "Turn speed", 360) / 20f
                : 0f;
        BlockPlacer.placeOneBlock(mc, below, hand, turnSpeed);
    }

    /**
     * Gets a usable building block into a hand.
     *
     * @return the hand holding one, or null if there is nothing suitable
     */
    private static InteractionHand selectBlock(Minecraft mc) {
        if (isBuildable(mc.player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        int slot = Inventories.indexOf(mc, Scaffold::isBuildable, 9, false);
        if (slot < 0) {
            return null;
        }
        Inventories.selectItem(mc, slot);
        // The selection takes effect immediately for hotbar slots, but the item
        // may not be in hand until next tick if a swap was needed.
        return isBuildable(mc.player.getMainHandItem()) ? InteractionHand.MAIN_HAND : null;
    }

    /** A plain full block, not a slab, torch, container or anything with state. */
    private static boolean isBuildable(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        var state = blockItem.getBlock().defaultBlockState();
        return state.isSolidRender() && !Blocks.isInteractive(state);
    }
}
