/*
 * Derived from Wurst 7 (net.wurstclient.hacks.NukerHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor, and built on this project's BlockBreaker. Upstream's ID filter,
 * flat-nuke and smash modes are not carried over. The nearest-first ordering and
 * the one-block-per-tick pacing are upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Breaks every block within a radius, nearest first.
 *
 * <p>Only one block is worked on per tick, because that is how vanilla breaking
 * works: {@code continueDestroyBlock} adds a tick of damage and the block gives
 * way when enough have accumulated. Spreading the effort over several blocks at
 * once just means none of them finish.
 *
 * <p>Ordering is nearest-first, with visible faces preferred - see
 * {@link BlockBreaker#byPriority()}. Bedrock and other unbreakable blocks are
 * skipped by checking the destroy progress rather than by name, so it needs no
 * list to maintain.
 */
public final class Nuker {
    private static final String MODULE_ID = "nuker";

    /** Guards against a huge radius turning into a very long loop. */
    private static final int MAX_RANGE = 6;

    private Nuker() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!Settings.enabled(MODULE_ID) || player == null || mc.level == null) {
            return;
        }
        int range = Math.min(MAX_RANGE, Settings.integer(MODULE_ID, "Range", 4));
        double rangeSq = (double) range * range;
        Vec3 eyes = Rotations.eyes(player);
        BlockPos origin = player.blockPosition();

        List<BlockBreaker.Breaking> targets = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-range, -range, -range), origin.offset(range, range, range))) {

            if (Vec3.atCenterOf(pos).distanceToSqr(eyes) > rangeSq) {
                continue;
            }
            BlockState state = Blocks.state(mc, pos);
            if (state.isAir() || state.canBeReplaced()) {
                continue;
            }
            // Zero destroy progress means it cannot be broken at all - bedrock,
            // barriers, or a block this tool simply cannot touch.
            if (Blocks.hardness(mc, pos) <= 0) {
                continue;
            }
            BlockBreaker.Breaking breaking = BlockBreaker.find(mc, eyes, pos.immutable());
            if (breaking != null) {
                targets.add(breaking);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        targets.sort(BlockBreaker.byPriority());
        BlockBreaker.Breaking best = targets.get(0);

        if (Settings.flag(MODULE_ID, "Auto tool", true)) {
            AutoTool.equipFor(mc, best.pos());
        }
        float turnSpeed = Settings.flag(MODULE_ID, "Rotate", true)
                ? (float) Settings.number(MODULE_ID, "Turn speed", 360) / 20f
                : 0f;
        BlockBreaker.breakOneBlock(mc, best.pos(), turnSpeed);
    }
}
