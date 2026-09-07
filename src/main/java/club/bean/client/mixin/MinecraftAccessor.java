/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Fast Place: reaches the right-click cooldown.
 *
 * <p>An accessor rather than an injection - it changes no behaviour of its
 * own, it only exposes a private field so the module can zero the delay
 * vanilla puts between block placements.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("rightClickDelay")
    void beanclient$setRightClickDelay(int delay);

    @Accessor("rightClickDelay")
    int beanclient$getRightClickDelay();
}
