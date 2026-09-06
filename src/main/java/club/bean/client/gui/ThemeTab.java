package club.bean.client.gui;

import club.bean.client.theme.Colours;
import club.bean.client.theme.Theme;
import club.bean.client.theme.ThemeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * The Themes tab: a theme dropdown, a live accent picker, and the three
 * buttons that connect the GUI back to the {@code themes/} folder.
 *
 * <p>Everything here writes through {@link ThemeManager}, and every other panel
 * reads its colours from {@code ThemeManager.current()} on each frame - which
 * is why picking a colour reskins the whole window immediately with no
 * invalidation step.
 */
public final class ThemeTab {
    private static final int LABEL_GAP = 11;
    private static final int DROPDOWN_H = 22;
    private static final int PICKER_H = 64;
    private static final int HUE_W = 12;
    private static final int BUTTON_H = 20;
    private static final int ITEM_H = 20;

    private static boolean dropdownOpen;

    /** Live HSV of the accent. Kept alongside the colour because grey has no hue. */
    private static final float[] HSV = { 0.08f, 0.8f, 0.9f };
    private static int syncedAccent;

    private static boolean draggingSv;
    private static boolean draggingHue;

    private static String toast = "";
    private static long toastUntil;

    private ThemeTab() {
    }

    // ---- layout ------------------------------------------------------------

    private static int x() {
        return BeanGui.panelX() + 12;
    }

    private static int w() {
        return BeanGui.panelW() - 24;
    }

    private static int dropdownY() {
        return BeanGui.panelY() + 12 + LABEL_GAP;
    }

    private static int accentLabelY() {
        return dropdownY() + DROPDOWN_H + 14;
    }

    private static int pickerY() {
        return accentLabelY() + LABEL_GAP;
    }

    private static int svW() {
        return Math.max(60, Math.min(180, w() - 20 - HUE_W));
    }

    private static int hueX() {
        return x() + svW() + 8;
    }

    private static int previewX() {
        return hueX() + HUE_W + 8;
    }

    private static int previewW() {
        return Math.max(0, x() + w() - previewX());
    }

    private static int buttonsY() {
        return pickerY() + PICKER_H + 10;
    }

    private static int buttonW() {
        return (w() - 16) / 3;
    }

    private static int buttonX(int index) {
        return x() + index * (buttonW() + 8);
    }

    private static int hintY() {
        return buttonsY() + BUTTON_H + 10;
    }

    // ---- state -------------------------------------------------------------

    /** Pulls HSV back from the theme whenever the accent changed behind our back. */
    private static void syncFromTheme() {
        int accent = ThemeManager.current().accent;
        if (accent != syncedAccent) {
            syncedAccent = accent;
            float[] hsv = Colours.toHsv(accent);
            HSV[0] = hsv[1] > 0.02f ? hsv[0] : HSV[0];
            HSV[1] = hsv[1];
            HSV[2] = hsv[2];
        }
    }

    private static void pushAccent() {
        int accent = Colours.fromHsv(HSV[0], HSV[1], HSV[2]);
        syncedAccent = accent;
        ThemeManager.setAccent(accent);
    }

    private static void say(String message) {
        toast = message;
        toastUntil = System.currentTimeMillis() + 2600L;
    }

    public static void reset() {
        dropdownOpen = false;
        draggingSv = false;
        draggingHue = false;
    }

    // ---- render ------------------------------------------------------------

