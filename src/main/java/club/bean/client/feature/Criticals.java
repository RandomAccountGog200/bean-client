/*
 * Derived from Wurst 7 (net.wurstclient.hacks.CriticalsHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor rather than Wurst's Hack base class and event manager. The three
 * modes and the packet sequence are upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.Locale;

/**
 * Puts the server's copy of you in the air for the instant a hit lands, which
 * is the state vanilla requires for a critical.
 *
 * <p>Derived from Wurst's {@code CriticalsHack}. The previous implementation
 * here sent two position packets and dropped the horizontal-collision flag;
 * both were wrong in ways worth spelling out, because they are the difference
 * between this working and not.
 *
 * <h2>Why four packets, not two</h2>
 *
 * <p>The sequence is up, down, a hair up, down again. The final
 * {@code 1.1e-5} nudge is the point: it is far too small to be a jump, but it
 * leaves the server holding a position that is fractionally above the ground
 * with a downward delta, which is what the critical check actually looks for.
 * Sending a plain up/down pair leaves you flat on the floor by the time the
 * attack arrives.
 *
 * <h2>Why the collision flag matters</h2>
 *
 * <p>{@code ServerboundMovePlayerPacket.Pos} carries a horizontal-collision
 * flag alongside the ground flag, and it must reflect reality. Hardcoding it
 * false while walking into a wall reports a state the server can see is
 * impossible from its own copy of your movement.
 */
public final class Criticals {
    private static final String MODULE_ID = "criticals";

    private Criticals() {
    }

    /**
     * Called immediately before an attack is sent.
     *
     * <p>Only from the ground, and not in liquid: in the air you are already
     * falling and the packets are noise, and in water vanilla refuses criticals
     * outright.
     */
    public static void beforeAttack(Minecraft mc, LocalPlayer player) {
        if (!Settings.enabled(MODULE_ID)) {
            return;
        }
        if (!player.onGround() || player.isInWater() || player.isInLava() || player.isPassenger()) {
            return;
        }
        switch (mode()) {
            case PACKET -> packetJump(mc, player);
            case MINI_JUMP -> miniJump(player);
            case FULL_JUMP -> player.jumpFromGround();
        }
    }

    /** Never actually moves you - only what the server is told. */
    private static void packetJump(Minecraft mc, LocalPlayer player) {
        if (mc.getConnection() == null) {
            return;
        }
        sendOffset(mc, player, 0.0625, true);
        sendOffset(mc, player, 0, false);
        sendOffset(mc, player, 1.1e-5, false);
        sendOffset(mc, player, 0, false);
    }

    private static void sendOffset(Minecraft mc, LocalPlayer player, double offset, boolean onGround) {
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                player.getX(), player.getY() + offset, player.getZ(),
                onGround, player.horizontalCollision));
    }

    /** A hop too small to see, but a real one - you genuinely leave the ground. */
    private static void miniJump(LocalPlayer player) {
        player.push(0, 0.1, 0);
        player.fallDistance = 0.1;
        player.setOnGround(false);
    }

    private static Mode mode() {
        String selected = Settings.mode(MODULE_ID, "Mode", "Packet").toLowerCase(Locale.ROOT);
        if (selected.startsWith("mini")) {
            return Mode.MINI_JUMP;
        }
        return selected.startsWith("full") ? Mode.FULL_JUMP : Mode.PACKET;
    }

    private enum Mode { PACKET, MINI_JUMP, FULL_JUMP }
}
