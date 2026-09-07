/*
 * Derived from Wurst 7 (net.wurstclient.hacks.AutoEatHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor, and reduced to the core behaviour - upstream's forbidden-food list,
 * injury handling and offhand juggling are not carried over. The
 * hunger-threshold logic and the "keep the key held" approach are upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Eats when you are hungry, then puts your item back.
 *
 * <p>Eating is not one action - it is holding right-click for over a second
 * while vanilla runs a use animation. So this is a small state machine: pick
 * food, hold the key, wait for the animation to end, restore the slot. Dropping
 * any of those steps produces the classic bug where the module eats forever, or
 * eats one bite and leaves you holding a carrot.
 *
 * <p>The saved slot is restored rather than remembered as an item: the
 * inventory can shift while eating, and a slot index is the thing that stays
 * meaningful.
 */
public final class AutoEat {
    private static final String MODULE_ID = "auto_eat";

    /** Hunger is 0-20; vanilla stops you eating at all when full. */
    private static final int MAX_HUNGER = 20;

    private static boolean eating;
    private static int previousSlot = -1;

    private AutoEat() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!Settings.enabled(MODULE_ID) || player == null || mc.level == null) {
            stop(mc);
            return;
        }
        if (eating) {
            continueEating(mc, player);
            return;
        }
        double threshold = Settings.number(MODULE_ID, "Eat when hunger below", 16);
        if (player.getFoodData().getFoodLevel() >= Math.min(threshold, MAX_HUNGER - 1)) {
            return;
        }
        int slot = Inventories.indexOf(mc, AutoEat::isFood, 9, false);
        if (slot < 0) {
            return;
        }
        previousSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        eating = true;
        mc.options.keyUse.setDown(true);
    }

    private static void continueEating(Minecraft mc, LocalPlayer player) {
        // isUsingItem goes false the moment the animation completes, which is
        // the only reliable signal that the food was actually consumed.
        boolean stillHungry = player.getFoodData().getFoodLevel() < MAX_HUNGER;
        boolean holdingFood = isFood(player.getMainHandItem());

        if (player.isUsingItem() && stillHungry && holdingFood) {
            return;
        }
        stop(mc);
    }

    /** Releases the key and restores the slot. Safe to call when not eating. */
    public static void stop(Minecraft mc) {
        if (!eating) {
            return;
        }
        eating = false;
        if (mc != null && mc.options != null) {
            mc.options.keyUse.setDown(false);
        }
        if (mc != null && mc.player != null && previousSlot >= 0) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
    }

    private static boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food != null;
    }
}
