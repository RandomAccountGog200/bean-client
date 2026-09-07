package club.bean.client.feature;

import club.bean.client.module.ModuleRegistry;
import club.bean.client.module.Setting;

/**
 * Raises the ambient floor of the lightmap so caves are readable.
 *
 * <p>The same class of change as the vanilla Brightness slider, without the
 * slider's ceiling. It cannot show you a block you were not already being sent;
 * it only changes how the light you already have is shaded.
 */
public final class Fullbright {
    private Fullbright() {
    }

    public static boolean isActive() {
        var module = ModuleRegistry.get("fullbright");
        return module != null && module.isEnabled();
    }

    /** 0 = vanilla, 1 = flat white ambient. */
    public static float ambientLevel() {
        var module = ModuleRegistry.get("fullbright");
        if (module == null) {
            return 0f;
        }
        for (Setting setting : module.settings()) {
            if (setting.name().equalsIgnoreCase("Brightness")) {
                return (float) setting.value();
            }
        }
        return 0.6f;
    }
}
