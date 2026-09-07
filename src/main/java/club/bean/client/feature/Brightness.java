package club.bean.client.feature;

import club.bean.client.module.Module;
import club.bean.client.module.ModuleRegistry;
import club.bean.client.module.Setting;
import net.minecraft.client.Minecraft;

/**
 * Puts the vanilla Brightness slider on a toggle.
 *
 * <p>Worth being straight about what this is: it drives {@code options.gamma()},
 * which vanilla validates to 0-1, so the ceiling is the game's own "Bright"
 * setting. It is not a true fullbright and will not light a pitch-black cave to
 * daylight — it saves you a trip to the video settings and puts your own value
 * back when you switch it off.
 *
 * <p>Doing better than the vanilla ceiling means injecting into the lightmap,
 * and that is not something this client does.
 */
public final class Brightness {
    private static final VanillaOption<Double> GAMMA =
            new VanillaOption<>("Brightness", () -> Minecraft.getInstance().options.gamma());

    private Brightness() {
    }

    private static Module module() {
        return ModuleRegistry.get("brightness");
    }

    private static double level() {
        Module module = module();
        if (module == null) {
            return 1.0;
        }
        for (Setting setting : module.settings()) {
            if (setting.name().equalsIgnoreCase("Level")) {
                return Math.max(0.0, Math.min(1.0, setting.value()));
            }
        }
        return 1.0;
    }

    /** Called every client tick. */
    public static void tick() {
        Module module = module();
        if (module == null || !module.isEnabled()) {
            GAMMA.restore();
            return;
        }
        GAMMA.override(level());
    }

    public static void reset() {
        GAMMA.restore();
    }
}
