package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;

/**
 * Caps the frame rate by driving the vanilla Max Framerate option.
 *
 * <p>Minecraft re-reads that option every frame, so writing it is enough - there
 * is nothing to save and nothing to hook. Like {@link Zoom} and
 * {@link Brightness} it goes through {@link VanillaOption}, which is what
 * guarantees the player's own cap comes back afterwards even if the game never
 * gets a clean shutdown.
 */
public final class FrameLimit {
    private static final String MODULE_ID = "fps_limiter";

    private static final VanillaOption<Integer> LIMIT = new VanillaOption<>(
            "Max framerate", "vanilla.framerate_limit",
            () -> Minecraft.getInstance().options.framerateLimit(),
            value -> (int) Math.round(value));

    private FrameLimit() {
    }

    /**
     * Recovers a cap left behind by a session that did not exit cleanly, then
     * applies the module's own state. Called once on the first client tick,
     * because a toggle restored from the config never fires its listener.
     */
    public static void init() {
        LIMIT.recover();
        apply();
    }

    /** Applies the cap, or hands the option back. Fires on toggle and on slider moves. */
    public static void apply() {
        if (!Settings.enabled(MODULE_ID)) {
            LIMIT.restore();
            return;
        }
        LIMIT.override(Settings.integer(MODULE_ID, "Limit", 60));
    }

    /** Drops the cap, for shutdown. */
    public static void reset() {
        LIMIT.restore();
    }
}
