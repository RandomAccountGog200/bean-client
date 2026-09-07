/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.mixin.MinecraftAccessor;
import club.bean.client.mixin.MultiPlayerGameModeAccessor;
import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;

/**
 * Removes the cooldowns between placing and breaking blocks.
 *
 * <p>Both are single private counters that vanilla decrements each tick, so
 * both modules are the same one line: set the counter to zero before the game
 * gets a chance to look at it. There is no cleverness here at all, which is why
 * they share a file.
 */
public final class FastAction {
    private FastAction() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        if (Settings.enabled("fast_place")) {
            ((MinecraftAccessor) mc).beanclient$setRightClickDelay(0);
        }
        if (Settings.enabled("fast_break") && mc.gameMode != null) {
            ((MultiPlayerGameModeAccessor) mc.gameMode).beanclient$setDestroyDelay(0);
        }
    }
}
