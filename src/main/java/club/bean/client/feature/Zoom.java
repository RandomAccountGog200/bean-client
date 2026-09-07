package club.bean.client.feature;

import club.bean.client.module.Module;
import club.bean.client.module.ModuleRegistry;
import club.bean.client.module.Setting;
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

    private static final VanillaOption<Integer> FOV =
            new VanillaOption<>("FOV", () -> Minecraft.getInstance().options.fov());

    private static boolean held;

    private Zoom() {
    }

    public static void setHeld(boolean value) {
        held = value;
    }

    private static boolean isEnabled() {
        Module module = ModuleRegistry.get("zoom");
        return module != null && module.isEnabled();
    }

    private static double factor() {
        Module module = ModuleRegistry.get("zoom");
        if (module == null) {
            return 2;
        }
        for (Setting setting : module.settings()) {
            if (setting.name().equalsIgnoreCase("Factor")) {
                return Math.max(1.0, setting.value());
            }
        }
        return 2;
    }

    /** Called every client tick. */
    public static void tick() {
        if (!isEnabled() || !held) {
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