    static void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY,
                       boolean interactive, Theme theme) {
        syncFromTheme();
        int x = x();
        int w = w();

        Draw.text(gfx, font, "THEME", x, BeanGui.panelY() + 12, theme.textDim);
        drawDropdownButton(gfx, font, mouseX, mouseY, interactive, theme);

        Draw.text(gfx, font, "ACCENT COLOUR", x, accentLabelY(), theme.textDim);
        drawSaturationValue(gfx, theme);
        drawHueStrip(gfx, theme);
        drawPreview(gfx, font, theme);

        drawButtons(gfx, font, mouseX, mouseY, interactive, theme);
        drawHint(gfx, font, theme);
        drawPalette(gfx, font, theme);
    }

    /**
     * The theme's own fields, as swatches. It doubles as documentation - the
     * label under each chip is the exact JSON key that produced it, so the file
     * format is discoverable from inside the game.
     */
    private static void drawPalette(GuiGraphicsExtractor gfx, Font font, Theme theme) {
        int y = paletteY();
        int bottom = BeanGui.panelY() + BeanGui.panelH();
        if (y + 46 > bottom) {
            return;
        }

        Draw.text(gfx, font, "THIS THEME'S JSON", x(), y, theme.textDim);

        String[] keys = { "background", "panel", "panelAlt", "text", "textDim", "accent" };
        int[] colours = { theme.background, theme.panel, theme.panelAlt,
                          theme.text, theme.textDim, theme.accent };

        int columns = 3;
        int cell = w() / columns;
        int rowH = 16;
        int top = y + LABEL_GAP + 2;
        int chip = 11;

        for (int i = 0; i < keys.length; i++) {
            int cx = x() + (i % columns) * cell;
            int cy = top + (i / columns) * rowH;
            Draw.roundRectOutlined(gfx, cx, cy, chip, chip, 2, colours[i],
                    Colours.withAlpha(theme.text, 45));
            Draw.text(gfx, font, Draw.clip(font, keys[i], cell - chip - 10), cx + chip + 5, cy + 2,
                    Colours.fade(theme.textDim, 0.8f));
        }

        // cornerRadius is the one non-colour field, so it is shown as a shape.
        int shapeY = top + 2 * rowH + 3;
        if (shapeY + chip <= bottom) {
            Draw.roundRect(gfx, x(), shapeY, chip, chip, theme.cornerRadius, theme.accent);
            Draw.text(gfx, font, "cornerRadius " + theme.cornerRadius, x() + chip + 5, shapeY + 2,
                    Colours.fade(theme.textDim, 0.8f));
            Draw.text(gfx, font, "pattern " + theme.pattern.name().toLowerCase(java.util.Locale.ROOT),
                    x() + cell + 5, shapeY + 2, Colours.fade(theme.textDim, 0.8f));
        }
    }

    private static int paletteY() {
        return hintY() + 9 * 2 + 12;
    }

    /** Drawn after everything else so the open dropdown is not clipped by the panel. */
    static void renderOverlay(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY,
                              boolean interactive, Theme theme) {
        if (!dropdownOpen) {
            return;
        }
        List<String> ids = ThemeManager.pickerIds();
        int x = x();
        int w = w();
        int top = dropdownY() + DROPDOWN_H + 3;
        int h = ids.size() * ITEM_H + 6;
        int radius = Math.max(2, theme.cornerRadius - 2);

        Draw.roundRect(gfx, x - 1, top - 1, w + 2, h + 2, radius + 1, Colours.withAlpha(0xFF000000, 90));
        Draw.roundRectOutlined(gfx, x, top, w, h, radius, theme.panelAlt,
                Colours.withAlpha(theme.accent, 120));

        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            Theme entry = ThemeManager.byId(id);
            int y = top + 3 + i * ITEM_H;
            boolean active = id.equals(ThemeManager.activeId());
            boolean hovered = interactive && BeanGui.hit(mouseX, mouseY, x + 3, y, w - 6, ITEM_H);

            if (active || hovered) {
                Draw.roundRect(gfx, x + 3, y, w - 6, ITEM_H, Math.max(1, radius - 2),
                        active ? Colours.withAlpha(theme.accent, 60) : Colours.withAlpha(theme.text, 18));
            }

            // Each entry previews its own accent, so the list reads as swatches.
            int swatch = entry == null ? theme.accent : entry.accent;
            Draw.roundRect(gfx, x + 9, y + (ITEM_H - 9) / 2, 9, 9, 2, swatch);

            String label = entry == null ? id : entry.name;
            Draw.text(gfx, font, Draw.clip(font, label, w - 40), x + 24,
                    y + (ITEM_H - font.lineHeight) / 2 + 1, active ? theme.text : theme.textDim);

            if (active) {
                Draw.glyph(gfx, Draw.CHECK, x + w - 18, y + (ITEM_H - 9) / 2, 1, theme.accent);
            }
        }
    }

    private static void drawDropdownButton(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY,
                                           boolean interactive, Theme theme) {
        int x = x();
        int y = dropdownY();
        int w = w();
        int radius = Math.max(2, theme.cornerRadius - 2);
        boolean hovered = interactive && BeanGui.hit(mouseX, mouseY, x, y, w, DROPDOWN_H);

        if (dropdownOpen) {
            Draw.roundRectOutlined(gfx, x, y, w, DROPDOWN_H, radius, theme.panelAlt,
                    Colours.withAlpha(theme.accent, 190));
        } else {
            Draw.roundRect(gfx, x, y, w, DROPDOWN_H, radius,
                    hovered ? Colours.lighten(theme.panelAlt, 0.05f) : theme.panelAlt);
        }

        Draw.roundRect(gfx, x + 8, y + (DROPDOWN_H - 10) / 2, 10, 10, 2, theme.accent);
        Draw.text(gfx, font, Draw.clip(font, ThemeManager.current().name, w - 46), x + 24,
                y + (DROPDOWN_H - font.lineHeight) / 2 + 1, theme.text);
        Draw.glyph(gfx, Draw.CHEVRON_DOWN, x + w - 20, y + (DROPDOWN_H - 9) / 2, 1,
                hovered || dropdownOpen ? theme.accent : theme.textDim);
    }

    /**
     * Saturation across, value down. Drawn one column at a time - a vertical
     * gradient from the fully-lit hue to black is exactly the right column of
     * an SV square, so 180 gradient quads reproduce the square precisely.
     */
    private static void drawSaturationValue(GuiGraphicsExtractor gfx, Theme theme) {
        int x = x();
        int y = pickerY();
        int w = svW();

        for (int i = 0; i < w; i++) {
            float saturation = w <= 1 ? 0f : (float) i / (w - 1);
            int top = Colours.fromHsv(HSV[0], saturation, 1f);
            gfx.fillGradient(x + i, y, x + i + 1, y + PICKER_H,
                    Draw.col(top), Draw.col(0xFF000000));
        }
        Draw.border(gfx, x - 1, y - 1, w + 2, PICKER_H + 2, Colours.withAlpha(theme.text, 40));

        int cursorX = x + Math.round((w - 1) * HSV[1]);
        int cursorY = y + Math.round((PICKER_H - 1) * (1f - HSV[2]));
        ring(gfx, cursorX, cursorY, HSV[2] > 0.55f && HSV[1] < 0.55f ? 0xFF000000 : 0xFFFFFFFF);
    }

    private static void drawHueStrip(GuiGraphicsExtractor gfx, Theme theme) {
        int x = hueX();
        int y = pickerY();
        int segments = 24;
        float step = (float) PICKER_H / segments;

        for (int i = 0; i < segments; i++) {
            int y0 = y + Math.round(i * step);
            int y1 = y + Math.round((i + 1) * step);
            gfx.fillGradient(x, y0, x + HUE_W, y1,
                    Draw.col(Colours.fromHsv((float) i / segments, 1f, 1f)),
                    Draw.col(Colours.fromHsv((float) (i + 1) / segments, 1f, 1f)));
        }
        Draw.border(gfx, x - 1, y - 1, HUE_W + 2, PICKER_H + 2, Colours.withAlpha(theme.text, 40));

        int markerY = y + Math.round((PICKER_H - 1) * HSV[0]);
        Draw.rect(gfx, x - 2, markerY - 1, HUE_W + 4, 3, 0xFFFFFFFF);
        Draw.rect(gfx, x - 1, markerY, HUE_W + 2, 1, Colours.fromHsv(HSV[0], 1f, 1f));
    }

    private static void ring(GuiGraphicsExtractor gfx, int cx, int cy, int colour) {
        Draw.rect(gfx, cx - 3, cy - 1, 2, 2, colour);
        Draw.rect(gfx, cx + 2, cy - 1, 2, 2, colour);
        Draw.rect(gfx, cx - 1, cy - 3, 2, 2, colour);
        Draw.rect(gfx, cx - 1, cy + 2, 2, 2, colour);
    }

    private static void drawPreview(GuiGraphicsExtractor gfx, Font font, Theme theme) {
        int w = previewW();
        if (w < 42) {
            return;
        }
        int x = previewX();
        int y = pickerY();
        int radius = Math.max(2, theme.cornerRadius - 2);

        Draw.roundRect(gfx, x, y, w, PICKER_H, radius, theme.accent);
        // A bean in the preview so the swatch also shows the mark on the accent.
        Draw.bean(gfx, x + w / 2, y + PICKER_H / 2 - 7, Math.min(34, w - 12), 22,
                Colours.contrastOn(theme.accent), theme.accent);

        String hex = Colours.toHex(theme.accent);
        int textX = x + (w - font.width(hex)) / 2;
        Draw.text(gfx, font, hex, textX, y + PICKER_H - 14, Colours.contrastOn(theme.accent));
    }

    private static void drawButtons(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY,
                                    boolean interactive, Theme theme) {
        String[] labels = { "Reset", "Save file", "Reload" };
        boolean[] enabled = { ThemeManager.hasAccentOverride(), true, true };

        for (int i = 0; i < labels.length; i++) {
            int bx = buttonX(i);
            int bw = buttonW();
            int by = buttonsY();
            boolean hovered = interactive && enabled[i] && BeanGui.hit(mouseX, mouseY, bx, by, bw, BUTTON_H);
            int radius = Math.max(2, theme.cornerRadius - 3);

            int fill = hovered ? Colours.withAlpha(theme.accent, 70) : theme.panelAlt;
            Draw.roundRect(gfx, bx, by, bw, BUTTON_H, radius,
                    enabled[i] ? fill : Colours.fade(theme.panelAlt, 0.45f));

            int colour = !enabled[i] ? Colours.fade(theme.textDim, 0.5f) : (hovered ? theme.text : theme.textDim);
            String label = Draw.clip(font, labels[i], bw - 10);
            Draw.text(gfx, font, label, bx + (bw - font.width(label)) / 2,
                    by + (BUTTON_H - font.lineHeight) / 2 + 1, colour);
        }
    }

    private static void drawHint(GuiGraphicsExtractor gfx, Font font, Theme theme) {
        int x = x();
        int y = hintY();
        if (y + font.lineHeight > BeanGui.panelY() + BeanGui.panelH()) {
            return;
        }

        boolean showToast = System.currentTimeMillis() < toastUntil && !toast.isEmpty();
        String line = showToast ? toast : "Drop a .json in " + ThemeManager.themesDir().getFileName() + "/ then hit Reload";
        Draw.text(gfx, font, Draw.clip(font, line, w()), x, y,
                showToast ? theme.accent : Colours.fade(theme.textDim, 0.85f));

        Theme current = ThemeManager.current();
        if (!current.description.isEmpty() && y + font.lineHeight * 2 + 4 <= BeanGui.panelY() + BeanGui.panelH()) {
            Draw.text(gfx, font, Draw.clip(font, current.description, w()), x, y + font.lineHeight + 3,
                    Colours.fade(theme.textDim, 0.6f));
        }
    }

    // ---- input -------------------------------------------------------------

    /** @return true when the click was consumed by this tab */
    static boolean mouseClicked(double mx, double my, int button) {
        syncFromTheme();

        if (dropdownOpen) {
            List<String> ids = ThemeManager.pickerIds();
            int top = dropdownY() + DROPDOWN_H + 3;
            for (int i = 0; i < ids.size(); i++) {
                if (BeanGui.hit(mx, my, x() + 3, top + 3 + i * ITEM_H, w() - 6, ITEM_H)) {
                    ThemeManager.select(ids.get(i));
                    syncFromTheme();
                    dropdownOpen = false;
                    return true;
                }
            }
            dropdownOpen = false;
            // Fall through: a click outside the list still closes it, but should
            // not also trigger whatever is underneath.
            return true;
        }

        if (BeanGui.hit(mx, my, x(), dropdownY(), w(), DROPDOWN_H)) {
            dropdownOpen = true;
            return true;
        }

        if (BeanGui.hit(mx, my, x(), pickerY(), svW(), PICKER_H)) {
            draggingSv = true;
            updateSv(mx, my);
            return true;
        }

        if (BeanGui.hit(mx, my, hueX(), pickerY(), HUE_W, PICKER_H)) {
            draggingHue = true;
            updateHue(my);
            return true;
        }

        for (int i = 0; i < 3; i++) {
            if (BeanGui.hit(mx, my, buttonX(i), buttonsY(), buttonW(), BUTTON_H)) {
                pressButton(i);
                return true;
            }
        }

        return false;
    }

    private static void pressButton(int index) {
        switch (index) {
            case 0 -> {
                if (ThemeManager.hasAccentOverride()) {
                    ThemeManager.resetAccent();
                    syncedAccent = 0;
                    syncFromTheme();
                    say("Accent reset to the theme file");
                }
            }
            case 1 -> {
                String file = ThemeManager.current().id + ".json";
                say(ThemeManager.saveCurrentToFile() ? "Saved to " + file : "Could not write " + file);
                syncedAccent = 0;
                syncFromTheme();
            }
            default -> {
                ThemeManager.reload();
                syncedAccent = 0;
                syncFromTheme();
                say("Reloaded " + ThemeManager.pickerIds().size() + " theme(s)");
            }
        }
    }

    static boolean mouseDragged(double mx, double my) {
        if (draggingSv) {
            updateSv(mx, my);
            return true;
        }
        if (draggingHue) {
            updateHue(my);
            return true;
        }
        return false;
    }

    static void mouseReleased() {
        draggingSv = false;
        draggingHue = false;
    }

    static boolean isDropdownOpen() {
        return dropdownOpen;
    }

    static void closeDropdown() {
        dropdownOpen = false;
    }

    private static void updateSv(double mx, double my) {
        int w = svW();
        HSV[1] = (float) Anim.clamp((mx - x()) / Math.max(1, w - 1), 0, 1);
        HSV[2] = 1f - (float) Anim.clamp((my - pickerY()) / Math.max(1, PICKER_H - 1), 0, 1);
        pushAccent();
    }

    private static void updateHue(double my) {
        HSV[0] = (float) Anim.clamp((my - pickerY()) / Math.max(1, PICKER_H - 1), 0, 1);
        pushAccent();
    }
}
