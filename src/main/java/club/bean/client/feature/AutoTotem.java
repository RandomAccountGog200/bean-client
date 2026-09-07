package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Keeps a totem in the off hand once your health drops.
 *
 * <p>The interesting part is that this does not move an item - it sends the
 * same three container clicks a player would make, through the same
 * {@code handleContainerInput} the real inventory screen calls. Pick the totem
 * up, put it down in the off hand, put whatever was displaced back. The server
 * is doing the moving; this only asks.
 *
 * <p>Which is also the constraint: the clicks are only legal against the
 * container the server thinks you have open, so this refuses to do anything
 * unless your own inventory is the active menu. Firing container clicks at a
 * chest you happen to be standing in front of would desync the two sides and
 * lose the item.
 */
public final class AutoTotem {
    /** Off hand, in the player's own inventory menu. */
    private static final int OFFHAND_SLOT = 45;

    /** First and last slot worth searching: armour and crafting are skipped. */
    private static final int FIRST_SLOT = 9;
    private static final int LAST_SLOT = 44;

    /** A swap is three round trips; hammering it every tick would desync. */
    private static final long COOLDOWN_MS = 250;

    private static long lastSwap;

    private AutoTotem() {
    }

    /**
     * Clears the cooldown so switching the module on can act immediately
     * rather than waiting out a swap that happened before it was enabled.
     */
    public static void onToggle(boolean enabled) {
        lastSwap = 0;
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        if (!Settings.enabled("auto_totem")) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) {
            return;
        }
        // Only when the player's own inventory is what the server has open.
        if (player.containerMenu != player.inventoryMenu) {
            return;
        }
        if (player.getHealth() > Settings.number("auto_totem", "Health", 10)) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.TOTEM_OF_UNDYING)) {
            return;
        }
        if (System.currentTimeMillis() - lastSwap < COOLDOWN_MS) {
            return;
        }
        int slot = findTotem(player);
        if (slot < 0) {
            return;
        }
        lastSwap = System.currentTimeMillis();

        int container = player.inventoryMenu.containerId;
        click(mc, player, container, slot);
        click(mc, player, container, OFFHAND_SLOT);
        // The third click is what puts a displaced item back rather than
        // leaving it stuck to the cursor.
        click(mc, player, container, slot);
    }

    private static void click(Minecraft mc, LocalPlayer player, int container, int slot) {
        mc.gameMode.handleContainerInput(container, slot, 0, ContainerInput.PICKUP, player);
    }

    private static int findTotem(LocalPlayer player) {
        for (int i = FIRST_SLOT; i <= LAST_SLOT; i++) {
            ItemStack stack = player.inventoryMenu.slots.get(i).getItem();
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return -1;
    }
}
