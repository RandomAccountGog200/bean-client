/*
 * Derived from Wurst 7 (net.wurstclient.util.BlockPlacer and BlockBreaker).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten to take the Minecraft instance rather than reading a
 * static one, and to use vanilla's own interaction manager. Upstream faces the
 * hit vector with a packet-only rotation that leaves the camera where it is;
 * this project does not fake rotations, so callers turn for real or not at all.
 * The face-selection algorithm is upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Works out how to place a block at a position, and places it.
 *
 * <p>You cannot ask Minecraft to put a block somewhere. You can only right-click
 * an existing block's face, and the new block appears next to it - so placing at
 * an empty position means finding a neighbour that is solid, choosing which of
 * its faces to click, and computing the exact point on that face to aim at. That
 * search is the whole of this class.
 *
 * <h2>How the face is chosen</h2>
 *
 * <p>Three preferences, in order, and each exists for a concrete reason:
 *
 * <ol>
 *   <li><b>Non-interactive neighbours first.</b> Clicking a chest opens the
 *       chest instead of placing. Vanilla lets you place against one anyway if
 *       you sneak, but sneaking moves the camera and can break line of sight,
 *       which produces a sneak/unsneak loop that places nothing.</li>
 *   <li><b>Then faces you can actually see.</b> A face you have no line of
 *       sight to is one the server will usually reject.</li>
 *   <li><b>Then the furthest face.</b> Counter-intuitive, but the furthest
 *       usable face is the one pointing back towards you, and a face pointing
 *       away is a rear face that cannot be clicked.</li>
 * </ol>
 *
 * <p>The shortcut at the top is worth knowing too: if the target position holds
 * something replaceable - air, grass, water - you click that block itself rather
 * than a neighbour, and the parameters for doing so are the same ones you would
 * compute to break it.
 */
public final class BlockPlacer {

    /** Everything needed to place one block. */
    public record Placement(BlockPos neighbour, Direction side, Vec3 hitVec,
                            double distanceSq, boolean lineOfSight, boolean requiresSneaking) {
        public BlockHitResult toHitResult() {
            return new BlockHitResult(hitVec, side, neighbour, false);
        }
    }

    private BlockPlacer() {
    }

    /**
     * Places one block at {@code pos}, if a usable face exists.
     *
     * @param turnSpeed degrees per tick to turn towards the face, or 0 not to turn
     * @return true if a placement was attempted
     */
    public static boolean placeOneBlock(Minecraft mc, BlockPos pos, InteractionHand hand,
                                        float turnSpeed) {
        Placement placement = find(mc, pos);
        // Sneaking to place against a container is upstream behaviour this port
        // does not attempt; refusing is better than opening the chest.
        if (placement == null || placement.requiresSneaking()) {
            return false;
        }
        LocalPlayer player = mc.player;
        if (turnSpeed > 0) {
            Rotations.turnTowards(mc, player, placement.hitVec(), turnSpeed);
        }
        mc.gameMode.useItemOn(player, hand, placement.toHitResult());
        player.swing(hand);
        return true;
    }

    /** @return how to place at {@code pos}, or null if there is no usable face */
    public static Placement find(Minecraft mc, BlockPos pos) {
        // Something replaceable already here: click it directly.
        if (Blocks.canBeClicked(mc, pos) && Blocks.state(mc, pos).canBeReplaced()) {
            BlockBreaker.Breaking breaking = BlockBreaker.find(mc, pos);
            if (breaking == null) {
                return null;
            }
            return new Placement(pos, breaking.side(), breaking.hitVec(),
                    breaking.distanceSq(), breaking.lineOfSight(),
                    Blocks.isInteractive(Blocks.state(mc, pos)));
        }

        Direction[] sides = Direction.values();
        Vec3[] hitVecs = new Vec3[sides.length];

        for (int i = 0; i < sides.length; i++) {
            BlockPos neighbour = pos.relative(sides[i]);
            BlockState state = Blocks.state(mc, neighbour);
            VoxelShape shape = state.getShape(mc.level, neighbour);
            // No shape, or replaceable, means nothing to click against.
            if (shape.isEmpty() || state.canBeReplaced()) {
                continue;
            }
            hitVecs[i] = faceCentre(neighbour, shape, sides[i].getOpposite());
        }

        Vec3 eyes = Rotations.eyes(mc.player);
        double distanceSqToCentre = eyes.distanceToSqr(Vec3.atCenterOf(pos));

        double[] distancesSq = new double[sides.length];
        boolean[] lineOfSight = new boolean[sides.length];
        boolean[] interactive = new boolean[sides.length];

        for (int i = 0; i < sides.length; i++) {
            if (hitVecs[i] == null) {
                distancesSq[i] = Double.MAX_VALUE;
                continue;
            }
            distancesSq[i] = eyes.distanceToSqr(hitVecs[i]);
            interactive[i] = Blocks.isInteractive(Blocks.state(mc, pos.relative(sides[i])));

            // A face nearer than the block's centre is on the near side of the
            // neighbour, so placing against it would mean clicking its rear -
            // which can never have line of sight. Skip the raycast.
            if (distancesSq[i] <= distanceSqToCentre) {
                continue;
            }
            lineOfSight[i] = Blocks.hasLineOfSight(mc, eyes, hitVecs[i]);
        }

        Direction best = sides[0];
        for (int i = 1; i < sides.length; i++) {
            if (hitVecs[i] == null) {
                continue;
            }
            int current = best.ordinal();

            if (interactive[current] != interactive[i]) {
                if (interactive[current]) {
                    best = sides[i];
                }
                continue;
            }
            if (lineOfSight[current] != lineOfSight[i]) {
                if (lineOfSight[i]) {
                    best = sides[i];
                }
                continue;
            }
            if (distancesSq[i] > distancesSq[current]) {
                best = sides[i];
            }
        }

        int chosen = best.ordinal();
        if (hitVecs[chosen] == null) {
            return null;
        }
        return new Placement(pos.relative(best), best.getOpposite(), hitVecs[chosen],
                distancesSq[chosen], lineOfSight[chosen], interactive[chosen]);
    }

    /** The middle of one face of a block's shape, in world coordinates. */
    static Vec3 faceCentre(BlockPos pos, VoxelShape shape, Direction face) {
        AABB box = shape.bounds();
        Vec3 halfSize = new Vec3(box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ)
                .scale(0.5);
        Vec3 centre = Vec3.atLowerCornerOf(pos).add(box.getCenter());
        Vec3i unit = face.getUnitVec3i();
        return centre.add(halfSize.x * unit.getX(), halfSize.y * unit.getY(),
                halfSize.z * unit.getZ());
    }
}
