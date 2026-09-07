/*
 * Derived from Wurst 7 (net.wurstclient.hacks.AutoArmorHack and
 * net.wurstclient.util.ItemUtils).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor, and routed through vanilla's handleContainerInput. Upstream's
 * enchantment-aware scoring is reduced to armour points plus toughness. The
 * per-slot best-armour search and the free-slot guard are upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Wears the best armour you are carrying.
 *
 * <p>Scoring reads the armour and toughness an item grants straight out of its
 * default attribute modifiers, which is where vanilla itself keeps those
 * numbers - so there is no table of materials to fall out of date when a new
 * one is added.
 *
 * <p>The swap is three container clicks, the same shape as {@code AutoTotem}:
 * pick the new piece up, put it in the armour slot, put the displaced piece
 * back. It refuses when the inventory is full and something is already worn,
 * since the third click would have nowhere to put the old armour and the piece
 * would be left stuck to the cursor.
 */
public final class AutoArmor {
    private static final String MODULE_ID = "auto_armor";
    private static final long COOLDOWN_MS = 250;

    private static long lastSwap;

    private AutoArmor() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!Settings.enabled(MODULE_ID) || player == null || mc.gameMode == null) {
            return;
        }
        // Only against the player's own inventory, or the clicks go to whatever
        // container the server thinks is open.
        if (player.containerMenu != player.inventoryMenu) {
            return;
        }
        if (System.currentTimeMillis() - lastSwap < COOLDOWN_MS) {
            return;
        }
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < Inventories.MAIN_SIZE; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            EquipmentSlot target = armourSlot(candidate);
            if (target == null) {
                continue;
            }
            ItemStack worn = player.getItemBySlot(target);
            if (score(candidate) <= score(worn)) {
                continue;
            }
            // The third click needs somewhere to put what we took off.
            if (!worn.isEmpty() && inventory.getFreeSlot() == -1) {
                continue;
            }
            swap(mc, player, slot, target);
            lastSwap = System.currentTimeMillis();
            return;
        }
    }

    private static void swap(Minecraft mc, LocalPlayer player, int slot, EquipmentSlot target) {
        int container = player.inventoryMenu.containerId;
        int from = Inventories.toNetworkSlot(slot);
        int to = armourNetworkSlot(target);

        click(mc, player, container, from);
        click(mc, player, container, to);
        click(mc, player, container, from);
    }

    private static void click(Minecraft mc, LocalPlayer player, int container, int slot) {
        mc.gameMode.handleContainerInput(container, slot, 0, ContainerInput.PICKUP, player);
    }

    /** Head is container slot 5, feet is 8. */
    private static int armourNetworkSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            default -> 8;
        };
    }

    /** Which armour slot an item belongs in, or null if it is not armour. */
    private static EquipmentSlot armourSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Equippable equippable = stack.getItem().components().get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return null;
        }
        EquipmentSlot slot = equippable.slot();
        return switch (slot) {
            case HEAD, CHEST, LEGS, FEET -> slot;
            default -> null;
        };
    }

    /** Armour points plus toughness. Empty scores zero, so anything beats nothing. */
    private static double score(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return attribute(stack.getItem(), Attributes.ARMOR)
                + attribute(stack.getItem(), Attributes.ARMOR_TOUGHNESS);
    }

    private static double attribute(Item item, Holder<Attribute> attribute) {
        ItemAttributeModifiers modifiers = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0;
        }
        double total = 0;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute() == attribute) {
                total += entry.modifier().amount();
            }
        }
        return total;
    }
}
