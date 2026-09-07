package club.bean.client.module;

import net.minecraft.client.Minecraft;

/**
 * Every module Bean Client ships with — and every one of them works.
 *
 * <p>There is no placeholder list any more. If a row is in the menu, toggling
 * it changes something. The whole set shares one property: it reads state the
 * vanilla client already has and draws it on your own screen, or flips a
 * vanilla option. Nothing is sent to the server, nothing is automated on your
 * behalf, and nothing reveals what the game did not already send you.
 *
 * <p>That is the line, and it is why there is no Combat tab. Aim assistance,
 * movement exploits and see-through-walls rendering are the features that only
 * pay off by taking something from the other people on the server, and they are
 * not going to appear here.
 */
public final class DefaultModules {
    private DefaultModules() {
    }

    private static Module hud(String name, String note) {
        return ModuleRegistry.registerModule(name, Category.HUD, enabled -> {})
                .description(note);
    }

    private static Module module(String name, Category category, Module.ToggleListener onToggle,
                                 String note) {
        return ModuleRegistry.registerModule(name, category, onToggle).description(note);
    }

    public static void registerAll() {
        hudModules();
        visual();
        smp();
        misc();
    }

    // ---- HUD - readouts drawn on your own screen --------------------------

    private static void hudModules() {
        hud("Watermark", "The Bean Client mark in the corner.");

        hud("Module List", "Lists every module you have switched on.")
                .setting(Setting.mode("Corner", "Top right", "Top left"));

        hud("FPS Display", "Your current frame rate.");

        hud("Coordinates", "Your position, and the matching Nether coordinates.")
                .setting(Setting.toggle("Nether conversion", true));

        hud("Ping Display", "Your latency to the server.");

        hud("CPS Counter", "Clicks per second, left and right. Counts clicks you made - it never makes one.");

        hud("Speedometer", "How fast you are actually moving, in blocks per second.");

        hud("Clock", "The real-world time, so you know when to stop.");

        hud("Session Timer", "How long this session has been running.");

        hud("Keystrokes", "WASD, the mouse buttons and jump, lit while held.");

        hud("Armour HUD", "Your armour and held item with durability remaining.");

        hud("Effects HUD", "Your active potion effects and how long they have left.");

        hud("Server Info", "Which server you are on and how many players are online.");
    }

    // ---- Visual - how your own client renders -----------------------------

    private static void visual() {
        module("Fullbright", Category.VISUAL, enabled -> {},
                        "Lifts the brightness floor past the vanilla slider. Lighting only - it cannot "
                                + "show you a block the server did not send.")
                .setting(Setting.slider("Brightness", 0.6, 0.0, 1.0, 2));

        module("Zoom", Category.VISUAL, enabled -> {},
                        "Hold the zoom key to narrow your FOV. The same change as moving the FOV slider.")
                .setting(Setting.slider("Factor", 4, 1, 10, 1))
                .setting(Setting.toggle("Smooth", true));
    }

    // ---- SMP - server quality of life -------------------------------------

    private static void smp() {
        module("Playtime Tracker", Category.SMP, enabled -> {},
                "Counts how long you have spent on each server, and remembers it.");

        module("Death Coords", Category.SMP, enabled -> {},
                "Records where you died and prints it to your own chat.");

        module("Chat Filter", Category.SMP, enabled -> {},
                        "Hides chat you have already seen. A display filter - nothing is sent back.")
                .setting(Setting.toggle("Hide duplicates", true))
                .setting(Setting.toggle("Hide links", false));
    }

    // ---- Misc -------------------------------------------------------------

    private static void misc() {
        module("FPS Limiter", Category.MISC, DefaultModules::applyFrameLimit,
                        "Caps your frame rate, and puts the vanilla setting back when you switch it off.")
                .setting(Setting.slider("Limit", 60, 10, 260, 0));

        module("Toggle Sounds", Category.MISC, enabled -> {},
                "Plays a click when you toggle a module, so you can feel the GUI respond.");
    }

    // ---- the one module that changes a vanilla setting --------------------

    /** Remembers the vanilla cap so turning the module off puts it back. */
    private static int savedFrameLimit = -1;

    private static void applyFrameLimit(boolean enabled) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return;
        }
        Module module = ModuleRegistry.get("fps_limiter");
        if (enabled) {
            if (savedFrameLimit < 0) {
                savedFrameLimit = mc.options.framerateLimit().get();
            }
            int limit = module == null || module.settings().isEmpty()
                    ? 60
                    : (int) Math.round(module.settings().get(0).value());
            mc.options.framerateLimit().set(limit);
        } else if (savedFrameLimit >= 0) {
            mc.options.framerateLimit().set(savedFrameLimit);
            savedFrameLimit = -1;
        }
        mc.options.save();
    }

    /** Re-applies the cap when its slider moves while the module is on. */
    public static void onFrameLimitChanged() {
        Module module = ModuleRegistry.get("fps_limiter");
        if (module != null && module.isEnabled()) {
            applyFrameLimit(true);
        }
    }
}
