/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.mixin.KeyMappingAccessor;
import club.bean.client.module.Settings;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Keeps walking while a screen is open.
 *
 * <p>Vanilla releases every key when a screen opens, which is why opening a
 * chest stops you dead. The mixin on {@code KeyMapping.isDown} asks this class
 * first, and this answers from the raw key state for the movement bindings.
 *
 * <p>Two guards keep it from being a nuisance. Chat is excluded, or typing "w"
 * walks you into a hole. And only the movement keys are affected, or every
 * hotbar key and inventory shortcut fires while you are trying to use the
 * screen.
 */
public final class InventoryMove {
    private InventoryMove() {
    }

    /**
     * @return true to force the key down, or null to let vanilla answer
     */
    public static Boolean isDown(KeyMapping key) {
        if (!Settings.enabled("inventory_move")) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null || mc.player == null) {
            return null;
        }
        Screen screen = currentScreen(mc);
        // No screen: vanilla already answers correctly.
        if (screen == null || screen instanceof ChatScreen) {
            return null;
        }
        if (!isMovement(mc, key)) {
            return null;
        }
        // isDown() is exactly what is being overridden, so ask the window
        // about the physical key instead.
        InputConstants.Key bound = ((KeyMappingAccessor) key).beanclient$getKey();
        if (bound == null || bound.getType() != InputConstants.Type.KEYSYM) {
            return null;
        }
        return InputConstants.isKeyDown(mc.getWindow(), bound.getValue());
    }

    private static boolean isMovement(Minecraft mc, KeyMapping key) {
        return key == mc.options.keyUp || key == mc.options.keyDown
                || key == mc.options.keyLeft || key == mc.options.keyRight
                || key == mc.options.keyJump || key == mc.options.keyShift
                || key == mc.options.keySprint;
    }

    private static Screen currentScreen(Minecraft mc) {
        // Minecraft has no public screen field in 26.2; the HUD holds it.
        return mc.gui == null ? null : mc.gui.screen();
    }
}
