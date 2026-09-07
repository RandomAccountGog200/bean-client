package club.bean.client.feature;

import club.bean.client.module.Settings;
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
    private static final VanillaOption<Double> GAMMA = new VanillaOption<>(
            "Brightness", "vanilla.gamma",
            () -> Minecraft.getInstance().options.gamma(),
            Double::valueOf);

    private Brightness() {
    }

    /** Puts gamma back if a previous session died while holding it. */
    public static void init() {
        GAMMA.recover();
    }

    private static double level() {
        return Math.max(0.0, Math.min(1.0, Settings.number("brightness", "Level", 1.0)));
    }

    /** Called every client tick. */
    public static void tick() {
        if (!Settings.enabled("brightness")) {
            GAMMA.restore();
            return;
        }
        GAMMA.override(level());
    }

    public static void reset() {
        GAMMA.restore();
    }
}
