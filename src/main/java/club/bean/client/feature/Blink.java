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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Holds your position updates, then releases them all at once.
 *
 * <p>While it is on, the server's copy of you does not move: the packets that
 * would have told it queue up here instead. Switching it off sends the queue in
 * order, and from the server's point of view you cross the whole distance in
 * one go.
 *
 * <p>Only movement packets are held. Holding everything would queue your
 * attacks, your chat and your inventory clicks too, which does not delay them
 * so much as break them - and a few packet types expect a reply, so holding one
 * hangs the connection rather than postponing it.
 *
 * <p>The queue is capped. An unbounded one is a memory leak with a toggle for a
 * trigger, and a queue thousands of packets long takes so long to drain that
 * the server times out anyway.
 */
public final class Blink {
    private static final int MAX_HELD = 512;

    private static final Queue<Packet<?>> HELD = new ArrayDeque<>();
    private static boolean holding;

    private Blink() {
    }

    /**
     * @return true if the packet was taken and should not be sent
     */
    public static boolean hold(Packet<?> packet) {
        if (!holding || !(packet instanceof ServerboundMovePlayerPacket)) {
            return false;
        }
        if (HELD.size() >= MAX_HELD) {
            return false;
        }
        HELD.add(packet);
        return true;
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        boolean wanted = Settings.enabled("blink");
        if (wanted == holding) {
            return;
        }
        holding = wanted;
        if (!holding) {
            release(mc);
        }
    }

    public static int held() {
        return HELD.size();
    }

    /** Sends everything held, in the order it was queued. */
    public static void release(Minecraft mc) {
        holding = false;
        if (mc == null || mc.getConnection() == null) {
            HELD.clear();
            return;
        }
        while (!HELD.isEmpty()) {
            mc.getConnection().send(HELD.poll());
        }
    }
}
