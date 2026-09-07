package club.bean.client.hud;

import club.bean.client.gui.Draw;
import club.bean.client.module.Module;
import club.bean.client.module.ModuleRegistry;
import club.bean.client.module.Setting;
import club.bean.client.theme.Colours;
import club.bean.client.theme.Theme;
import club.bean.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The in-game overlay — the part of Bean Client that actually does something.
 *
 * <p>Everything here reads state the vanilla client already has and draws it on
 * your own screen. Nothing is sent to the server and nothing is derived that the
 * client was not already given, so it stays on the right side of the line the
 * rest of the module list does not yet cross.
 *
 * <p>Each block is gated on a module's toggle and reads that module's settings,
 * which is what makes the wiring real: flipping a switch in the GUI changes what
 * you see, with no code path special-cased for it.
 */
public final class ClientHud {
    private static final long STARTED = System.currentTimeMillis();

    private ClientHud() {
    }

    public static long sessionMillis() {
        return System.currentTimeMillis() - STARTED;
    }

    static void render(GuiGraphicsExtractor gfx, Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        Theme theme = ThemeManager.current();
        Font font = mc.font;

        drawWatermark(gfx, mc, font, theme);
        drawInfoLines(gfx, mc, font, theme);
        drawModuleList(gfx, font, theme);
    }

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
            if (setting.name().equalsIgnoreCase(settingName)) {
                return setting.type() == Setting.Type.TOGGLE ? setting.boolValue() : fallback;
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

    // ---- blocks -----------------------------------------------------------

    private static void drawWatermark(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme) {
        if (!on("hud") || !option("hud", "Watermark", true)) {
            return;
        }
        int x = 6;
        int y = 6;
        String name = "Bean";
        int w = 20 + font.width(name) + font.width(" Client") + 12;

        Draw.roundRect(gfx, x, y, w, 18, Math.max(2, theme.cornerRadius - 2),
                Colours.withAlpha(theme.background, 170));
        Draw.bean(gfx, x + 12, y + 9, 15, 10, theme.accent, Colours.withAlpha(theme.background, 255));
        Draw.text(gfx, font, name, x + 21, y + 5, theme.text);
        Draw.text(gfx, font, " Client", x + 21 + font.width(name), y + 5, theme.accent);
    }

    /** FPS, coordinates, ping and session time, stacked under the watermark. */
    private static void drawInfoLines(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme) {
        List<String> lines = new ArrayList<>();

        if (on("fps_display")) {
            lines.add(mc.getFps() + " fps");
        }
        if (on("coordinates")) {
            BlockPos pos = mc.player.blockPosition();
            lines.add(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            if (option("coordinates", "Show nether", true) && mc.level.dimension() != null) {
                lines.add("nether  " + (pos.getX() / 8) + ", " + (pos.getZ() / 8));
            }
        }
        if (on("ping_display")) {
            lines.add(ping(mc) + " ms");
        }
        if (on("session_timer")) {
            lines.add(formatDuration(sessionMillis()));
        }
        if (lines.isEmpty()) {
            return;
        }

        int x = 6;
        int y = on("hud") && option("hud", "Watermark", true) ? 28 : 6;
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, font.width(line));
        }

        int h = lines.size() * 11 + 7;
        Draw.roundRect(gfx, x, y, width + 16, h, Math.max(2, theme.cornerRadius - 2),
                Colours.withAlpha(theme.background, 150));
        Draw.roundRect(gfx, x, y + 4, 2, h - 8, 1, theme.accent);

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

    /** The enabled-module list down the right-hand side. */
    private static void drawModuleList(GuiGraphicsExtractor gfx, Font font, Theme theme) {
        if (!on("hud") || !option("hud", "Module list", true)) {
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

        boolean left = mode("hud", "Corner", "Top right").toLowerCase(Locale.ROOT).contains("left");
        int screenW = gfx.guiWidth();
        int y = 6;

        for (Module module : enabled) {
            String name = module.name();
            int w = font.width(name);
            int x = left ? 6 : screenW - w - 10;

            Draw.roundRect(gfx, x - 4, y - 1, w + 8, 12, 2, Colours.withAlpha(theme.background, 140));
            Draw.text(gfx, font, name, x, y + 1, theme.accent);
            y += 12;
        }
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
