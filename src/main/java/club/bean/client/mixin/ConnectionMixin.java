/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.mixin;

import club.bean.client.feature.Blink;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blink: holds outgoing packets instead of sending them.
 *
 * <p>While it is on, the server's copy of you stays where you were: your
 * position updates queue up locally. Switching it off releases the queue, and
 * the server sees you cover the whole distance at once.
 *
 * <p>Only the single-argument send is intercepted. The overloads that take a
 * channel listener are used for packets that expect a reply, and holding one
 * of those back would hang rather than delay.
 */
@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"), cancellable = true)
    private void beanclient$blink(Packet<?> packet, CallbackInfo info) {
        if (Blink.hold(packet)) {
            info.cancel();
        }
    }
}
