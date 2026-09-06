package club.bean.client.gui;

import club.bean.client.module.Category;
import club.bean.client.module.Module;
import club.bean.client.module.Setting;
import club.bean.client.theme.Colours;
import club.bean.client.theme.Theme;
import club.bean.client.theme.ThemeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the whole window, and owns the layout maths that hit-testing reads back.
 *
 * <p>There is exactly one render path. The screen host calls it while the
 * window is open and the HUD host calls it for the tail of the close
 * animation, so the two can never drift apart visually.
 *
 * <p>Nothing here knows what a module <em>does</em> - it walks
 * {@code ModuleRegistry} and draws names, booleans and settings.
 */
public final class BeanGuiRenderer {
    /** One module row plus its drawer, positioned in screen space. */
    public record Row(Module module, int y, int rowH, int blockH) {
        public int drawerTop() {
            return y + rowH;
        }
    }

    private BeanGuiRenderer() {
    }

    // ---- layout ------------------------------------------------------------

    /** Rows for the current category, already offset by the scroll position. */
    public static List<Row> layoutRows() {
        List<Row> rows = new ArrayList<>();
        if (BeanGui.selected().isSpecial()) {
            return rows;
        }
        int y = BeanGui.listTop() - Math.round(BeanGui.scroll());
        for (Module module : BeanGui.visibleModules()) {
            int blockH = Math.round(BeanGui.blockHeight(module));
            rows.add(new Row(module, y, BeanGui.ROW_H, blockH));
            y += blockH + BeanGui.ROW_GAP;
        }
        return rows;
    }

    public static int toggleX() {
        return BeanGui.listX() + BeanGui.listW() - 12 - BeanGui.TOGGLE_W;
    }

    public static int gearX() {
        return toggleX() - 12 - BeanGui.GEAR;
    }

    /** Y of the {@code index}-th setting inside an open drawer. */
    public static int settingY(Row row, int index) {
        return row.drawerTop() + BeanGui.DRAWER_PAD + index * BeanGui.SETTING_H;
    }

    public static int settingX() {
        return BeanGui.listX() + 16;
    }

    public static int settingW() {
        return BeanGui.listW() - 32;
    }

    /** Slider track for a settings row, or null when that setting is not a slider. */
    public static int[] sliderBounds() {
        int x = settingX() + 92;
        int right = settingX() + settingW() - 40;
        int w = Math.max(36, right - x);
        return new int[] { x, w };
    }

    public static int railRowY(int index) {
        return BeanGui.railY() + 10 + index * (BeanGui.RAIL_ROW_H + BeanGui.RAIL_GAP);
    }

    /** The scale the window is currently drawn at - 0.94 while opening, 1 once settled. */
    public static float currentScale() {
        return Anim.lerp(0.94f, 1.0f, Anim.easeOutBack(BeanGui.progress()));
    }

    /**
     * Undoes the open animation's scale so hit-testing matches what is on
     * screen. Without this, clicking during the ~150ms the window is still
     * growing would land a few pixels off.
     */
    public static double localX(double screenX) {
        int cx = BeanGui.x() + BeanGui.width() / 2;
        return cx + (screenX - cx) / currentScale();
    }

    public static double localY(double screenY) {
        int cy = BeanGui.y() + BeanGui.height() / 2;
        return cy + (screenY - cy) / currentScale();
    }

    // ---- render ------------------------------------------------------------

