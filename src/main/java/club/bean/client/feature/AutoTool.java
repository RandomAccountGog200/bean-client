/*
 * Derived from Wurst 7 (net.wurstclient.hacks.AutoToolHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor. Upstream's silk-touch preference and switch-back behaviour are not
 * carried over. The speed comparison and the repair-mode guard are upstream's.
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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Switches to the fastest tool for whatever you are about to hit.
 *
 * <p>The comparison is just {@code ItemStack.getDestroySpeed(state)} - vanilla's
 * own number, the same one that decides how long mining takes - so there is no
 * table of which tool suits which block. An empty hand scores 1, which is why
 * the search starts there: anything that beats bare hands is worth switching to.
 *
 * <p><b>Repair mode</b> refuses tools below a damage threshold. Losing a
 * near-broken diamond pickaxe to an automatic switch is a bad trade, and the
 * threshold is a percentage of maximum durability rather than a flat number so
 * it behaves the same for every tool.
 */
public final class AutoTool {
    private static final String MODULE_ID = "auto_tool";

    /** An empty hand's destroy speed, and therefore the score to beat. */
    private static final float BARE_HANDS = 1f;

    private AutoTool() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        if (!Settings.enabled(MODULE_ID) || mc.player == null || mc.level == null) {
            return;
        }
        // Only act on what the crosshair is on: switching tools for a block you
        // are merely looking past is churn.
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        equipFor(mc, blockHit.getBlockPos());
    }

    /** Selects the best tool for the block at {@code pos}, if any beats what is held. */
    public static void equipFor(Minecraft mc, BlockPos pos) {
        BlockState state = Blocks.state(mc, pos);
        if (state.isAir()) {
            return;
        }
        int best = bestSlot(mc, state);
        if (best >= 0) {
            Inventories.selectItem(mc, best);
        }
    }

    private static int bestSlot(Minecraft mc, BlockState state) {
        LocalPlayer player = mc.player;
        Inventory inventory = player.getInventory();
        boolean useSwords = Settings.flag(MODULE_ID, "Use swords", false);
        double repairMode = Settings.number(MODULE_ID, "Repair mode", 0);

        ItemStack held = player.getMainHandItem();
        // A held tool that is too damaged scores as bare hands, so anything
        // healthy will beat it and the switch happens.
        float bestSpeed = tooDamaged(held, repairMode) ? BARE_HANDS : speed(held, state);
        int bestSlot = -1;

        for (int slot = 0; slot < 9; slot++) {
            if (slot == inventory.getSelectedSlot()) {
                continue;
            }
            ItemStack stack = inventory.getItem(slot);
            float candidate = speed(stack, state);
            if (candidate <= bestSpeed) {
                continue;
            }
            if (!useSwords && stack.is(ItemTags.SWORDS)) {
                continue;
            }
            if (tooDamaged(stack, repairMode)) {
                continue;
            }
            bestSpeed = candidate;
            bestSlot = slot;
        }
        return bestSlot;
    }

    private static float speed(ItemStack stack, BlockState state) {
        return stack.isEmpty() ? BARE_HANDS : stack.getDestroySpeed(state);
    }

    /**
     * @param repairMode percent of durability below which a tool is spared;
     *                   0 disables the guard entirely
     */
    private static boolean tooDamaged(ItemStack stack, double repairMode) {
        if (repairMode <= 0 || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return false;
        }
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        return remaining * 100.0 / stack.getMaxDamage() <= repairMode;
    }
}
