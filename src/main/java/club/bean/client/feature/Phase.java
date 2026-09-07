/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Walks through blocks.
 *
 * <p>No mixin needed: {@code noPhysics} is a public field vanilla already
 * checks, normally for spectators. Setting it turns collision off wholesale.
 *
 * <p>It is also the most obvious thing in the client. The server runs its own
 * collision and will simply refuse to believe you are inside a wall, so expect
 * to be pulled back out. What this genuinely enables is the vertical variant -
 * dropping through a floor - where the client commits to the new position
 * before the server has caught up.
 */
public final class Phase {
    private static boolean applied;

    private Phase() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        boolean wanted = Settings.enabled("phase");
        if (wanted) {
            player.noPhysics = true;
            applied = true;
            // Vanilla only sinks a no-physics entity if it is not flying, so
            // hold it still unless the player asks to move.
            if (!player.getAbilities().flying) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0,
                        player.getDeltaMovement().z);
            }
        } else if (applied) {
            player.noPhysics = false;
            applied = false;
        }
    }

    public static void reset(Minecraft mc) {
        if (applied && mc != null && mc.player != null) {
            mc.player.noPhysics = false;
        }
        applied = false;
    }
}
