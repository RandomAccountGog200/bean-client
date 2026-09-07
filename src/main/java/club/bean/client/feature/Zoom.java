package club.bean.client.feature;

import club.bean.client.module.ModuleRegistry;
import club.bean.client.module.Setting;

/**
 * Hold-to-zoom, the way Optifine does it.
 *
 * <p>This narrows your own camera FOV and nothing else. It does not extend
 * render distance, reveal anything the client was not already drawing, or
 * change what the server sends - it is the same class of change as moving the
 * FOV slider.
 */
public final class Zoom {
    private static boolean held;
    private static float current = 1f;

    private Zoom() {
    }

    public static void setHeld(boolean value) {
        held = value;
    }

    public static boolean isEnabled() {
        var module = ModuleRegistry.get("zoom");
        return module != null && module.isEnabled();
    }

    private static double factorSetting() {
        var module = ModuleRegistry.get("zoom");
        if (module == null) {
            return 4;
        }
        for (Setting setting : module.settings()) {
            if (setting.name().equalsIgnoreCase("Factor")) {
                return Math.max(1.0, setting.value());
            }
        }
        return 4;
    }

    private static boolean smooth() {
        var module = ModuleRegistry.get("zoom");
        if (module == null) {
            return true;
        }
        for (Setting setting : module.settings()) {
            if (setting.name().equalsIgnoreCase("Smooth")) {
                return setting.boolValue();
            }
        }
        return true;
    }

    /** Multiplier applied to the camera FOV this frame. 1 means untouched. */
    public static float factor() {
        float goal = isEnabled() && held ? (float) (1.0 / factorSetting()) : 1f;
        if (!smooth()) {
            current = goal;
        } else {
            current += (goal - current) * 0.4f;
            if (Math.abs(goal - current) < 0.001f) {
                current = goal;
            }
        }
        return current;
    }
}
