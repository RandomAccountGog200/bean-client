/*
 * Derived from Wurst 7 (net.wurstclient.hacks.AimAssistHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor. Upstream's integrations with its own AutoSword, SpearAssist and
 * external-target plumbing are dropped, as is the fake-rotation path - this
 * project never reports a heading different from the one it draws. Target
 * selection, the degrees-per-second turn rate and the line-of-sight gate are
 * upstream's.
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Pulls the crosshair towards a target rather than snapping to it.
 *
 * <p>The difference from Killaura is that this only aims - it never attacks.
 * You still click. That makes the turn rate the entire point of the module: at
 * 720 degrees per second it is a lock-on, and at 60 it is a nudge that a hand
 * could plausibly have produced.
 *
 * <p>The rate is expressed per second and divided by 20 to get a per-tick
 * budget, which is upstream's convention and is why the slider reads in
 * hundreds.
 *
 * <p>Sticky targeting: once a target is picked it is kept while it stays valid,
 * rather than re-choosing the nearest every tick. Without that, two players
 * standing near each other make the aim flick between them.
 */
public final class AimAssist {
    private static final String MODULE_ID = "aim_assist";

    /** Ticks per second, for converting the turn-rate slider. */
    private static final float TICKS_PER_SECOND = 20f;

    private static LivingEntity target;

    private AimAssist() {
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
        // Aiming for you while you are eating or drawing a bow fights the thing
        // you are actually doing.
        if (player.isUsingItem() && !Settings.flag(MODULE_ID, "While using items", false)) {
            target = null;
            return;
        }

        double range = Settings.number(MODULE_ID, "Range", 6);
        Targets.Filter filter = Targets.Filter.parse(Settings.mode(MODULE_ID, "Targets", "Players"));

        // Keep the current target while it is still valid - see class javadoc.
        if (!stillValid(mc, player, target, range, filter)) {
            target = Targets.nearest(mc, range, filter);
        }
        if (target == null) {
            return;
        }
        Vec3 aimPoint = target.getBoundingBox().getCenter();
        if (Settings.flag(MODULE_ID, "Check line of sight", true)
                && !hasLineOfSight(mc, player, aimPoint)) {
            target = null;
            return;
        }
        // The FOV gate is what stops it dragging your view onto someone behind
        // you; without it the module is just a slow lock-on.
        double fov = Settings.number(MODULE_ID, "FOV", 120);
        if (fov < 360 && Rotations.angleTo(player, aimPoint) > fov / 2) {
            target = null;
            return;
        }

        float perTick = (float) Settings.number(MODULE_ID, "Turn speed", 180) / TICKS_PER_SECOND;
        Rotations.turnTowards(mc, player, aimPoint, perTick);
    }

    private static boolean stillValid(Minecraft mc, LocalPlayer player, LivingEntity candidate,
                                      double range, Targets.Filter filter) {
        if (candidate == null || !Targets.valid(mc, candidate, filter)) {
            return false;
        }
        double reach = range * range;
        return candidate.getBoundingBox().getCenter()
                .distanceToSqr(Rotations.eyes(player)) <= reach;
    }

    /** True when nothing solid sits between the player's eyes and the point. */
    static boolean hasLineOfSight(Minecraft mc, LocalPlayer player, Vec3 target) {
        ClipContext context = new ClipContext(Rotations.eyes(player), target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        return mc.level.clip(context).getType() == HitResult.Type.MISS;
    }
}
