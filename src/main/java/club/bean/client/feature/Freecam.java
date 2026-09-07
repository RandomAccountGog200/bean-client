/*
 * Copyright (c) 2026 Bean Client contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Flies the camera around while your body stays put.
 *
 * <p>Worth being clear about what that means: your player does not move. It
 * keeps standing where you left it, keeps taking damage, and keeps being
 * visible to everyone else. Only the viewpoint leaves, which is why this shows
 * you things without the server having any idea you looked.
 *
 * <p>The camera position is driven here and applied by a mixin at the tail of
 * the camera update, after vanilla has finished putting it on the player.
 */
public final class Freecam {
    private static boolean active;
    private static Vec3 position = Vec3.ZERO;
    private static float yaw;
    private static float pitch;

    private Freecam() {
    }

    public static boolean isActive() {
        return active;
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        boolean wanted = Settings.enabled("freecam") && player != null && mc.level != null;

        if (wanted != active) {
            active = wanted;
            if (active) {
                // Start where the eyes are, so it does not jump on activation.
                position = Rotations.eyes(player);
                yaw = player.getYRot();
                pitch = player.getXRot();
            }
            return;
        }
        if (!active || player == null) {
            return;
        }

        // The player's own rotation is still driven by the mouse, so the camera
        // simply follows it - you steer normally, the body just does not go.
        yaw = player.getYRot();
        pitch = player.getXRot();

        double speed = Settings.number("freecam", "Speed", 1.0);
        Vec3 look = new Rotation(yaw, pitch).toLookVec();
        Vec3 move = Vec3.ZERO;

        if (player.input != null) {
            var keys = player.input.keyPresses;
            if (keys.forward()) {
                move = move.add(look);
            }
            if (keys.backward()) {
                move = move.subtract(look);
            }
            // Strafing is the look vector turned ninety degrees in the
            // horizontal plane, which is (-z, 0, x).
            Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
            if (keys.right()) {
                move = move.add(right);
            }
            if (keys.left()) {
                move = move.subtract(right);
            }
            if (keys.jump()) {
                move = move.add(0, 1, 0);
            }
            if (keys.shift()) {
                move = move.add(0, -1, 0);
            }
        }
        if (move.lengthSqr() > 0) {
            position = position.add(move.normalize().scale(speed));
        }
    }

    /** Called from the camera mixin, after vanilla has positioned it. */
    public static void applyTo(Camera camera) {
        if (!active) {
            return;
        }
        CameraAccess.set(camera, position, yaw, pitch);
    }

    /**
     * Moving the camera needs protected members, so it lives behind this small
     * seam rather than being reached from several places.
     */
    static final class CameraAccess {
        private CameraAccess() {
        }

        static void set(Camera camera, Vec3 pos, float yaw, float pitch) {
            ((club.bean.client.mixin.CameraInvoker) (Object) camera)
                    .beanclient$setRotation(yaw, pitch);
            ((club.bean.client.mixin.CameraInvoker) (Object) camera)
                    .beanclient$setPosition(pos);
        }
    }
}
