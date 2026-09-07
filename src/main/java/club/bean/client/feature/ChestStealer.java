/*
 * Derived from Wurst 7 (net.wurstclient.hacks.AutoStealHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor, and routed through vanilla's handleContainerInput. Upstream's
 * buttons in the chest screen and its item filters are not carried over. The
 * one-slot-per-tick pacing is upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Empties an open container into your inventory.
 *
 * <p>Each item is one shift-click, which is the same input a player would send.
 * They go out at a configurable rate rather than all at once: a chest emptied in
 * a single tick is a burst of clicks no hand could produce, and a server that
 * rate-limits container interactions will drop most of them anyway.
 *
 * <h2>Finding the container's own slots</h2>
 *
 * <p>Every container menu ends with the player's 36 inventory slots appended to
 * it, so the container's own contents are everything before that - which is why
 * the loop stops at {@code slots.size() - 36} rather than at a fixed number.
 * That works for a single chest, a double chest, a shulker box and a hopper
 * without knowing which is open.
 */
public final class ChestStealer {
    private static final String MODULE_ID = "chest_stealer";

    /** Every container menu ends with the player's own 36 slots. */
    private static final int PLAYER_SLOTS = 36;

    private static long lastMove;

    private ChestStealer() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!Settings.enabled(MODULE_ID) || player == null || mc.gameMode == null) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        // The player's own inventory is not a container to steal from.
        if (menu == null || menu == player.inventoryMenu) {
            return;
        }
        int containerSlots = menu.slots.size() - PLAYER_SLOTS;
        if (containerSlots <= 0) {
            return;
        }
        double perSecond = Math.max(1, Settings.number(MODULE_ID, "Items per second", 8));
        if (System.currentTimeMillis() - lastMove < 1000.0 / perSecond) {
            return;
        }

        for (int slot = 0; slot < containerSlots; slot++) {
            ItemStack stack = menu.slots.get(slot).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            mc.gameMode.handleContainerInput(menu.containerId, slot, 0,
                    ContainerInput.QUICK_MOVE, player);
            lastMove = System.currentTimeMillis();
            return;
        }
        // Nothing left. Close it, if asked to.
        if (Settings.flag(MODULE_ID, "Close when empty", true)) {
            player.closeContainer();
        }
    }
}
