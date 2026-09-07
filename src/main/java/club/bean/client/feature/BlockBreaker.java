/*
 * Derived from Wurst 7 (net.wurstclient.util.BlockBreaker).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten to take the Minecraft instance rather than reading a
 * static one. Upstream faces the hit vector with a packet-only rotation that
 * leaves the camera where it is; this project does not fake rotations, so
 * callers turn for real or not at all. The face-selection algorithm is
 * upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Comparator;

/**
 * Works out which face of a block to hit, and hits it.
 *
 * <p>The mirror of {@link BlockPlacer}, and simpler: you are aiming at a block
 * that exists, so the only question is which of its six faces to point at.
 *
 * <p>Two preferences: faces you can see beat faces you cannot, and among those,
 * nearest wins. Note that this is the opposite of the placer, which prefers the
 * <em>furthest</em> face - and both are right. Breaking aims at the face turned
 * towards you; placing aims at a neighbour's face turned back towards you, which
 * is the far side of that neighbour.
 */
public final class BlockBreaker {

    /** Everything needed to hit one block. */
    public record Breaking(BlockPos pos, Direction side, Vec3 hitVec,
                           double distanceSq, boolean lineOfSight) {
        public BlockHitResult toHitResult() {
            return new BlockHitResult(hitVec, side, pos, false);
        }
    }

    private BlockBreaker() {
    }

    /**
     * Damages the block at {@code pos} by one tick's worth.
     *
     * <p>Breaking is not instant: {@code continueDestroyBlock} has to be called
     * repeatedly until the block gives way, which is why callers run this every
     * tick rather than once.
     *
     * @param turnSpeed degrees per tick to turn towards the face, or 0 not to turn
     */
    public static boolean breakOneBlock(Minecraft mc, BlockPos pos, float turnSpeed) {
        Breaking breaking = find(mc, pos);
        if (breaking == null) {
            return false;
        }
        if (turnSpeed > 0) {
            Rotations.turnTowards(mc, mc.player, breaking.hitVec(), turnSpeed);
        }
        if (!mc.gameMode.continueDestroyBlock(breaking.pos(), breaking.side())) {
            return false;
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    /** @return how to hit {@code pos}, or null if it has no shape */
    public static Breaking find(Minecraft mc, BlockPos pos) {
        return find(mc, Rotations.eyes(mc.player), pos);
    }

    public static Breaking find(Minecraft mc, Vec3 eyes, BlockPos pos) {
        VoxelShape shape = Blocks.shape(mc, pos);
        if (shape.isEmpty()) {
            return null;
        }
        Direction[] sides = Direction.values();
        Vec3[] hitVecs = new Vec3[sides.length];
        for (int i = 0; i < sides.length; i++) {
            hitVecs[i] = BlockPlacer.faceCentre(pos, shape, sides[i]);
        }

        double distanceSqToCentre = eyes.distanceToSqr(
                Vec3.atLowerCornerOf(pos).add(shape.bounds().getCenter()));
        double[] distancesSq = new double[sides.length];
        boolean[] lineOfSight = new boolean[sides.length];

        for (int i = 0; i < sides.length; i++) {
            distancesSq[i] = eyes.distanceToSqr(hitVecs[i]);
            // Faces further away than the centre are the rear ones; they cannot
            // have line of sight, so the raycast is skipped.
            if (distancesSq[i] >= distanceSqToCentre) {
                continue;
            }
            lineOfSight[i] = Blocks.hasLineOfSight(mc, eyes, hitVecs[i]);
        }

        Direction best = sides[0];
        for (int i = 1; i < sides.length; i++) {
            int current = best.ordinal();
            if (lineOfSight[current] != lineOfSight[i]) {
                if (lineOfSight[i]) {
                    best = sides[i];
                }
                continue;
            }
            if (distancesSq[i] < distancesSq[current]) {
                best = sides[i];
            }
        }
        int chosen = best.ordinal();
        return new Breaking(pos, best, hitVecs[chosen], distancesSq[chosen], lineOfSight[chosen]);
    }

    /** Visible faces first, then nearest - the order Nuker mines in. */
    public static Comparator<Breaking> byPriority() {
        return Comparator.comparing(Breaking::lineOfSight).reversed()
                .thenComparingDouble(Breaking::distanceSq);
    }
}
