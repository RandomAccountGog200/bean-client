/*
 * Derived from Wurst 7 (net.wurstclient.hacks.TrajectoriesHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry, and the path is
 * returned for the HUD to project rather than drawn in the world - Minecraft
 * 26.2 removed the world-render hooks this project would have needed. The
 * simulation is upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicts where a thrown item will land.
 *
 * <p>There is no formula for this - the answer depends on what the arrow hits on
 * the way - so it is simulated: step the projectile forward in tenths of a tick,
 * apply drag then gravity, and raycast each step against blocks and entities
 * until something stops it.
 *
 * <p>The order inside the step matters and is vanilla's: move, then apply drag,
 * then apply gravity. Swapping drag and gravity produces a path that is subtly
 * wrong at range, in a way that looks fine until you try to hit something.
 *
 * <p>Different projectiles have different gravity and launch speed, which is why
 * an ender pearl drops so much faster than an arrow.
 */
public final class Trajectories {
    private static final String MODULE_ID = "trajectories";

    /** Steps to simulate before giving up. */
    private static final int MAX_STEPS = 1000;
    /** Fraction of a tick per step. */
    private static final double STEP = 0.1;
    /** Vanilla's per-tick air drag for projectiles. */
    private static final double DRAG = 0.999;

    /** A predicted flight path and what stopped it. */
    public record Path(List<Vec3> points, HitResult.Type ending) {
    }

    private Trajectories() {
    }

    /** @return the path of whatever is in hand, or null if it is not throwable */
    public static Path predict(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!Settings.enabled(MODULE_ID) || player == null || mc.level == null) {
            return null;
        }
        ItemStack held = player.getMainHandItem();
        double gravity = gravityOf(held);
        if (gravity <= 0) {
            return null;
        }
        double speed = launchSpeed(player, held);

        Vec3 position = Rotations.eyes(player);
        Vec3 motion = Rotations.current(player).toLookVec().scale(speed);

        List<Vec3> points = new ArrayList<>();
        HitResult.Type ending = HitResult.Type.MISS;

        for (int step = 0; step < MAX_STEPS; step++) {
            points.add(position);
            Vec3 previous = position;

            position = position.add(motion.scale(STEP));
            motion = motion.scale(DRAG);
            motion = motion.add(0, -gravity * STEP, 0);

            BlockHitResult block = mc.level.clip(new net.minecraft.world.level.ClipContext(
                    previous, position, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            if (block.getType() != HitResult.Type.MISS) {
                points.set(points.size() - 1, block.getLocation());
                ending = HitResult.Type.BLOCK;
                break;
            }
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, previous, position,
                    new AABB(previous, position),
                    (Entity e) -> !e.isSpectator() && e.isPickable(), 64 * 64);
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                points.set(points.size() - 1, hit.getLocation());
                ending = HitResult.Type.ENTITY;
                break;
            }
            if (points.size() > 2 && position.y < mc.level.getMinY() - 16) {
                break;
            }
        }
        return new Path(points, ending);
    }

    /** Per-tick gravity, or 0 for anything that is not thrown. */
    private static double gravityOf(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BowItem || item instanceof CrossbowItem
                || stack.is(Items.TRIDENT)) {
            return 0.05;
        }
        if (stack.is(Items.ENDER_PEARL) || stack.is(Items.SNOWBALL)
                || stack.is(Items.EGG) || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION) || stack.is(Items.EXPERIENCE_BOTTLE)) {
            return 0.03;
        }
        return 0;
    }

    /** Launch speed, which for a bow depends on how far it has been drawn. */
    private static double launchSpeed(LocalPlayer player, ItemStack stack) {
        if (stack.getItem() instanceof BowItem) {
            float ticks = (72000 - player.getUseItemRemainingTicks()) / 20f;
            float velocity = (ticks * ticks + ticks * 2) / 3;
            return Math.min(1f, velocity) * 3.0;
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return 3.15;
        }
        return 1.5;
    }
}
