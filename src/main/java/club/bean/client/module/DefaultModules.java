package club.bean.client.module;

import club.bean.client.BeanClient;
import net.minecraft.client.Minecraft;

/**
 * The modules the client ships with.
 *
 * <p>Two kinds live here, and the difference is deliberate and visible:
 *
 * <ul>
 *   <li>{@link #real} modules are wired to a listener that changes something.
 *       They read state the vanilla client already has and draw it on your own
 *       screen, or flip a vanilla option. Nothing is sent to the server.
 *   <li>{@link #placeholder} modules flip a boolean and write a line to the log.
 *       They exist so the GUI has a realistic list to lay out.
 * </ul>
 *
 * <p>Combat automation, movement exploits and anything that reveals what the
 * game did not show you are placeholders and stay that way. They are labels in
 * a menu, not features.
 */
public final class DefaultModules {
    private DefaultModules() {
    }

    /** Flips a boolean, says so, does nothing. */
    private static Module placeholder(String name, Category category, String note) {
        return ModuleRegistry.registerModule(name, category,
                        enabled -> BeanClient.LOGGER.info("[shell] {} -> {}", name, enabled ? "ON" : "OFF"))
                .description(note);
    }

    /** Registers a module whose toggle actually does something. */
    private static Module real(String name, Category category, Module.ToggleListener onToggle,
                               String note) {
        return ModuleRegistry.registerModule(name, category, onToggle).description(note).working();
    }

    public static void registerAll() {
        combat();
        movement();
        visual();
        player();
        world();
        smp();
        misc();
    }

    // ---- Combat - every one of these is a placeholder ---------------------

