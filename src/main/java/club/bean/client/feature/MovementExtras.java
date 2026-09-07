/*
 * Derived from Wurst 7 (net.wurstclient.hacks.SpiderHack, BunnyHopHack,
 * SafeWalkHack, JesusHack and ElytraFlightHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor rather than Wurst's Hack base class and event manager. SafeWalk is
 * partial - see its javadoc below.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Three small movement modules that share a tick.
 *
 * <p>Grouped because each is a handful of lines and a file apiece would be more
 * ceremony than code. All three are derived from Wurst.
 */
public final class MovementExtras {
    /** Upward velocity Spider holds while against a wall. */
    private static final double CLIMB_SPEED = 0.2;

    /** Upward velocity that holds you at the surface of a liquid. */
    private static final double FLOAT_SPEED = 0.11;
    /** The hop that carries you back out of the water in Walk mode. */
    private static final double HOP_SPEED = 0.30;

    /** Ticks since Jesus last left the water, for the hop cycle. */
    private static int surfaceTicks;

    private static boolean sneaking;

    private MovementExtras() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc, LocalPlayer player) {
        spider(player);
        bunnyHop(player);
        safeWalk(mc, player);
        jesus(mc, player);
        elytraFly(player);
    }

    public static void reset(Minecraft mc) {
        if (sneaking && mc != null && mc.options != null) {
            mc.options.keyShift.setDown(false);
            sneaking = false;
        }
    }

    /**
     * Climbs walls by holding an upward velocity while against one.
     *
     * <p>{@code horizontalCollision} is the whole trigger: it is true precisely
     * when you are pushing into something, so the module needs no block lookup
     * of its own.
     */
    private static void spider(LocalPlayer player) {
        if (!Settings.enabled("spider") || !player.horizontalCollision) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        // Never damp an existing faster climb - only top it up.
        if (velocity.y >= CLIMB_SPEED) {
            return;
        }
        player.setDeltaMovement(velocity.x, CLIMB_SPEED, velocity.z);
    }

    /**
     * Holds you at the surface of water or lava.
     *
     * <p><b>Bob</b> keeps a small upward velocity while you are in the liquid,
     * which floats you at the top of it. <b>Walk</b> adds a two-tick cycle -
     * hop, then cancel the hop - that carries you back out each time you sink
     * in, which is what makes it look like walking rather than swimming.
     *
     * <p>Upstream has a third mode that makes the liquid genuinely solid by
     * overriding the player's water-collision test. That needs a mixin, so it
     * is not here; these two work by writing motion, which the server will
     * disagree with.
     */
    private static void jesus(Minecraft mc, LocalPlayer player) {
        if (!Settings.enabled("jesus") || mc.options.keyShift.isDown()) {
            return;
        }
        boolean inLiquid = player.isInWater() || player.isInLava();
        Vec3 velocity = player.getDeltaMovement();

        if (inLiquid) {
            player.setDeltaMovement(velocity.x, FLOAT_SPEED, velocity.z);
            surfaceTicks = 0;
            return;
        }
        if (Settings.mode("jesus", "Mode", "Walk").toLowerCase(Locale.ROOT).startsWith("bob")) {
            return;
        }
        // Out of the liquid: hop on the first tick, then kill the hop on the
        // next so you come straight back down onto the surface.
        if (surfaceTicks == 0) {
            player.setDeltaMovement(velocity.x, HOP_SPEED, velocity.z);
        } else if (surfaceTicks == 1) {
            player.setDeltaMovement(velocity.x, 0, velocity.z);
        }
        surfaceTicks++;
    }

    /**
     * Flies an elytra at a constant speed and height.
     *
     * <p>Only while you are actually gliding - it does not launch you, and it
     * does not work without an elytra deployed. Vanilla elytra flight trades
     * height for speed; this holds both.
     */
    private static void elytraFly(LocalPlayer player) {
        if (!Settings.enabled("elytra_fly") || !player.isFallFlying()) {
            return;
        }
        double speed = Settings.number("elytra_fly", "Speed", 1.2);
        Vec3 look = Rotations.current(player).toLookVec();
        Vec3 velocity = player.getDeltaMovement();

        if (Settings.flag("elytra_fly", "Hold altitude", true)) {
            player.setDeltaMovement(look.x * speed, 0, look.z * speed);
        } else {
            player.setDeltaMovement(look.x * speed, velocity.y, look.z * speed);
        }
    }

    /** Jumps automatically whenever the chosen condition holds. */
    private static void bunnyHop(LocalPlayer player) {
        if (!Settings.enabled("bunny_hop") || !player.onGround() || player.isShiftKeyDown()) {
            return;
        }
        boolean moving = player.zza != 0 || player.xxa != 0;
        String when = Settings.mode("bunny_hop", "Jump if", "Sprinting").toLowerCase(Locale.ROOT);

        boolean should = when.startsWith("always")
                || (when.startsWith("walking") && moving)
                || (when.startsWith("sprinting") && moving && player.isSprinting());
        if (should) {
            player.jumpFromGround();
        }
    }

    /**
     * Sneaks automatically at the edge of a drop.
     *
     * <p><b>Partial port.</b> Upstream's SafeWalk has a second, better mode that
     * stops you walking off an edge without visibly sneaking, by overriding
     * {@code isStayingOnGroundSurface} on the player - which needs a mixin, and
     * this project has none. What is here is upstream's "Sneak at edges"
     * behaviour: it presses the real sneak key, so other players see you sneak
     * exactly as if you had done it yourself.
     *
     * <p>The test shrinks your bounding box inward by the edge distance and
     * drops it by your step height; if nothing collides with that, there is
     * nothing under you and it is time to sneak.
     */
    private static void safeWalk(Minecraft mc, LocalPlayer player) {
        if (!Settings.enabled("safe_walk") || !player.onGround()) {
            releaseSneak(mc);
            return;
        }
        double edge = Settings.number("safe_walk", "Edge distance", 0.05);
        AABB box = player.getBoundingBox()
                .expandTowards(0, -player.maxUpStep(), 0)
                .inflate(-edge, 0, -edge);

        setSneaking(mc, mc.level.noCollision(player, box));
    }

    private static void releaseSneak(Minecraft mc) {
        if (sneaking) {
            setSneaking(mc, false);
        }
    }

    private static void setSneaking(Minecraft mc, boolean value) {
        if (sneaking == value) {
            return;
        }
        // Only ever release a key this module pressed; the player may be
        // holding sneak themselves.
        mc.options.keyShift.setDown(value);
        sneaking = value;
    }
}
