/*
 * Derived from Wurst 7 (net.wurstclient.hacks.BowAimbotHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor, and falls back to a straight-line aim rather than upstream's
 * rotation faker when no ballistic solution exists. The trajectory solution and
 * the movement prediction are upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Aims a bow, allowing for drop and for where the target is going.
 *
 * <p>Arrows are not hitscan: they fall, so hitting anything at range means
 * aiming above it, and the further away it is the further above. This solves the
 * angle rather than guessing it.
 *
 * <h2>The ballistics</h2>
 *
 * <p>Given a launch speed {@code v}, a horizontal distance {@code x}, a height
 * difference {@code y} and gravity {@code g}, the launch angle that lands on the
 * target is
 *
 * <pre>{@code
 * theta = atan( (v^2 - sqrt(v^4 - g*(g*x^2 + 2*y*v^2))) / (g*x) )
 * }</pre>
 *
 * <p>Two things fall out of that. The square root goes negative when the target
 * is simply out of range for the current draw, which is why the result is
 * checked for NaN - a half-drawn bow cannot reach as far as a full one. And
 * there are two solutions, a flat shot and a lobbed one; the minus sign picks
 * the flat one, which arrives sooner and is easier to lead.
 *
 * <p>Draw strength matters as much as angle. Vanilla derives arrow speed from
 * how long the bow has been held, and the module reads the same counter, so a
 * shot released early is aimed for the speed it will actually leave at.
 */
public final class BowAimbot {
    private static final String MODULE_ID = "bow_aimbot";

    /** Arrow gravity per tick, and the value vanilla uses. */
    private static final float GRAVITY = 0.006f;

    /** Vanilla's use-item counter starts here and counts down. */
    private static final int USE_TICKS_START = 72000;

    private static LivingEntity target;

    private BowAimbot() {
    }

    public static LivingEntity target() {
        return target;
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!Settings.enabled(MODULE_ID) || player == null || mc.level == null) {
            target = null;
            return;
        }
        ItemStack held = player.getInventory().getSelectedItem();
        Item item = held.getItem();
        if (!(item instanceof BowItem) && !(item instanceof CrossbowItem)) {
            target = null;
            return;
        }
        // A bow only matters while drawn; a crossbow only while loaded.
        if (item instanceof BowItem && !mc.options.keyUse.isDown() && !player.isUsingItem()) {
            target = null;
            return;
        }
        if (item instanceof CrossbowItem && !CrossbowItem.isCharged(held)) {
            target = null;
            return;
        }

        double range = Settings.number(MODULE_ID, "Range", 64);
        Targets.Filter filter = Targets.Filter.parse(Settings.mode(MODULE_ID, "Targets", "Players"));
        if (target == null || !Targets.valid(mc, target, filter)) {
            target = Targets.nearest(mc, range, filter);
        }
        if (target == null) {
            return;
        }
        aim(mc, player, drawStrength(player));
    }

    /**
     * Arrow launch speed, 0-1, from how long the bow has been drawn.
     *
     * <p>The curve is vanilla's: speed rises faster than linearly and is capped
     * at a full draw, which is why a bow held for a second does far more than
     * half the damage of one held for two.
     */
    private static float drawStrength(LocalPlayer player) {
        float ticks = (USE_TICKS_START - player.getUseItemRemainingTicks()) / 20f;
        float velocity = (ticks * ticks + ticks * 2) / 3;
        return Math.min(1f, velocity);
    }

    private static void aim(Minecraft mc, LocalPlayer player, float velocity) {
        double lead = mc.player.getEyePosition().distanceTo(target.getBoundingBox().getCenter())
                * Settings.number(MODULE_ID, "Predict movement", 0.1);

        // Lead the target by its own last-tick velocity, scaled by flight time
        // (approximated by distance) - a stationary target contributes nothing.
        double dx = target.getX() + (target.getX() - target.xOld) * lead - player.getX();
        double dy = target.getY() + (target.getY() - target.yOld) * lead
                + target.getBbHeight() * 0.5 - player.getY()
                - player.getEyeHeight(player.getPose());
        double dz = target.getZ() + (target.getZ() - target.zOld) * lead - player.getZ();

        float neededYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        player.setYRot(Rotations.limitAngleChange(player.getYRot(), neededYaw));

        double flat = Math.sqrt(dx * dx + dz * dz);
        float vSq = velocity * velocity;
        float vPow4 = vSq * vSq;
        double root = vPow4 - GRAVITY * (GRAVITY * flat * flat + 2 * dy * vSq);

        float pitch = (float) -Math.toDegrees(
                Math.atan((vSq - Math.sqrt(root)) / (GRAVITY * flat)));

        if (Float.isNaN(pitch)) {
            // Out of range for this draw: point straight at it, which is the
            // best available answer and becomes correct as the bow finishes
            // drawing.
            Rotations.apply(player, Rotations.needed(player, target.getBoundingBox().getCenter()));
            return;
        }
        player.setXRot(pitch);
    }
}
