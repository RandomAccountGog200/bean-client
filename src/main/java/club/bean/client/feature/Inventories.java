/*
 * Derived from Wurst 7 (net.wurstclient.util.InventoryUtils).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: reduced to the subset the ported modules use, rewritten to take the
 * Minecraft instance rather than reading a static one, and routed through
 * vanilla's handleContainerInput instead of Wurst's mixin interface. The slot
 * numbering and the selection strategy are upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Finding items and getting them into your hand.
 *
 * <h2>Two different slot numberings</h2>
 *
 * <p>This trips everyone up once. The player's <em>inventory</em> indexes the
 * hotbar 0-8, the main inventory 9-35, armour 36-39 and the off hand 40. The
 * <em>container</em> the server knows about numbers the same slots completely
 * differently: main inventory 9-35, hotbar 36-44, armour 5-8, off hand 45.
 *
 * <p>Read a stack with an inventory index; send a click with a container slot.
 * {@link #toNetworkSlot} converts, and mixing them up produces the classic bug
 * where a module reliably grabs the wrong item.
 */
public final class Inventories {
    /** Inventory indices 0-35: hotbar plus main inventory. */
    public static final int MAIN_SIZE = 36;
    /** Inventory index of the off hand. */
    public static final int OFFHAND = 40;

    private Inventories() {
    }

    /**
     * The inventory index of the first matching item, or -1.
     *
     * @param maxSlot        exclusive; 9 for hotbar only, 36 for the whole inventory
     * @param includeOffhand also check the off hand, whatever {@code maxSlot} is
     */
    public static int indexOf(Minecraft mc, Predicate<ItemStack> match,
                              int maxSlot, boolean includeOffhand) {
        Inventory inventory = mc.player.getInventory();
        for (int slot = 0; slot < maxSlot; slot++) {
            if (match.test(inventory.getItem(slot))) {
                return slot;
            }
        }
        if (includeOffhand && match.test(inventory.getItem(OFFHAND))) {
            return OFFHAND;
        }
        return -1;
    }

    public static int indexOf(Minecraft mc, Item item, int maxSlot, boolean includeOffhand) {
        return indexOf(mc, stack -> stack.is(item), maxSlot, includeOffhand);
    }

    public static int count(Minecraft mc, Predicate<ItemStack> match) {
        Inventory inventory = mc.player.getInventory();
        int total = 0;
        for (int slot = 0; slot < MAIN_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (match.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Gets the item in {@code slot} into the selected hotbar slot.
     *
     * <p>Three cases, cheapest first: already in the hotbar, so just change the
     * selection; there is a free hotbar slot, so shift-click it across; or the
     * hotbar is full, so swap it with whatever is currently held.
     *
     * @return true if something was done. The item may still be in transit -
     *         the server has to acknowledge the click - so callers should not
     *         assume the next line already sees it in hand.
     */
    public static boolean selectItem(Minecraft mc, int slot) {
        if (slot < 0) {
            return false;
        }
        LocalPlayer player = mc.player;
        Inventory inventory = player.getInventory();

        if (slot < 9) {
            inventory.setSelectedSlot(slot);
            return true;
        }
        int container = player.inventoryMenu.containerId;
        int free = inventory.getFreeSlot();

        if (free > -1 && free < 9) {
            mc.gameMode.handleContainerInput(container, toNetworkSlot(slot), 0,
                    ContainerInput.QUICK_MOVE, player);
        } else {
            // SWAP's button argument is the destination hotbar index.
            mc.gameMode.handleContainerInput(container, toNetworkSlot(slot),
                    inventory.getSelectedSlot(), ContainerInput.SWAP, player);
        }
        return true;
    }

    /** Convenience: find the item and select it in one call. */
    public static boolean selectItem(Minecraft mc, Predicate<ItemStack> match,
                                     int maxSlot, boolean includeOffhand) {
        return selectItem(mc, indexOf(mc, match, maxSlot, includeOffhand));
    }

    /** Inventory index to the slot number the server uses. */
    public static int toNetworkSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            return slot + 36;
        }
        if (slot >= 36 && slot < 40) {
            return 44 - slot;
        }
        if (slot == OFFHAND) {
            return 45;
        }
        return slot;
    }
}