    /**
     * @param interactive false when drawn from the HUD during the close
     *                    animation - hover highlights are suppressed so the
     *                    window does not react to a cursor that is no longer
     *                    controlling it
     */
    public static void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, boolean interactive) {
        BeanGui.tickAnimations();
        float progress = BeanGui.progress();
        if (progress <= 0.002f) {
            return;
        }

        Theme theme = ThemeManager.current();
        float alpha = Anim.easeOutCubic(progress);
        float scale = Anim.lerp(0.94f, 1.0f, Anim.easeOutBack(progress));

        int cx = BeanGui.x() + BeanGui.width() / 2;
        int cy = BeanGui.y() + BeanGui.height() / 2;

        // Scissor rectangles are transformed by the pose in 26.x, so scaling
        // here is safe - clipped content scales with the window.
        gfx.pose().pushMatrix();
        gfx.pose().translate(cx, cy);
        gfx.pose().scale(scale, scale);
        gfx.pose().translate(-cx, -cy);
        Draw.setOpacity(alpha);

        try {
            drawWindow(gfx, font, mouseX, mouseY, interactive, theme);
        } finally {
            Draw.setOpacity(1f);
            gfx.pose().popMatrix();
        }
    }

    private static void drawWindow(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY,
                                   boolean interactive, Theme theme) {
        int x = BeanGui.x();
        int y = BeanGui.y();
        int w = BeanGui.width();
        int h = BeanGui.height();
        int radius = theme.cornerRadius;

        // Soft drop shadow: three offset rounded rects at low alpha.
        for (int i = 3; i >= 1; i--) {
            Draw.roundRect(gfx, x - i, y - i + 2, w + i * 2, h + i * 2, radius + i,
                    Colours.withAlpha(0xFF000000, 22 - i * 5));
        }

        Draw.roundRect(gfx, x, y, w, h, radius, theme.background);
        Draw.backgroundPattern(gfx, x, y, w, h, theme);
        Draw.roundRect(gfx, x, y, w, h, radius, Colours.withAlpha(theme.background, 0));

        drawTitleBar(gfx, font, mouseX, mouseY, interactive, theme);
        drawRail(gfx, font, mouseX, mouseY, interactive, theme);

        // Panel
        int px = BeanGui.panelX();
        int py = BeanGui.panelY();
        int pw = BeanGui.panelW();
        int ph = BeanGui.panelH();
        Draw.roundRect(gfx, px, py, pw, ph, Math.max(2, radius - 2), theme.panel);

        if (BeanGui.selected() == Category.THEMES) {
            gfx.enableScissor(px, py, px + pw, py + ph);
            ThemeTab.render(gfx, font, mouseX, mouseY, interactive, theme);
            gfx.disableScissor();
        } else {
            drawSearch(gfx, font, mouseX, mouseY, interactive, theme);
            drawList(gfx, font, mouseX, mouseY, interactive, theme);
        }

        drawGrip(gfx, theme, mouseX, mouseY, interactive);

        // The dropdown escapes the panel, so it is drawn last and unclipped.
        if (BeanGui.selected() == Category.THEMES) {
            ThemeTab.renderOverlay(gfx, font, mouseX, mouseY, interactive, theme);
        }
    }

    private static void drawTitleBar(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY,
                                     boolean interactive, Theme theme) {
        int x = BeanGui.x();
        int y = BeanGui.y();

        // Logo: an accent bean with the window background showing through the crease.
        Draw.bean(gfx, x + BeanGui.PAD + 10, y + BeanGui.TITLE_H / 2, 20, 14, theme.accent, theme.background);

        int textX = x + BeanGui.PAD + 24;
        int textY = y + BeanGui.TITLE_H / 2 - font.lineHeight / 2;
        Draw.text(gfx, font, "Bean", textX, textY, theme.text);
        Draw.text(gfx, font, " Client", textX + font.width("Bean"), textY, theme.accent);

        // Right-hand status: theme name and module count, dimmed.
        String status = ThemeManager.current().name + "  ·  "
                + club.bean.client.module.ModuleRegistry.count() + " modules";
        int closeX = x + BeanGui.width() - BeanGui.PAD - 10;
        Draw.textRight(gfx, font, status, closeX - 16, textY, theme.textDim);

        // Close button
        boolean hoverClose = interactive && BeanGui.hit(mouseX, mouseY, closeX - 7, y + 9, 15, 15);
        int closeColour = hoverClose ? theme.accent : theme.textDim;
        for (int i = 0; i < 7; i++) {
            Draw.rect(gfx, closeX - 3 + i, y + 12 + i, 1, 1, closeColour);
            Draw.rect(gfx, closeX + 3 - i, y + 12 + i, 1, 1, closeColour);
        }

        Draw.rect(gfx, x + BeanGui.PAD, y + BeanGui.TITLE_H - 1,
                BeanGui.width() - BeanGui.PAD * 2, 1, Colours.withAlpha(theme.text, 18));
    }

    public static boolean inCloseButton(double mx, double my) {
        int closeX = BeanGui.x() + BeanGui.width() - BeanGui.PAD - 10;
        return BeanGui.hit(mx, my, closeX - 7, BeanGui.y() + 9, 15, 15);
    }

    private static void drawRail(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY,
                                 boolean interactive, Theme theme) {
        int rx = BeanGui.railX();
        int ry = BeanGui.railY();
        int radius = Math.max(2, theme.cornerRadius - 2);
        Draw.roundRect(gfx, rx, ry, BeanGui.RAIL_W, BeanGui.railH(), radius, theme.panel);

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            int y = railRowY(i);
            if (y + BeanGui.RAIL_ROW_H > ry + BeanGui.railH()) {
                break;
            }

            boolean active = BeanGui.selected() == category;
            boolean hovered = interactive
                    && BeanGui.hit(mouseX, mouseY, rx + 6, y, BeanGui.RAIL_W - 12, BeanGui.RAIL_ROW_H);

            if (active) {
                Draw.roundRect(gfx, rx + 6, y, BeanGui.RAIL_W - 12, BeanGui.RAIL_ROW_H,
                        Math.max(2, radius - 1), theme.accent);
            } else if (hovered) {
                Draw.roundRect(gfx, rx + 6, y, BeanGui.RAIL_W - 12, BeanGui.RAIL_ROW_H,
                        Math.max(2, radius - 1), Colours.withAlpha(theme.text, 16));
            }

            int content = active ? Colours.contrastOn(theme.accent) : (hovered ? theme.text : theme.textDim);
            Draw.glyph(gfx, category.icon(), rx + 16, y + (BeanGui.RAIL_ROW_H - 9) / 2, 1, content);
            Draw.text(gfx, font, category.label(), rx + 32,
                    y + (BeanGui.RAIL_ROW_H - font.lineHeight) / 2 + 1, content);
        }
    }

    /** Which rail row is under the cursor, or null. */
    public static Category railHit(double mx, double my) {
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            if (BeanGui.hit(mx, my, BeanGui.railX() + 6, railRowY(i),
                    BeanGui.RAIL_W - 12, BeanGui.RAIL_ROW_H)) {
                return categories[i];
            }
        }
        return null;
    }

    private static void drawSearch(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY,
                                   boolean interactive, Theme theme) {
        int sx = BeanGui.searchX();
        int sy = BeanGui.searchY();
        int sw = BeanGui.searchW();
        int sh = BeanGui.SEARCH_H;
        boolean focused = BeanGui.isSearchFocused();
        int radius = Math.max(2, theme.cornerRadius - 2);

        if (focused) {
            Draw.roundRectOutlined(gfx, sx, sy, sw, sh, radius, theme.panelAlt,
                    Colours.withAlpha(theme.accent, 190));
        } else {
            boolean hovered = interactive && BeanGui.hit(mouseX, mouseY, sx, sy, sw, sh);
            Draw.roundRect(gfx, sx, sy, sw, sh, radius,
                    hovered ? Colours.lighten(theme.panelAlt, 0.05f) : theme.panelAlt);
        }

        Draw.glyph(gfx, Draw.SEARCH, sx + 9, sy + (sh - 9) / 2, 1, focused ? theme.accent : theme.textDim);

        String query = BeanGui.query();
        int textY = sy + (sh - font.lineHeight) / 2 + 1;
        int textX = sx + 24;
        int maxTextW = sw - 24 - 22;

        if (query.isEmpty()) {
            Draw.text(gfx, font, "Search modules...", textX, textY, Colours.fade(theme.textDim, 0.7f));
        } else {
            Draw.text(gfx, font, Draw.clip(font, query, maxTextW), textX, textY, theme.text);
        }

        // Caret blinks off the wall clock so it keeps ticking while the game does not.
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caretX = textX + Math.min(font.width(query), maxTextW);
            Draw.rect(gfx, caretX + 1, sy + 6, 1, sh - 12, theme.accent);
        }

        if (!query.isEmpty()) {
            boolean hoverClear = interactive && inSearchClear(mouseX, mouseY);
            int clearX = sx + sw - 14;
            int clearColour = hoverClear ? theme.accent : theme.textDim;
            for (int i = 0; i < 5; i++) {
                Draw.rect(gfx, clearX - 2 + i, sy + sh / 2 - 2 + i, 1, 1, clearColour);
                Draw.rect(gfx, clearX + 2 - i, sy + sh / 2 - 2 + i, 1, 1, clearColour);
            }
        }
    }

    public static boolean inSearchBar(double mx, double my) {
        return BeanGui.hit(mx, my, BeanGui.searchX(), BeanGui.searchY(),
                BeanGui.searchW(), BeanGui.SEARCH_H);
    }

    public static boolean inSearchClear(double mx, double my) {
        int clearX = BeanGui.searchX() + BeanGui.searchW() - 14;
        return BeanGui.hit(mx, my, clearX - 6, BeanGui.searchY() + BeanGui.SEARCH_H / 2 - 6, 13, 13);
    }

    private static void drawList(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY,
                                 boolean interactive, Theme theme) {
        int top = BeanGui.listTop();
        int bottom = BeanGui.listBottom();
        int lx = BeanGui.listX();
        int lw = BeanGui.listW();
        if (bottom <= top) {
            return;
        }

        List<Row> rows = layoutRows();
        gfx.enableScissor(lx, top, lx + lw, bottom);

        if (rows.isEmpty()) {
            String message = BeanGui.query().isEmpty()
                    ? "Nothing registered in " + BeanGui.selected().label()
                    : "No modules match \"" + Draw.clip(font, BeanGui.query(), lw - 80) + "\"";
            Draw.text(gfx, font, message, lx + 12, top + 14, theme.textDim);
        }

        boolean cursorInList = interactive && mouseY >= top && mouseY < bottom;
        for (Row row : rows) {
            if (row.y() > bottom || row.y() + row.blockH() < top) {
                continue;
            }
            drawRow(gfx, font, row, mouseX, mouseY, cursorInList, theme, lx, lw);
        }

        gfx.disableScissor();
        drawScrollbar(gfx, theme, top, bottom);
    }

    private static void drawRow(GuiGraphicsExtractor gfx, Font font, Row row, int mouseX, int mouseY,
                                boolean cursorInList, Theme theme, int lx, int lw) {
        Module module = row.module();
        int y = row.y();
        int radius = Math.max(2, theme.cornerRadius - 2);

        boolean hovered = cursorInList && BeanGui.hit(mouseX, mouseY, lx, y, lw, BeanGui.ROW_H);
        float on = BeanGui.toggleAnim(module);

        int base = Colours.mix(theme.panelAlt, theme.accent, on * 0.10f);
        Draw.roundRect(gfx, lx, y, lw, row.blockH(), radius,
                hovered ? Colours.lighten(base, 0.05f) : base);

        // Accent bar down the left edge, fading in with the toggle.
        if (on > 0.01f) {
            Draw.roundRect(gfx, lx, y + 5, 3, BeanGui.ROW_H - 10, 1, Colours.fade(theme.accent, on));
        }

        int nameColour = Colours.mix(theme.textDim, theme.text, Math.max(on, hovered ? 1f : 0f));
        int nameX = lx + 14;
        int nameMax = gearX() - nameX - 10;
        int textY = y + (BeanGui.ROW_H - font.lineHeight) / 2 + 1;

        // A search spans every category, so results say where they came from.
        boolean searching = !BeanGui.query().isEmpty();
        String tag = searching ? module.category().label() : "";
        int tagW = searching ? font.width(tag) + 10 : 0;

        String name = Draw.clip(font, module.name(), nameMax - tagW);
        Draw.text(gfx, font, name, nameX, textY, nameColour);
        if (searching && font.width(name) + tagW <= nameMax) {
            Draw.text(gfx, font, tag, nameX + font.width(name) + 10, textY,
                    Colours.fade(theme.textDim, 0.65f));
        }

        if (module.hasSettings()) {
            boolean hoverGear = cursorInList && inGear(row, mouseX, mouseY);
            int gearColour = module.isExpanded() ? theme.accent : (hoverGear ? theme.text : theme.textDim);
            Draw.glyph(gfx, Draw.GEAR, gearX(), y + (BeanGui.ROW_H - 9) / 2, 1, gearColour);
        }

        Draw.toggleSwitch(gfx, toggleX(), y + (BeanGui.ROW_H - BeanGui.TOGGLE_H) / 2,
                BeanGui.TOGGLE_W, BeanGui.TOGGLE_H, on, theme);

        if (module.drawerProgress() > 0.01f) {
            drawDrawer(gfx, font, row, mouseX, mouseY, cursorInList, theme, lx, lw);
        }
    }

    private static void drawDrawer(GuiGraphicsExtractor gfx, Font font, Row row, int mouseX, int mouseY,
                                   boolean cursorInList, Theme theme, int lx, int lw) {
        Module module = row.module();
        int drawerTop = row.drawerTop();
        int visible = Math.round(BeanGui.drawerHeight(module) * module.drawerProgress());
        if (visible <= 2) {
            return;
        }

        // Clip to the animating height so the settings slide out from under the row.
        gfx.enableScissor(lx, drawerTop, lx + lw, drawerTop + visible);
        Draw.rect(gfx, lx + 14, drawerTop, lw - 28, 1, Colours.withAlpha(theme.text, 20));

        List<Setting> settings = module.settings();
        for (int i = 0; i < settings.size(); i++) {
            drawSetting(gfx, font, settings.get(i), settingY(row, i), mouseX, mouseY,
                    cursorInList, theme);
        }
        gfx.disableScissor();
    }

    private static void drawSetting(GuiGraphicsExtractor gfx, Font font, Setting setting, int y,
                                    int mouseX, int mouseY, boolean cursorInList, Theme theme) {
        int sx = settingX();
        int sw = settingW();
        int textY = y + (BeanGui.SETTING_H - font.lineHeight) / 2;
        boolean hovered = cursorInList && BeanGui.hit(mouseX, mouseY, sx, y, sw, BeanGui.SETTING_H);

        Draw.text(gfx, font, Draw.clip(font, setting.name(), 86), sx, textY,
                hovered ? theme.text : theme.textDim);

        switch (setting.type()) {
            case TOGGLE -> {
                int tw = 22;
                int th = 11;
                Draw.toggleSwitch(gfx, sx + sw - tw, y + (BeanGui.SETTING_H - th) / 2, tw, th,
                        setting.boolValue() ? 1f : 0f, theme);
            }
            case SLIDER -> {
                int[] bounds = sliderBounds();
                Draw.slider(gfx, bounds[0], y + 3, bounds[1], BeanGui.SETTING_H - 6,
                        (float) setting.fraction(), theme);
                Draw.textRight(gfx, font, setting.displayValue(), sx + sw, textY, theme.text);
            }
            case MODE -> {
                Draw.textRight(gfx, font, setting.displayValue(), sx + sw - 12, textY, theme.accent);
                Draw.glyph(gfx, Draw.CHEVRON_DOWN, sx + sw - 9, y + (BeanGui.SETTING_H - 9) / 2, 1,
                        hovered ? theme.accent : theme.textDim);
            }
        }
    }

    public static boolean inGear(Row row, double mx, double my) {
        return row.module().hasSettings()
                && BeanGui.hit(mx, my, gearX() - 4, row.y() + (BeanGui.ROW_H - 9) / 2 - 4, 17, 17);
    }

    public static boolean inToggle(Row row, double mx, double my) {
        return BeanGui.hit(mx, my, toggleX() - 3, row.y() + (BeanGui.ROW_H - BeanGui.TOGGLE_H) / 2 - 3,
                BeanGui.TOGGLE_W + 6, BeanGui.TOGGLE_H + 6);
    }

    private static void drawScrollbar(GuiGraphicsExtractor gfx, Theme theme, int top, int bottom) {
        float max = BeanGui.maxScroll();
        if (max <= 0.5f) {
            return;
        }
        int trackH = bottom - top;
        float viewRatio = trackH / (trackH + max);
        int thumbH = Math.max(18, Math.round(trackH * viewRatio));
        int thumbY = top + Math.round((trackH - thumbH) * (BeanGui.scroll() / max));
        // Sits in the panel's right padding rather than over the rows, so a
        // long list never has the bar clipping its rounded corners.
        int barX = BeanGui.panelX() + BeanGui.panelW() - 7;
        Draw.roundRect(gfx, barX, top, 3, trackH, 1, Colours.withAlpha(theme.text, 14));
        Draw.roundRect(gfx, barX, thumbY, 3, thumbH, 1, Colours.withAlpha(theme.accent, 150));
    }

    private static void drawGrip(GuiGraphicsExtractor gfx, Theme theme, int mouseX, int mouseY,
                                 boolean interactive) {
        int gx = BeanGui.x() + BeanGui.width() - BeanGui.GRIP;
        int gy = BeanGui.y() + BeanGui.height() - BeanGui.GRIP;
        boolean hovered = interactive && BeanGui.inGrip(mouseX, mouseY);
        int colour = hovered ? theme.accent : Colours.withAlpha(theme.textDim, 120);
        for (int i = 0; i < 3; i++) {
            int offset = i * 3;
            Draw.rect(gfx, gx + 8 - offset, gy + 8, 2, 2, colour);
            Draw.rect(gfx, gx + 8, gy + 8 - offset, 2, 2, colour);
        }
    }
}
