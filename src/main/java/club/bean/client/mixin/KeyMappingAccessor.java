/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Inventory Move: reaches which physical key a binding is bound to.
 *
 * <p>{@code KeyMapping.isDown()} is false while a screen is open, which is the
 * behaviour being worked around, so the module has to ask the operating system
 * about the key directly - and for that it needs the key itself, which is
 * protected.
 */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("key")
    InputConstants.Key beanclient$getKey();
}
