/*
 * Derived from Wurst 7 (net.wurstclient.hacks.StepHack).
 *
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * Copyright (c) 2026 Bean Client contributors.
 *
 * Modified: rewritten against Bean Client's module registry and Settings
 * accessor rather than Wurst's Hack base class and event manager. Simple mode
 * uses this project's AttributeHold instead of a mixin on the step-height
 * getter. The incremental-step sequence in Legit mode is upstream's.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/**
 * Walks up blocks taller than vanilla allows.
 *
 * <p>Two modes, and they work in genuinely different ways.
 *
 * <p><b>Simple</b> raises the player's step-height attribute and lets vanilla's
 * own movement code do the rest. It is this project's original implementation:
 * no packets, no teleport, and whatever physics follow are the ones vanilla
 * would have run anyway. It will step several blocks at once if you ask it to,
 * which is exactly why it looks nothing like a player.
 *
 * <p><b>Legit</b> is derived from Wurst and is the more careful of the two. It
 * measures how far up the obstruction actually goes, checks there is room above
 * to stand, and then moves you there through two intermediate positions at 42%
 * and 75% of the height - an arc rather than a jump to the top. It refuses
 * anything over one block, which is the vanilla limit for a step.
 *
 * <p>The guards on Legit mode are most of its value. It only fires when you are
 * genuinely walking into something on the ground, not swimming, not on a ladder,
 * and not already jumping - each of which vanilla would have handled itself.
 */
public final class Step {
    private static final String MODULE_ID = "step";

    /** A player's own step height, which the attribute's base value carries. */
    private static final double VANILLA_STEP = 0.6;

    /** Where the two intermediate positions sit, as a fraction of the climb. */
    private static final double FIRST_STAGE = 0.42;
    private static final double SECOND_STAGE = 0.753;

    private static final AttributeHold HEIGHT = new AttributeHold(
            "step", Attributes.STEP_HEIGHT, AttributeModifier.Operation.ADD_VALUE);

    private Step() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc, LocalPlayer player) {
        if (!Settings.enabled(MODULE_ID)) {
            HEIGHT.clear(player);
            return;
        }
        if (simple()) {
            HEIGHT.set(player, Settings.number(MODULE_ID, "Height", 1.0) - VANILLA_STEP);
            return;
        }
        // Legit mode must not also be holding the attribute open, or vanilla
        // steps first and there is nothing left to climb.
        HEIGHT.clear(player);
        legitStep(mc, player);
    }

    public static void reset(LocalPlayer player) {
        HEIGHT.clear(player);
    }

    private static void legitStep(Minecraft mc, LocalPlayer player) {
        if (!player.horizontalCollision || !player.onGround()) {
            return;
        }
        if (player.onClimbable() || player.isInWater() || player.isInLava() || player.isJumping()) {
            return;
        }
        if (player.input == null || player.input.getMoveVector().length() <= 1.0e-5f) {
            return;
        }

        // Nudged up slightly and widened, so a block flush against the player
        // still registers as the thing being walked into.
        AABB probe = player.getBoundingBox().move(0, 0.05, 0).inflate(0.05);
        // There has to be somewhere to stand once up there.
        if (!mc.level.noCollision(player, probe.move(0, 1, 0))) {
            return;
        }

        double top = Double.NEGATIVE_INFINITY;
        for (VoxelShape shape : mc.level.getBlockCollisions(player, probe)) {
            if (!shape.isEmpty()) {
                top = Math.max(top, shape.bounds().maxY);
            }
        }
        double climb = top - player.getY();
        if (climb <= 0 || climb > 1) {
            return;
        }

        // Two intermediate positions rather than one jump to the top: the
        // server sees a climb, not a teleport.
        if (mc.getConnection() != null) {
            sendStage(mc, player, climb * FIRST_STAGE);
            sendStage(mc, player, climb * SECOND_STAGE);
        }
        player.setPos(player.getX(), player.getY() + climb, player.getZ());
    }

    private static void sendStage(Minecraft mc, LocalPlayer player, double offset) {
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                player.getX(), player.getY() + offset, player.getZ(),
                player.onGround(), player.horizontalCollision));
    }

    private static boolean simple() {
        return Settings.mode(MODULE_ID, "Mode", "Legit")
                .toLowerCase(Locale.ROOT).startsWith("simple");
    }
}
