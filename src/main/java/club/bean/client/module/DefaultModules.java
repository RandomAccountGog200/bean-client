package club.bean.client.module;

import club.bean.client.BeanClient;

/**
 * The placeholder rows the shell ships with.
 *
 * <p>None of these do anything. Every one is registered with the same
 * {@link #log} listener, which flips the boolean and prints a line. They exist
 * so the GUI has something to lay out, and so the shape of a real registration
 * is obvious - delete a row here, register your own, and nothing else changes.
 */
public final class DefaultModules {
    private DefaultModules() {
    }

    /** The stand-in for real behaviour: flip a boolean, say so, do nothing. */
    private static Module.ToggleListener log(String name) {
        return enabled -> BeanClient.LOGGER.info("[shell] {} -> {}", name, enabled ? "ON" : "OFF");
    }

    private static Module row(String name, Category category, String description) {
        return ModuleRegistry.registerModule(name, category, log(name)).description(description);
    }

    public static void registerAll() {
        combat();
        movement();
        visual();
        player();
        world();
        misc();
    }

    private static void combat() {
        row("Aimbot", Category.COMBAT, "Placeholder row - no aiming happens.")
                .setting(Setting.slider("Range", 3.0, 1.0, 6.0, 1))
                .setting(Setting.slider("FOV", 90, 10, 180, 0))
                .setting(Setting.mode("Target", "Closest", "Lowest health", "Angle"))
                .setting(Setting.toggle("Through walls", false));

        row("Killaura", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("CPS", 8, 1, 20, 0))
                .setting(Setting.mode("Sort", "Distance", "Health", "Armour"))
                .setting(Setting.toggle("Players only", true));

        row("Auto Crystal", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Place range", 4.5, 1.0, 6.0, 1))
                .setting(Setting.slider("Min damage", 6, 0, 20, 0))
                .setting(Setting.toggle("Anti-suicide", true));

        row("Silent Aim", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Smoothing", 0.4, 0.0, 1.0, 2))
                .setting(Setting.toggle("Only while attacking", true));

        row("Hitflick", Category.COMBAT, "Placeholder row.")
                .setting(Setting.slider("Flick angle", 35, 0, 180, 0));

        row("Reach Display", Category.COMBAT, "Placeholder row.")
                .setting(Setting.mode("Units", "Blocks", "Metres"));
    }

    private static void movement() {
        row("Sprint", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.toggle("Keep while sneaking", false))
                .setting(Setting.mode("Mode", "Legit", "Always"));

        row("Speed", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.slider("Multiplier", 1.4, 1.0, 3.0, 2))
                .setting(Setting.mode("Mode", "Vanilla", "Strafe", "Bhop"));

        row("No Slow", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.toggle("Eating", true))
                .setting(Setting.toggle("Shields", true))
                .setting(Setting.toggle("Cobwebs", false));

        row("Step", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.slider("Height", 1.0, 0.5, 2.5, 1));

        row("Velocity", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.slider("Horizontal", 0, 0, 100, 0))
                .setting(Setting.slider("Vertical", 0, 0, 100, 0));

        row("Jesus", Category.MOVEMENT, "Placeholder row.")
                .setting(Setting.mode("Mode", "Solid", "Dolphin"));
    }

    private static void visual() {
        row("Fullbright", Category.VISUAL, "Placeholder row.")
                .setting(Setting.slider("Gamma", 15, 1, 20, 0));

        row("ESP", Category.VISUAL, "Placeholder row.")
                .setting(Setting.mode("Shape", "Box", "Outline", "Glow"))
                .setting(Setting.toggle("Players", true))
                .setting(Setting.toggle("Mobs", false))
                .setting(Setting.toggle("Items", false));

        row("Chams", Category.VISUAL, "Placeholder row.")
                .setting(Setting.slider("Opacity", 0.6, 0.0, 1.0, 2));

        row("Nametags", Category.VISUAL, "Placeholder row.")
                .setting(Setting.slider("Scale", 1.0, 0.5, 3.0, 1))
                .setting(Setting.toggle("Show health", true))
                .setting(Setting.toggle("Show ping", true));

        row("HUD", Category.VISUAL, "Placeholder row.")
                .setting(Setting.toggle("Watermark", true))
                .setting(Setting.toggle("Module list", true))
                .setting(Setting.mode("Corner", "Top left", "Top right", "Bottom left", "Bottom right"));

        row("Zoom", Category.VISUAL, "Placeholder row.")
                .setting(Setting.slider("Factor", 4, 1, 10, 1))
                .setting(Setting.toggle("Smooth camera", true));
    }

    private static void player() {
        row("Auto Tool", Category.PLAYER, "Placeholder row.")
                .setting(Setting.toggle("Avoid breaking tools", true));

        row("Auto Eat", Category.PLAYER, "Placeholder row.")
                .setting(Setting.slider("Hunger threshold", 12, 1, 20, 0));

        row("Fast Place", Category.PLAYER, "Placeholder row.")
                .setting(Setting.slider("Delay", 2, 0, 10, 0));

        row("Inventory Manager", Category.PLAYER, "Placeholder row.")
                .setting(Setting.toggle("Auto sort", true))
                .setting(Setting.toggle("Drop junk", false));

        row("No Fall", Category.PLAYER, "Placeholder row.")
                .setting(Setting.mode("Mode", "Packet", "Edit", "Motion"));

        row("Freecam", Category.PLAYER, "Placeholder row.")
                .setting(Setting.slider("Speed", 1.0, 0.2, 5.0, 1));
    }

    private static void world() {
        row("Chest ESP", Category.WORLD, "Placeholder row.")
                .setting(Setting.slider("Range", 32, 8, 128, 0))
                .setting(Setting.toggle("Trapped chests", true))
                .setting(Setting.toggle("Barrels", true));

        row("Nuker", Category.WORLD, "Placeholder row.")
                .setting(Setting.slider("Radius", 4, 1, 8, 0))
                .setting(Setting.mode("Order", "Nearest", "Top down"));

        row("Scaffold", Category.WORLD, "Placeholder row.")
                .setting(Setting.toggle("Tower", true))
                .setting(Setting.mode("Rotation", "None", "Snap", "Smooth"));

        row("Auto Farm", Category.WORLD, "Placeholder row.")
                .setting(Setting.slider("Range", 4.5, 1.0, 6.0, 1))
                .setting(Setting.toggle("Replant", true));

        row("Xray", Category.WORLD, "Placeholder row.")
                .setting(Setting.slider("Opacity", 0.25, 0.0, 1.0, 2));

        row("Terrain Blend", Category.WORLD, "Placeholder row.")
                .setting(Setting.mode("Blend", "Off", "Soft", "Hard"));
    }

    private static void misc() {
        row("Auto GG", Category.MISC, "Placeholder row.")
                .setting(Setting.slider("Delay", 500, 0, 3000, 0));

        row("Discord RPC", Category.MISC, "Placeholder row.")
                .setting(Setting.toggle("Show server", false))
                .setting(Setting.toggle("Show elapsed time", true));

        row("Announcer", Category.MISC, "Placeholder row.")
                .setting(Setting.mode("Voice", "Classic", "Robot", "Off"));

        row("Client Chat", Category.MISC, "Placeholder row.")
                .setting(Setting.toggle("Prefix messages", true));

        row("FPS Limiter", Category.MISC, "Placeholder row.")
                .setting(Setting.slider("Unfocused cap", 30, 5, 260, 0));

        row("Name Protect", Category.MISC, "Placeholder row.")
                .setting(Setting.toggle("Hide own name", true))
                .setting(Setting.toggle("Hide others", false));
    }
}
