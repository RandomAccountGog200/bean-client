package club.bean.client.feature;

import club.bean.client.BeanKeys;
import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/**
 * Throws an ender pearl on a keypress, then puts your item back.
 *
 * <p>Not a Wurst port - upstream has no equivalent - but it is on the requested
 * list, and it is a small piece of state rather than a one-liner. The pearl has
 * to be selected before it can be thrown, and the previous slot restored
 * afterwards, so the throw spans two ticks: select on the first, use and restore
 * on the second. Doing both in one tick throws whatever was in your hand before
 * the swap took effect.
 */
public final class PearlThrow {
    private static final String MODULE_ID = "pearl_key";

    private static int pendingSlot = -1;
    private static int previousSlot = -1;

    private PearlThrow() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!Settings.enabled(MODULE_ID) || player == null || mc.gameMode == null) {
            pendingSlot = -1;
            return;
        }

        // Second tick: the pearl is in hand now, so throw it and restore.
        if (pendingSlot >= 0) {
            if (player.getMainHandItem().is(Items.ENDER_PEARL)) {
                mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.MAIN_HAND);
            }
            if (previousSlot >= 0) {
                player.getInventory().setSelectedSlot(previousSlot);
            }
            pendingSlot = -1;
            previousSlot = -1;
            return;
        }

        if (!BeanKeys.pearl.consumeClick()) {
            return;
        }
        int slot = Inventories.indexOf(mc, Items.ENDER_PEARL, 9, false);
        if (slot < 0) {
            return;
        }
        previousSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        pendingSlot = slot;
    }
}
