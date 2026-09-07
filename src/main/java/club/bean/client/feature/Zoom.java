package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;

/**
 * Hold-to-zoom, done by moving the vanilla FOV slider.
 *
 * <p>This is not a render hook — it writes the FOV option while the key is held
 * and writes the player's own value back when it is released, so the effect is
 * exactly what they would get by dragging the slider themselves. The trade-off
 * is that the vanilla minimum of 30 is the floor, so the practical limit is
 * roughly 2.3x from a default 70 FOV rather than an arbitrary factor.
 *
 * <p>Nothing about the camera is hidden from the server, because the server was
 * never told the FOV in the first place.
 */
public final class Zoom {
    private static final int MIN_FOV = 30;

    private static final VanillaOption<Integer> FOV = new VanillaOption<>(
            "FOV", "vanilla.fov",
            () -> Minecraft.getInstance().options.fov(),
            value -> (int) Math.round(value));

    private static boolean held;

    private Zoom() {
    }

    /** Puts the FOV back if a previous session died mid-zoom. */
    public static void init() {
        FOV.recover();
    }

    public static void setHeld(boolean value) {
        held = value;
    }

    private static double factor() {
        return Math.max(1.0, Settings.number("zoom", "Factor", 2));
    }

    /** Called every client tick. */
    public static void tick() {
        if (!Settings.enabled("zoom") || !held) {
            FOV.restore();
            return;
        }
        Integer base = FOV.baseValue();
        if (base == null) {
            return;
        }
        int target = (int) Math.round(base / factor());
        FOV.override(Math.max(MIN_FOV, Math.min(base, target)));
    }

    /** Drops any override, for when the module is switched off or the world unloads. */
    public static void reset() {
        held = false;
        FOV.restore();
    }
}