    private static void combat() {
        placeholder("Aimbot", Category.COMBAT, "Placeholder row - no aiming happens.")
                .setting(Setting.slider("Range", 3.0, 1.0, 6.0, 1))
                .setting(Setting.slider("FOV", 90, 10, 180, 0))
                .setting(Setting.mode("Target", "Closest", "Lowest health", "Angle"))
                .setting(Setting.toggle("Through walls", false));

        placeholder("Killaura", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("CPS", 8, 1, 20, 0))
                .setting(Setting.mode("Sort", "Distance", "Health", "Armour"))
                .setting(Setting.toggle("Players only", true));

        placeholder("Auto Totem", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Health threshold", 8, 1, 20, 0))
                .setting(Setting.slider("Delay", 2, 0, 20, 0))
                .setting(Setting.toggle("Keep in offhand", true))
                .setting(Setting.mode("Move mode", "Instant", "Legit", "Packet"));

        placeholder("Auto Crystal", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Place range", 4.5, 1.0, 6.0, 1))
                .setting(Setting.slider("Min damage", 6, 0, 20, 0))
                .setting(Setting.toggle("Anti-suicide", true));

        placeholder("Auto Anchor", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Range", 4.5, 1.0, 6.0, 1))
                .setting(Setting.toggle("Auto glowstone", true));

        placeholder("Auto Armour", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Delay", 3, 0, 20, 0))
                .setting(Setting.toggle("Prefer enchanted", true));

        placeholder("Offhand", Category.COMBAT, "Placeholder row.")
                .setting(Setting.mode("Item", "Totem", "Crystal", "Gapple", "Shield"))
                .setting(Setting.toggle("Swap on low health", true));

        placeholder("Auto Pot", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Health threshold", 10, 1, 20, 0))
                .setting(Setting.mode("Type", "Healing", "Regen", "Both"));

        placeholder("Silent Aim", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Smoothing", 0.4, 0.0, 1.0, 2))
                .setting(Setting.toggle("Only while attacking", true));

        placeholder("Trigger Bot", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Delay", 4, 0, 20, 0))
                .setting(Setting.toggle("Players only", true));

        placeholder("Criticals", Category.COMBAT, "Placeholder row.")
                .setting(Setting.mode("Mode", "Packet", "Jump", "Mini-jump"));

        placeholder("Bow Aimbot", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Prediction", 1.0, 0.0, 3.0, 1))
                .setting(Setting.toggle("Only when drawn", true));

        placeholder("Shield Breaker", Category.COMBAT, "Placeholder row.")
                .setting(Setting.mode("Method", "Axe swap", "Disabler"));

        placeholder("Surround", Category.COMBAT, "Placeholder row.")
                .setting(Setting.toggle("Centre first", true))
                .setting(Setting.slider("Blocks per tick", 4, 1, 8, 0));

        placeholder("Burrow", Category.COMBAT, "Placeholder row.")
                .setting(Setting.mode("Mode", "Instant", "Packet"));

        placeholder("Hitflick", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Flick angle", 35, 0, 180, 0));

        placeholder("Anti Bot", Category.COMBAT, "Placeholder row.")
                .setting(Setting.toggle("Ignore no-armour", true));

        placeholder("Reach Display", Category.COMBAT, "Placeholder row.")
                .setting(Setting.mode("Units", "Blocks", "Metres"));
    }

    private static void movement() {
        placeholder("Sprint", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.toggle("Keep while sneaking", false))
                .setting(Setting.mode("Mode", "Legit", "Always"));

        placeholder("Speed", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.slider("Multiplier", 1.4, 1.0, 3.0, 2))
                .setting(Setting.mode("Mode", "Vanilla", "Strafe", "Bhop"));

        placeholder("No Slow", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.toggle("Eating", true))
                .setting(Setting.toggle("Shields", true))
                .setting(Setting.toggle("Cobwebs", false));

        placeholder("Step", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.slider("Height", 1.0, 0.5, 2.5, 1));

        placeholder("Velocity", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.slider("Horizontal", 0, 0, 100, 0))
                .setting(Setting.slider("Vertical", 0, 0, 100, 0));

        placeholder("Jesus", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.mode("Mode", "Solid", "Dolphin"));
    }

    // ---- Visual - the HUD block is real -----------------------------------

    private static void visual() {
        real("HUD", Category.VISUAL, enabled -> {}, "Watermark, readouts and the enabled-module list.")
                .setting(Setting.toggle("Watermark", true))
                .setting(Setting.toggle("Module list", true))
                .setting(Setting.mode("Corner", "Top right", "Top left"));

        real("FPS Display", Category.VISUAL, enabled -> {}, "Shows your frame rate on the HUD.");

        real("Coordinates", Category.VISUAL, enabled -> {},
                        "Shows your position, and the matching Nether coordinates.")
                .setting(Setting.toggle("Show nether", true));

        real("Ping Display", Category.VISUAL, enabled -> {}, "Shows your latency to the server.");

        placeholder("Fullbright", Category.VISUAL, "Placeholder row.")
                .setting(Setting.slider("Gamma", 15, 1, 20, 0));

        placeholder("ESP", Category.VISUAL, "Placeholder row.")
                .setting(Setting.mode("Shape", "Box", "Outline", "Glow"))
                .setting(Setting.toggle("Players", true))
                .setting(Setting.toggle("Mobs", false));

        placeholder("Chams", Category.VISUAL, "Placeholder row.")
                .setting(Setting.slider("Opacity", 0.6, 0.0, 1.0, 2));

        placeholder("Nametags", Category.VISUAL, "Placeholder row.")
                .setting(Setting.slider("Scale", 1.0, 0.5, 3.0, 1))
                .setting(Setting.toggle("Show health", true));

        placeholder("Zoom", Category.VISUAL, "Placeholder row.")
                .setting(Setting.slider("Factor", 4, 1, 10, 1));
    }

    private static void player() {
        placeholder("Auto Tool", Category.PLAYER, "Placeholder row.")
                .setting(Setting.toggle("Avoid breaking tools", true));

        placeholder("Auto Eat", Category.PLAYER, "Placeholder row.")
                .setting(Setting.slider("Hunger threshold", 12, 1, 20, 0));

        placeholder("Fast Place", Category.PLAYER, "Placeholder row.")
                .setting(Setting.slider("Delay", 2, 0, 10, 0));

        placeholder("Inventory Manager", Category.PLAYER, "Placeholder row.")
                .setting(Setting.toggle("Auto sort", true))
                .setting(Setting.toggle("Drop junk", false));

        placeholder("No Fall", Category.PLAYER, "Placeholder row.")
                .setting(Setting.mode("Mode", "Packet", "Edit", "Motion"));

        placeholder("Freecam", Category.PLAYER, "Placeholder row.")
                .setting(Setting.slider("Speed", 1.0, 0.2, 5.0, 1));
    }

    private static void world() {
        placeholder("Chest ESP", Category.WORLD, "Placeholder row.")
                .setting(Setting.slider("Range", 32, 8, 128, 0))
                .setting(Setting.toggle("Trapped chests", true));

        placeholder("Nuker", Category.WORLD, "Placeholder row.")
                .setting(Setting.slider("Radius", 4, 1, 8, 0))
                .setting(Setting.mode("Order", "Nearest", "Top down"));

        placeholder("Scaffold", Category.WORLD, "Placeholder row.")
                .setting(Setting.toggle("Tower", true))
                .setting(Setting.mode("Rotation", "None", "Snap", "Smooth"));

        placeholder("Auto Farm", Category.WORLD, "Placeholder row.")
                .setting(Setting.slider("Range", 4.5, 1.0, 6.0, 1))
                .setting(Setting.toggle("Replant", true));

        placeholder("Xray", Category.WORLD, "Placeholder row.")
                .setting(Setting.slider("Opacity", 0.25, 0.0, 1.0, 2));

        placeholder("Terrain Blend", Category.WORLD, "Placeholder row.")
                .setting(Setting.mode("Blend", "Off", "Soft", "Hard"));
    }

    // ---- SMP - server quality of life, nothing hidden ---------------------

    private static void smp() {
        real("Session Timer", Category.SMP, enabled -> {}, "How long this session has been running.");

        placeholder("Playtime Tracker", Category.SMP, "Placeholder row.")
                .setting(Setting.toggle("Per server", true));

        placeholder("Death Coords", Category.SMP, "Placeholder row.")
                .setting(Setting.toggle("Copy to clipboard", true));

        placeholder("Waypoints", Category.SMP, "Placeholder row.")
                .setting(Setting.slider("Render distance", 256, 32, 1024, 0))
                .setting(Setting.toggle("Show distance", true));

        placeholder("Inventory Value", Category.SMP, "Placeholder row - would price your own inventory.")
                .setting(Setting.mode("Source", "Auction house", "Shop"))
                .setting(Setting.toggle("Include hotbar", true));

        placeholder("AH Price Lookup", Category.SMP, "Placeholder row.")
                .setting(Setting.toggle("On hover", true));

        placeholder("Auto Reconnect", Category.SMP, "Placeholder row.")
                .setting(Setting.slider("Delay", 5, 1, 60, 0))
                .setting(Setting.slider("Attempts", 10, 1, 100, 0));

        placeholder("Chat Filter", Category.SMP, "Placeholder row.")
                .setting(Setting.toggle("Hide duplicates", true))
                .setting(Setting.toggle("Hide advertisements", true));

        placeholder("Server Stats", Category.SMP, "Placeholder row.")
                .setting(Setting.toggle("Show TPS", true))
                .setting(Setting.toggle("Show player count", true));
    }

    private static void misc() {
        real("FPS Limiter", Category.MISC, DefaultModules::applyFrameLimit,
                        "Caps your frame rate. Actually changes the vanilla setting.")
                .setting(Setting.slider("Limit", 60, 10, 260, 0));

        placeholder("Auto GG", Category.MISC, "Placeholder row.")
                .setting(Setting.slider("Delay", 500, 0, 3000, 0));

        placeholder("Discord RPC", Category.MISC, "Placeholder row.")
                .setting(Setting.toggle("Show server", false));

        placeholder("Announcer", Category.MISC, "Placeholder row.")
                .setting(Setting.mode("Voice", "Classic", "Robot", "Off"));

        placeholder("Client Chat", Category.MISC, "Placeholder row.")
                .setting(Setting.toggle("Prefix messages", true));

        placeholder("Name Protect", Category.MISC, "Placeholder row.")
                .setting(Setting.toggle("Hide own name", true));
    }

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
            savedFrameLimit = savedFrameLimit < 0 ? -1 : savedFrameLimit;
            applyFrameLimit(true);
        }
    }
}
