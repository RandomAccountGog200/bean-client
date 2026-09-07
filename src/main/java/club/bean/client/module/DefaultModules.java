package club.bean.client.module;

import club.bean.client.feature.AutoTotem;
import club.bean.client.feature.Brightness;
import club.bean.client.feature.ChatFilter;
import club.bean.client.feature.Combat;
import club.bean.client.feature.XRay;
import club.bean.client.feature.FrameLimit;
import club.bean.client.feature.Movement;
import club.bean.client.feature.Trackers;
import club.bean.client.feature.Zoom;
import club.bean.client.gui.Draw;

/**
 * Every module Bean Client ships with — and every one of them works.
 *
 * <p>This file is only the registration table. What each row actually does
 * lives next door in {@code club.bean.client.feature} (the stateful ones) or in
 * {@code club.bean.client.hud.ClientHud} (the readouts, which are pure draw
 * code gated on the toggle). Nothing here is a placeholder - if a row is in the
 * menu, toggling it changes something.
 *
 * <p>The rows fall into two groups, and the split is worth knowing before
 * reading any of them.
 *
 * <p><b>HUD, Visual, SMP and Misc</b> only read state the vanilla client
 * already holds and draw it on your own screen, or move a vanilla option you
 * could have moved yourself. Nothing is sent to the server and nothing is
 * automated on your behalf.
 *
 * <p><b>Combat, Movement and Render</b> do not respect that line. They act on
 * your behalf, they show you things the client had decided not to draw, and
 * several of them tell the server something that is not true. Any server
 * running an anticheat is looking for exactly these. They are here because they
 * were asked for; that does not make them a good idea.
 *
 * <p>What has not changed is that there are still <b>no mixins</b>. Every one of
 * these goes through a public API - an entity attribute, a packet the client
 * already sends, or a projection onto the HUD - which is a real constraint and
 * shapes what they can do. Reach is the clearest case: it lengthens the client
 * raycast, and the server throws the result away.
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
        combat();
        movement();
        render();
        player();
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

    // ---- Combat -----------------------------------------------------------

    private static void combat() {
        module("Killaura", Category.COMBAT, enabled -> {},
                        "Attacks the nearest target on a timer. Rotation is real - your camera turns.")
                .setting(Setting.slider("Range", 4.2, 3, 6, 1))
                .setting(Setting.slider("Delay", 100, 0, 1000, 0))
                .setting(Setting.mode("Targets", "Players", "Mobs", "All"))
                .setting(Setting.toggle("Rotate", true))
                .setting(Setting.slider("Turn speed", 720, 20, 720, 0))
                .setting(Setting.toggle("Wait for cooldown", true));

        module("Trigger Bot", Category.COMBAT, enabled -> {},
                        "Attacks whatever your crosshair is already on. You still aim it.")
                .setting(Setting.slider("Delay", 150, 0, 1000, 0))
                .setting(Setting.mode("Targets", "All", "Players", "Mobs"));

        module("Criticals", Category.COMBAT, enabled -> {},
                        "Puts the server's copy of you in the air for the instant a hit lands. "
                                + "Packet mode never actually moves you; the jump modes do.")
                .setting(Setting.mode("Mode", "Packet", "Mini Jump", "Full Jump"));

        module("Auto Clicker", Category.COMBAT, enabled -> {},
                        "Clicks at a set rate while you hold the attack button down.")
                .setting(Setting.slider("CPS", 10, 1, 20, 0))
                .setting(Setting.toggle("Jitter", true));

        module("Reach", Category.COMBAT, enabled -> {},
                        "Lengthens the client interaction raycast. The server validates against its own "
                                + "copy, so past vanilla range the attack is simply dropped.")
                .setting(Setting.slider("Extra blocks", 1.0, 0, 3, 1));

        module("Crystal Aura", Category.COMBAT, enabled -> {},
                        "Places end crystals next to a target and breaks them. Scores every legal "
                                + "position by what the blast would do to them and to you, and acts "
                                + "on the best one that clears both thresholds.")
                .setting(Setting.slider("Range", 4.5, 3, 6, 1))
                .setting(Setting.slider("Target range", 12, 4, 16, 0))
                .setting(Setting.slider("Min damage", 6, 1, 20, 0))
                .setting(Setting.slider("Max self damage", 8, 0, 20, 0))
                .setting(Setting.slider("Delay", 50, 0, 500, 0))
                .setting(Setting.toggle("Place", true))
                .setting(Setting.toggle("Break", true));

        module("Aim Assist", Category.COMBAT, enabled -> {},
                        "Pulls your crosshair towards a target without ever attacking. You still "
                                + "click; the turn speed is what decides whether it reads as a nudge "
                                + "or a lock-on.")
                .setting(Setting.slider("Turn speed", 180, 20, 720, 0))
                .setting(Setting.slider("Range", 6, 3, 12, 1))
                .setting(Setting.slider("FOV", 120, 30, 360, 0))
                .setting(Setting.mode("Targets", "Players", "Mobs", "All"))
                .setting(Setting.toggle("Check line of sight", true))
                .setting(Setting.toggle("While using items", false));

        module("Bow Aimbot", Category.COMBAT, enabled -> {},
                        "Solves the launch angle for a bow or crossbow, allowing for drop and for "
                                + "where the target is heading. Only aims while drawn or loaded.")
                .setting(Setting.slider("Range", 64, 8, 128, 0))
                .setting(Setting.slider("Predict movement", 0.1, 0, 1.0, 2))
                .setting(Setting.mode("Targets", "Players", "Mobs", "All"));

        module("Auto Totem", Category.COMBAT, AutoTotem::onToggle,
                        "Moves a totem to your off hand when your health drops, using ordinary "
                                + "container clicks.")
                .setting(Setting.slider("Health", 10, 1, 20, 0));
    }

    // ---- Movement ---------------------------------------------------------

    private static void movement() {
        module("Sprint", Category.MOVEMENT, enabled -> {},
                "Sprints whenever you are walking forward.");

        module("Step", Category.MOVEMENT, enabled -> {},
                        "Walks up blocks. Legit measures the obstruction and climbs it through two "
                                + "intermediate positions; Simple raises the vanilla step-height "
                                + "attribute and lets vanilla do the rest.")
                .setting(Setting.mode("Mode", "Legit", "Simple"))
                .setting(Setting.slider("Height", 1.0, 0.6, 2.5, 1));

        module("Fly", Category.MOVEMENT, enabled -> {},
                        "Zeroes gravity and drives your motion from the movement keys.")
                .setting(Setting.slider("Speed", 0.5, 0.1, 2.0, 2));

        module("Speed", Category.MOVEMENT, enabled -> {},
                        "Scales your horizontal motion after vanilla has computed it.")
                .setting(Setting.slider("Multiplier", 1.4, 1.0, 3.0, 2));

        module("Velocity", Category.MOVEMENT, enabled -> {},
                        "Damps knockback. 0% takes all of it off that axis.")
                .setting(Setting.slider("Horizontal", 0, 0, 100, 0))
                .setting(Setting.slider("Vertical", 100, 0, 100, 0));

        module("Spider", Category.MOVEMENT, enabled -> {},
                "Climbs walls by holding an upward velocity while you are pushing into one.");

        module("Bunny Hop", Category.MOVEMENT, enabled -> {},
                        "Jumps for you whenever the chosen condition holds.")
                .setting(Setting.mode("Jump if", "Sprinting", "Walking", "Always"));

        module("Safe Walk", Category.MOVEMENT, enabled -> {},
                        "Stops you walking off edges. By default it overrides the check vanilla "
                                + "only applies while sneaking; Visible sneak instead presses the "
                                + "real key, which others can see.")
                .setting(Setting.toggle("Visible sneak", false))
                .setting(Setting.slider("Edge distance", 0.05, 0.05, 0.25, 2));

        module("No Slow", Category.MOVEMENT, enabled -> {},
                "Removes the movement penalty for eating, drawing a bow or holding a shield.");

        module("Phase", Category.MOVEMENT, enabled -> {},
                "Turns off collision. The server runs its own and will pull you back out of "
                        + "walls; dropping through floors is what this actually gets you.");

        module("Boat Fly", Category.MOVEMENT, enabled -> {},
                        "Flies whatever you are riding, by writing the vehicle's own motion.")
                .setting(Setting.slider("Speed", 1.0, 0.2, 3.0, 1));

        module("Jesus", Category.MOVEMENT, enabled -> {},
                        "Holds you at the surface of water and lava. Bob floats; Walk hops you back "
                                + "out each time you sink in.")
                .setting(Setting.mode("Mode", "Walk", "Bob"));

        module("Elytra Fly", Category.MOVEMENT, enabled -> {},
                        "Flies an elytra at a constant speed, optionally without losing height. "
                                + "Only while you are already gliding.")
                .setting(Setting.slider("Speed", 1.2, 0.5, 3.0, 1))
                .setting(Setting.toggle("Hold altitude", true));

        module("No Fall", Category.MOVEMENT, enabled -> {},
                "Claims to be on the ground while falling. Fall damage is the server sum, not yours.");
    }

    // ---- Render -----------------------------------------------------------

    private static void render() {
        module("ESP", Category.RENDER, enabled -> {},
                        "Boxes around entities, projected onto the HUD.")
                .setting(Setting.mode("Targets", "Players", "Mobs", "All"))
                .setting(Setting.slider("Range", 64, 8, 128, 0))
                .setting(Setting.toggle("Fill", false));

        module("Tracers", Category.RENDER, enabled -> {},
                        "Lines from the bottom of the screen to each target.")
                .setting(Setting.mode("Targets", "Players", "Mobs", "All"))
                .setting(Setting.slider("Range", 64, 8, 128, 0));

        module("Name Tags", Category.RENDER, enabled -> {},
                        "Names, health and distance above every entity, through walls.")
                .setting(Setting.mode("Targets", "All", "Players", "Mobs"))
                .setting(Setting.slider("Range", 48, 8, 128, 0))
                .setting(Setting.toggle("Show health", true))
                .setting(Setting.toggle("Show distance", true));

        module("Trajectories", Category.RENDER, enabled -> {},
                "Simulates where a thrown item will land and draws the path, marking the impact.");

        module("Target HUD", Category.RENDER, enabled -> {},
                "A panel for whatever the combat modules are aimed at, with a health bar.");

        module("X-Ray", Category.RENDER, XRay::onToggle,
                "Draws only ores and containers, and hides everything else. Toggling rebuilds "
                        + "the loaded chunks, which is what makes the change appear at once.");

        module("Freecam", Category.RENDER, enabled -> {},
                        "Detaches the camera. Your body stays where it is, keeps taking damage, "
                                + "and stays visible to everyone else.")
                .setting(Setting.slider("Speed", 1.0, 0.2, 4.0, 1));

        module("Item ESP", Category.RENDER, enabled -> {},
                        "Labels dropped items with their name and stack size.")
                .setting(Setting.slider("Range", 32, 8, 64, 0));

        module("Player Radar", Category.RENDER, enabled -> {},
                        "A top-down radar, rotated so your facing is up. Shares the bottom-right "
                                + "corner with the Effects HUD.")
                .setting(Setting.mode("Targets", "Players", "Mobs", "All"))
                .setting(Setting.slider("Range", 64, 16, 128, 0))
                .setting(Setting.slider("Size", 100, 60, 160, 0));
    }

    // ---- Player - inventory, tools and blocks ------------------------------

    private static void player() {
        module("Auto Tool", Category.PLAYER, enabled -> {},
                        "Switches to the fastest tool for whatever your crosshair is on, using "
                                + "vanilla's own mining-speed number.")
                .setting(Setting.toggle("Use swords", false))
                .setting(Setting.slider("Repair mode", 0, 0, 100, 0));

        module("Auto Eat", Category.PLAYER, enabled -> {},
                        "Eats when hunger drops, then puts your item back.")
                .setting(Setting.slider("Eat when hunger below", 16, 1, 19, 0));

        module("Auto Armor", Category.PLAYER, enabled -> {},
                "Wears the best armour you are carrying, scored from the armour and toughness "
                        + "its attribute modifiers grant.");

        module("Chest Stealer", Category.PLAYER, enabled -> {},
                        "Empties an open container into your inventory, one shift-click at a time.")
                .setting(Setting.slider("Items per second", 8, 1, 20, 0))
                .setting(Setting.toggle("Close when empty", true));

        module("Scaffold", Category.PLAYER, enabled -> {},
                        "Places a full block under your feet as you walk off an edge.")
                .setting(Setting.toggle("Rotate", true))
                .setting(Setting.slider("Turn speed", 360, 20, 720, 0));

        module("Fast Place", Category.PLAYER, enabled -> {},
                "Removes the delay vanilla puts between block placements.");

        module("Fast Break", Category.PLAYER, enabled -> {},
                "Removes the delay vanilla puts between finishing one block and starting the next.");

        module("Inventory Move", Category.PLAYER, enabled -> {},
                "Keeps the movement keys working while a screen is open. Chat is excluded, so "
                        + "typing still types.");

        module("Blink", Category.PLAYER, enabled -> {},
                "Holds your position updates while it is on, then releases them all at once. "
                        + "Only movement packets are held.");

        module("Pearl Key", Category.PLAYER, enabled -> {},
                "Throws an ender pearl on a keypress (R by default) and puts your item back.");

        module("Nuker", Category.PLAYER, enabled -> {},
                        "Breaks every block in range, nearest first. One block per tick, because "
                                + "that is how vanilla breaking accumulates damage.")
                .setting(Setting.slider("Range", 4, 1, 6, 0))
                .setting(Setting.toggle("Auto tool", true))
                .setting(Setting.toggle("Rotate", true))
                .setting(Setting.slider("Turn speed", 360, 20, 720, 0));
    }

    // ---- Visual - how your own client renders -----------------------------

    private static void visual() {
        // Both of these are driven from the client tick rather than the toggle,
        // because they have to keep re-asserting the option and hand it back the
        // moment they are switched off. The toggle only has to stop them dead.
        module("Brightness", Category.VISUAL, enabled -> {
                            if (!enabled) {
                                Brightness.reset();
                            }
                        },
                        "The vanilla Brightness slider on a toggle. Tops out where the game does, and "
                                + "restores your own value when switched off.")
                .setting(Setting.slider("Level", 1.0, 0.0, 1.0, 2));

        module("Fullbright", Category.VISUAL, enabled -> {},
                "The real one: every light level reports as maximum, so an unlit cave renders "
                        + "as daylight. Brightness only moves the vanilla slider.");

        module("Zoom", Category.VISUAL, enabled -> {
                            if (!enabled) {
                                Zoom.reset();
                            }
                        },
                        "Hold C to narrow your FOV, by moving the vanilla FOV option. Floors at the "
                                + "game's own minimum of 30.")
                .setting(Setting.slider("Factor", 2, 1, 4, 1));
    }

    // ---- SMP - server quality of life -------------------------------------

    private static void smp() {
        // The tracker counts continuously either way - the toggle governs the
        // readout - but switching it off is a natural point to bank the minutes
        // accrued so far rather than leaving them for the next flush.
        module("Playtime Tracker", Category.SMP, enabled -> Trackers.flushPlaytime(),
                "Counts how long you have spent on each server, and remembers it.");

        module("Death Coords", Category.SMP, enabled -> {},
                "Records where you died and prints it to your own chat.");

        // Clearing the seen-message ring on every flip means switching the
        // filter off and on again is a way to un-hide a repeat you wanted.
        module("Chat Filter", Category.SMP, enabled -> ChatFilter.forget(),
                        "Hides chat you have already seen. A display filter - nothing is sent back.")
                .setting(Setting.toggle("Hide duplicates", true))
                .setting(Setting.toggle("Hide links", false));
    }

    // ---- Misc -------------------------------------------------------------

    private static void misc() {
        module("FPS Limiter", Category.MISC, enabled -> FrameLimit.apply(),
                        "Caps your frame rate, and puts the vanilla setting back when you switch it off.")
                .setting(Setting.slider("Limit", 60, 10, 260, 0))
                // Without this the slider would only bite on the next toggle.
                .onSettingChange(setting -> FrameLimit.apply());

        module("Fast GUI", Category.MISC, Draw::setFast,
                "Draws the menu at GUI-pixel resolution instead of the display's. Blockier, "
                        + "but roughly half the draw calls - turn it on if the menu is slow.");

        module("Toggle Sounds", Category.MISC, enabled -> {},
                "Plays a click when you toggle a module, so you can feel the GUI respond.");
    }
}
