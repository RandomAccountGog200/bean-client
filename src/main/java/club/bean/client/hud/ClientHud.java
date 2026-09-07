package club.bean.client.hud;

import club.bean.client.feature.Clicks;
import club.bean.client.feature.Trackers;
import club.bean.client.gui.Draw;
import club.bean.client.module.Module;
import club.bean.client.module.ModuleRegistry;
import club.bean.client.module.Setting;
import club.bean.client.theme.Colours;
import club.bean.client.theme.Theme;
import club.bean.client.theme.ThemeManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The in-game overlay — the part of Bean Client that does something.
 *
 * <p>Everything here reads state the vanilla client already has and draws it on
 * your own screen. Nothing is sent to the server, and nothing is derived that
 * the client was not already given.
 *
 * <p>Each block is gated on its module's toggle and reads that module's
 * settings, so flipping a switch in the GUI changes what you see with no code
 * path special-cased for it.
 */
public final class ClientHud {
    private static final long STARTED = System.currentTimeMillis();
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private static final EquipmentSlot[] ARMOUR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND
    };

    private ClientHud() {
    }

    public static long sessionMillis() {
        return System.currentTimeMillis() - STARTED;
    }

    static void render(GuiGraphicsExtractor gfx, Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        Clicks.sample(mc.options.keyAttack.isDown(), mc.options.keyUse.isDown());

        Theme theme = ThemeManager.current();
        Font font = mc.font;

        int top = 6;
        if (on("watermark")) {
            drawWatermark(gfx, font, theme, 6, top);
            top += 22;
        }
        drawInfoStack(gfx, mc, font, theme, 6, top);
        drawModuleList(gfx, font, theme);
        drawKeystrokes(gfx, mc, font, theme);
        drawArmour(gfx, mc, font, theme);
        drawEffects(gfx, mc, font, theme);
    }

    // ---- module helpers ---------------------------------------------------

    private static boolean on(String id) {
        Module module = ModuleRegistry.get(id);
        return module != null && module.isEnabled();
    }

    private static boolean option(String moduleId, String settingName, boolean fallback) {
        Module module = ModuleRegistry.get(moduleId);
        if (module == null) {
            return fallback;
        }
        for (Setting setting : module.settings()) {
            if (setting.name().equalsIgnoreCase(settingName) && setting.type() == Setting.Type.TOGGLE) {
                return setting.boolValue();
            }
        }
        return fallback;
    }

    private static String mode(String moduleId, String settingName, String fallback) {
        Module module = ModuleRegistry.get(moduleId);
        if (module == null) {
            return fallback;
        }
        for (Setting setting : module.settings()) {
            if (setting.name().equalsIgnoreCase(settingName) && setting.type() == Setting.Type.MODE) {
                return setting.modeValue();
            }
        }
        return fallback;
    }

    // ---- panels -----------------------------------------------------------

    private static void panel(GuiGraphicsExtractor gfx, Theme theme, int x, int y, int w, int h) {
        Draw.roundRect(gfx, x, y, w, h, Math.max(2, theme.cornerRadius - 2),
                Colours.withAlpha(theme.background, 165));
        Draw.roundRect(gfx, x, y + 4, 2, h - 8, 1, theme.accent);
    }

    private static void drawWatermark(GuiGraphicsExtractor gfx, Font font, Theme theme, int x, int y) {
        String name = "Bean";
        int w = 22 + font.width(name) + font.width(" Client") + 10;
        Draw.roundRect(gfx, x, y, w, 18, Math.max(2, theme.cornerRadius - 2),
                Colours.withAlpha(theme.background, 175));
        Draw.bean(gfx, x + 12, y + 9, 15, 10, theme.accent, Colours.withAlpha(theme.background, 255));
        Draw.text(gfx, font, name, x + 22, y + 5, theme.text);
        Draw.text(gfx, font, " Client", x + 22 + font.width(name), y + 5, theme.accent);
    }

    /** FPS, coordinates, ping, speed, clock, session and server, stacked top-left. */
    private static void drawInfoStack(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme,
                                      int x, int y) {
        List<String> lines = new ArrayList<>();

        if (on("fps_display")) {
            lines.add(mc.getFps() + " fps");
        }
        if (on("coordinates")) {
            BlockPos pos = mc.player.blockPosition();
            lines.add(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            if (option("coordinates", "Nether conversion", true)) {
                lines.add("nether  " + Math.floorDiv(pos.getX(), 8) + ", " + Math.floorDiv(pos.getZ(), 8));
            }
        }
        if (on("ping_display")) {
            lines.add(ping(mc) + " ms");
        }
        if (on("cps_counter")) {
            lines.add(Clicks.left() + " / " + Clicks.right() + " cps");
        }
        if (on("speedometer")) {
            lines.add(String.format(Locale.ROOT, "%.1f b/s", Trackers.blocksPerSecond()));
        }
        if (on("clock")) {
            lines.add(LocalTime.now().format(CLOCK));
        }
        if (on("session_timer")) {
            lines.add(formatDuration(sessionMillis()));
        }
        if (on("playtime_tracker")) {
            long minutes = Trackers.playtimeMinutes(mc);
            lines.add(minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m played"
                    : minutes + "m played");
        }
        if (on("server_info")) {
            lines.add(Trackers.serverName(mc));
            if (mc.getConnection() != null) {
                lines.add(mc.getConnection().getOnlinePlayers().size() + " online");
            }
        }
        if (on("death_coords")) {
            BlockPos death = Trackers.deathPos();
            if (death != null) {
                lines.add("death  " + death.getX() + ", " + death.getY() + ", " + death.getZ());
            }
        }
        if (lines.isEmpty()) {
            return;
        }

        int width = 0;
        for (String line : lines) {
            width = Math.max(width, font.width(line));
        }
        int h = lines.size() * 11 + 7;
        panel(gfx, theme, x, y, width + 16, h);
        for (int i = 0; i < lines.size(); i++) {
            Draw.text(gfx, font, lines.get(i), x + 9, y + 5 + i * 11, theme.text);
        }
    }

    private static int ping(Minecraft mc) {
        if (mc.getConnection() == null || mc.player == null) {
            return 0;
        }
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info == null ? 0 : Math.max(0, info.getLatency());
    }

    /** The enabled-module list down one side. */
    private static void drawModuleList(GuiGraphicsExtractor gfx, Font font, Theme theme) {
        if (!on("module_list")) {
            return;
        }
        List<Module> enabled = new ArrayList<>();
        for (Module module : ModuleRegistry.all()) {
            if (module.isEnabled()) {
                enabled.add(module);
            }
        }
        if (enabled.isEmpty()) {
            return;
        }
        // Longest first reads as a wedge rather than a ragged column.
        enabled.sort((a, b) -> font.width(b.name()) - font.width(a.name()));

        boolean left = mode("module_list", "Corner", "Top right").toLowerCase(Locale.ROOT).contains("left");
        int screenW = gfx.guiWidth();
        int y = 6;

        for (Module module : enabled) {
            String name = module.name();
            int w = font.width(name);
            int x = left ? 6 : screenW - w - 10;
            Draw.roundRect(gfx, x - 4, y - 1, w + 8, 12, 2, Colours.withAlpha(theme.background, 145));
            Draw.text(gfx, font, name, x, y + 1, theme.accent);
            y += 12;
        }
    }

    /** WASD and the mouse buttons, lit while held. */
    private static void drawKeystrokes(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme) {
        if (!on("keystrokes")) {
            return;
        }
        int key = 18;
        int gap = 2;
        int block = key * 3 + gap * 2;
        int x = 6;
        int y = gfx.guiHeight() - block - 34;

        drawKey(gfx, font, theme, x + key + gap, y, key, key, "W", mc.options.keyUp);
        drawKey(gfx, font, theme, x, y + key + gap, key, key, "A", mc.options.keyLeft);
        drawKey(gfx, font, theme, x + key + gap, y + key + gap, key, key, "S", mc.options.keyDown);
        drawKey(gfx, font, theme, x + (key + gap) * 2, y + key + gap, key, key, "D", mc.options.keyRight);

        int wide = (block - gap) / 2;
        int row = y + (key + gap) * 2;
        drawKey(gfx, font, theme, x, row, wide, key, "LMB", mc.options.keyAttack);
        drawKey(gfx, font, theme, x + wide + gap, row, wide, key, "RMB", mc.options.keyUse);
        drawKey(gfx, font, theme, x, row + key + gap, block, 10, "", mc.options.keyJump);
    }

    private static void drawKey(GuiGraphicsExtractor gfx, Font font, Theme theme, int x, int y,
                                int w, int h, String label, KeyMapping key) {
        boolean down = key.isDown();
        int fill = down ? Colours.withAlpha(theme.accent, 210) : Colours.withAlpha(theme.background, 165);
        Draw.roundRect(gfx, x, y, w, h, 3, fill);
        if (!label.isEmpty()) {
            int colour = down ? Colours.contrastOn(theme.accent) : theme.text;
            Draw.textCentred(gfx, font, label, x + w / 2, y + (h - font.lineHeight) / 2 + 1, colour);
        }
    }

    /** Your armour and held item, with durability. */
    private static void drawArmour(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme) {
        if (!on("armour_hud")) {
            return;
        }
        List<ItemStack> shown = new ArrayList<>();
        for (EquipmentSlot slot : ARMOUR) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                shown.add(stack);
            }
        }
        if (shown.isEmpty()) {
            return;
        }

        int rowH = 18;
        int w = 58;
        int h = shown.size() * rowH + 6;
        int x = gfx.guiWidth() - w - 6;
        int y = (gfx.guiHeight() - h) / 2;
        panel(gfx, theme, x, y, w, h);

        for (int i = 0; i < shown.size(); i++) {
            ItemStack stack = shown.get(i);
            int rowY = y + 3 + i * rowH;
            gfx.item(stack, x + 8, rowY);
            if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                int left = stack.getMaxDamage() - stack.getDamageValue();
                int percent = Math.round(100f * left / stack.getMaxDamage());
                int colour = percent <= 15 ? 0xFFE05555 : (percent <= 40 ? 0xFFE0B040 : theme.text);
                Draw.text(gfx, font, percent + "%", x + 28, rowY + 5, colour);
            }
        }
    }

    /** Active potion effects, with time remaining. */
    private static void drawEffects(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme) {
        if (!on("effects_hud")) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            String name = effect.getEffect().value().getDisplayName().getString();
            int level = effect.getAmplifier() + 1;
            lines.add(name + (level > 1 ? " " + level : "") + "  " + formatTicks(effect.getDuration()));
        }
        if (lines.isEmpty()) {
            return;
        }

        int width = 0;
        for (String line : lines) {
            width = Math.max(width, font.width(line));
        }
        int h = lines.size() * 11 + 7;
        int x = gfx.guiWidth() - width - 22;
        int y = gfx.guiHeight() - h - 34;
        panel(gfx, theme, x, y, width + 16, h);
        for (int i = 0; i < lines.size(); i++) {
            Draw.text(gfx, font, lines.get(i), x + 9, y + 5 + i * 11, theme.text);
        }
    }

    static String formatTicks(int ticks) {
        int seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours > 0
                ? String.format(Locale.ROOT, "%dh %02dm", hours, minutes)
                : String.format(Locale.ROOT, "%dm %02ds", minutes, seconds % 60);
    }
}
