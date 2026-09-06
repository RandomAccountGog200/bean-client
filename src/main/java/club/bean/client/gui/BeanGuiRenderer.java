package club.bean.client.gui;

import club.bean.client.module.Category;
import club.bean.client.module.Module;
import club.bean.client.module.ModuleRegistry;
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
 * <p>Nothing here knows what a module <em>does</em> — it walks
 * {@code ModuleRegistry} and draws names, booleans and settings.
 */
public final class BeanGuiRenderer {
    /** One module row plus its drawer, positioned in screen space. */
    public record Row(Module module, int index, int y, int rowH, int blockH) {
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
        int index = 0;
        for (Module module : BeanGui.visibleModules()) {
            int blockH = Math.round(BeanGui.blockHeight(module));
            rows.add(new Row(module, index++, y, BeanGui.ROW_H, blockH));
            y += blockH + BeanGui.ROW_GAP;
        }
        return rows;
    }

    public static int toggleX() {
        return BeanGui.listX() + BeanGui.listW() - 14 - BeanGui.TOGGLE_W;
    }

    public static int gearX() {
        return toggleX() - 13 - BeanGui.GEAR;
    }

    /** Y of the {@code index}-th setting inside an open drawer. */
    public static int settingY(Row row, int index) {
        return row.drawerTop() + BeanGui.DRAWER_PAD + index * BeanGui.SETTING_H;
    }

    public static int settingX() {
        return BeanGui.listX() + 17;
    }

    public static int settingW() {
        return BeanGui.listW() - 34;
    }

    /** Slider track for a settings row, as {@code {x, width}}. */
    public static int[] sliderBounds() {
        int x = settingX() + 96;
        int right = settingX() + settingW() - 42;
        return new int[] { x, Math.max(36, right - x) };
    }

    /** The scale the window is currently drawn at — under 1 while opening. */
    public static float currentScale() {
        return Anim.lerp(0.95f, 1.0f, Anim.easeOutBack(BeanGui.progress()));
    }

    /**
     * Undoes the open animation's scale so hit-testing matches what is on
     * screen. Without this, clicking while the window is still growing would
     * land a few pixels off.
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
     *                    animation — hover highlights are suppressed so the
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
        float scale = currentScale();

        int cx = BeanGui.x() + BeanGui.width() / 2;
        int cy = BeanGui.y() + BeanGui.height() / 2;

        // Scissor rectangles are transformed by the pose in 26.x, so scaling
        // here is safe — clipped content scales with the window.
        gfx.pose().pushMatrix();
        gfx.pose().translate(cx, cy);
        gfx.pose().scale(scale, scale);
        // A few pixels of rise on the way in, which reads as the window
        // settling rather than simply appearing.
        gfx.pose().translate(-cx, -cy + (1f - Anim.easeOutCubic(progress)) * 9f);
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
        double radius = theme.cornerRadius + 2;

        // Soft drop shadow: a few offset rounded rects at low alpha.
        for (int i = 5; i >= 1; i--) {
            Draw.roundRect(gfx, x - i, y - i + 3, w + i * 2, h + i * 2, radius + i,
                    Colours.withAlpha(0xFF000000, 16 - i * 2));
        }

        Draw.roundRect(gfx, x, y, w, h, radius, theme.background);
        Draw.backgroundPattern(gfx, x, y, w, h, theme);
        // Hairline highlight so the window has a defined edge on any backdrop.
        Draw.roundBorder(gfx, x, y, w, h, radius, 1, Colours.withAlpha(theme.text, 26));

        drawTitleBar(gfx, font, mouseX, mouseY, interactive, theme);
        drawRail(gfx, font, mouseX, mouseY, interactive, theme);

        int px = BeanGui.panelX();
        int py = BeanGui.panelY();
        int pw = BeanGui.panelW();
        int ph = BeanGui.panelH();
        double panelRadius = Math.max(2, theme.cornerRadius);
        Draw.roundRect(gfx, px, py, pw, ph, panelRadius, theme.panel);

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
        int centre = y + BeanGui.TITLE_H / 2;

        // Logo: an accent bean with the window background showing in the crease.
        Draw.bean(gfx, x + BeanGui.PAD + 11, centre, 22, 15, theme.accent, theme.background);

        int textX = x + BeanGui.PAD + 26;
        int textY = centre - font.lineHeight / 2;
        Draw.text(gfx, font, "Bean", textX, textY, theme.text);
        Draw.text(gfx, font, " Client", textX + font.width("Bean"), textY, theme.accent);

        int closeCx = x + BeanGui.width() - BeanGui.PAD - 8;
        String status = ThemeManager.current().name + "  ·  " + ModuleRegistry.count() + " modules";
        Draw.textRight(gfx, font, status, closeCx - 18, textY, Colours.fade(theme.textDim, 0.9f));

        boolean hoverClose = interactive && inCloseButton(mouseX, mouseY);
        if (hoverClose) {
            Draw.circle(gfx, closeCx, centre, 9, Colours.withAlpha(theme.text, 22));
        }
        Icons.cross(gfx, closeCx, centre, 8, hoverClose ? theme.accent : theme.textDim);

        Draw.rect(gfx, x + BeanGui.PAD, y + BeanGui.TITLE_H - 1,
                BeanGui.width() - BeanGui.PAD * 2, 1, Colours.withAlpha(theme.text, 16));
    }

    public static boolean inCloseButton(double mx, double my) {
        int cx = BeanGui.x() + BeanGui.width() - BeanGui.PAD - 8;
        int cy = BeanGui.y() + BeanGui.TITLE_H / 2;
        return BeanGui.hit(mx, my, cx - 9, cy - 9, 19, 19);
    }

    private static void drawRail(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY,
                                 boolean interactive, Theme theme) {
        int rx = BeanGui.railX();
        int ry = BeanGui.railY();
        double radius = Math.max(2, theme.cornerRadius);
        Draw.roundRect(gfx, rx, ry, BeanGui.RAIL_W, BeanGui.railH(), radius, theme.panel);

        Category[] categories = Category.values();

        // The pill itself is eased in tickAnimations; here we just draw it.
        float indicator = BeanGui.railIndicator();

        double pillRadius = Math.max(2, radius - 1);
        Draw.roundRect(gfx, rx + 6, indicator, BeanGui.RAIL_W - 12, BeanGui.RAIL_ROW_H,
                pillRadius, theme.accent);

        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            int y = BeanGui.railRowY(i);
            if (y + BeanGui.RAIL_ROW_H > ry + BeanGui.railH()) {
                break;
            }

            boolean hovered = interactive
                    && BeanGui.hit(mouseX, mouseY, rx + 6, y, BeanGui.RAIL_W - 12, BeanGui.RAIL_ROW_H);
            // How much of the pill is under this row right now, so the label
            // cross-fades to the on-accent colour as the pill arrives.
            float covered = 1f - Math.min(1f, Math.abs(indicator - y) / (float) BeanGui.RAIL_ROW_H);

            if (hovered && covered < 0.5f) {
                Draw.roundRect(gfx, rx + 6, y, BeanGui.RAIL_W - 12, BeanGui.RAIL_ROW_H, pillRadius,
                        Colours.withAlpha(theme.text, 14));
            }

            int resting = hovered ? theme.text : theme.textDim;
            int content = Colours.mix(resting, Colours.contrastOn(theme.accent), covered);
            Icons.category(gfx, category, rx + 16, y + (BeanGui.RAIL_ROW_H - 11) / 2, 11, content);
            Draw.text(gfx, font, category.label(), rx + 34,
                    y + (BeanGui.RAIL_ROW_H - font.lineHeight) / 2 + 1, content);
        }
    }

    /** Which rail row is under the cursor, or null. */
    public static Category railHit(double mx, double my) {
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            if (BeanGui.hit(mx, my, BeanGui.railX() + 6, BeanGui.railRowY(i),
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
        boolean hovered = interactive && BeanGui.hit(mouseX, mouseY, sx, sy, sw, sh);
        double radius = Math.max(2, theme.cornerRadius - 1);

        Draw.roundRect(gfx, sx, sy, sw, sh, radius,
                hovered && !focused ? Colours.lighten(theme.panelAlt, 0.05f) : theme.panelAlt);
        if (focused) {
            Draw.roundBorder(gfx, sx, sy, sw, sh, radius, 1, Colours.withAlpha(theme.accent, 200));
        }

        Icons.search(gfx, sx + 9, sy + (sh - 11) / 2, 11, focused ? theme.accent : theme.textDim);

        String query = BeanGui.query();
        int textY = sy + (sh - font.lineHeight) / 2 + 1;
        int textX = sx + 26;
        int maxTextW = sw - 26 - 24;

        if (query.isEmpty()) {
            Draw.text(gfx, font, "Search modules...", textX, textY, Colours.fade(theme.textDim, 0.7f));
        } else {
            Draw.text(gfx, font, Draw.clip(font, query, maxTextW), textX, textY, theme.text);
        }

        // Caret blinks off the wall clock so it keeps ticking while the game does not.
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caretX = textX + Math.min(font.width(query), maxTextW);
            Draw.roundRect(gfx, caretX + 1, sy + 7, 1, sh - 14, 0.5, theme.accent);
        }

        if (!query.isEmpty()) {
            boolean hoverClear = interactive && inSearchClear(mouseX, mouseY);
            Icons.cross(gfx, sx + sw - 14, sy + sh / 2.0, 7,
                    hoverClear ? theme.accent : theme.textDim);
        }
    }

    public static boolean inSearchBar(double mx, double my) {
        return BeanGui.hit(mx, my, BeanGui.searchX(), BeanGui.searchY(),
                BeanGui.searchW(), BeanGui.SEARCH_H);
    }

    public static boolean inSearchClear(double mx, double my) {
        int clearX = BeanGui.searchX() + BeanGui.searchW() - 14;
        return BeanGui.hit(mx, my, clearX - 7, BeanGui.searchY() + BeanGui.SEARCH_H / 2 - 7, 15, 15);
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
                    : "No modules match \"" + Draw.clip(font, BeanGui.query(), lw - 90) + "\"";
            Draw.text(gfx, font, message, lx + 14, top + 16, theme.textDim);
        }

        boolean cursorInList = interactive && mouseY >= top && mouseY < bottom;
        float saved = Draw.opacity();
        for (Row row : rows) {
            if (row.y() > bottom || row.y() + row.blockH() < top) {
                // Off-screen rows still need their hover decayed, or they come
                // back lit when you scroll to them.
                row.module().setHoverProgress(Anim.approach(row.module().hoverProgress(), 0f,
                        BeanGui.lastDelta(), 16f));
                continue;
            }
            drawRow(gfx, font, row, mouseX, mouseY, cursorInList, theme, lx, lw, saved);
        }
        Draw.setOpacity(saved);

        gfx.disableScissor();
        drawScrollbar(gfx, theme, top, bottom);
    }

    private static void drawRow(GuiGraphicsExtractor gfx, Font font, Row row, int mouseX, int mouseY,
                                boolean cursorInList, Theme theme, int lx, int lw, float baseOpacity) {
        Module module = row.module();
        double radius = Math.max(2, theme.cornerRadius - 1);

        boolean hovered = cursorInList && BeanGui.hit(mouseX, mouseY, lx, row.y(), lw, BeanGui.ROW_H);
        float hover = Anim.approach(module.hoverProgress(), hovered ? 1f : 0f,
                BeanGui.lastDelta(), 16f);
        module.setHoverProgress(hover);

        // Staggered entrance: slide in from the left and fade up.
        float reveal = BeanGui.rowReveal(row.index());
        if (reveal <= 0.004f) {
            return;
        }
        Draw.setOpacity(baseOpacity * reveal);
        int y = row.y();
        int offset = Math.round((1f - reveal) * 14f);

        float on = BeanGui.toggleAnim(module);
        int base = Colours.mix(theme.panelAlt, theme.accent, on * 0.08f);
        Draw.roundRect(gfx, lx + offset, y, lw - offset, row.blockH(), radius,
                Colours.lighten(base, hover * 0.06f));

        // Accent bar down the left edge, fading in with the toggle.
        if (on > 0.01f) {
            Draw.roundRect(gfx, lx + offset, y + 6, 3, BeanGui.ROW_H - 12, 1.5,
                    Colours.fade(theme.accent, on));
        }

        int nameColour = Colours.mix(theme.textDim, theme.text, Math.max(on, hover));
        int nameX = lx + offset + 15;
        int nameMax = gearX() - nameX - 12;
        int textY = y + (BeanGui.ROW_H - font.lineHeight) / 2 + 1;

        // A search spans every category, so results say where they came from.
        boolean searching = !BeanGui.query().isEmpty();
        String tag = searching ? module.category().label() : "";
        int tagW = searching ? font.width(tag) + 11 : 0;

        String name = Draw.clip(font, module.name(), nameMax - tagW);
        Draw.text(gfx, font, name, nameX, textY, nameColour);
        if (searching && font.width(name) + tagW <= nameMax) {
            Draw.text(gfx, font, tag, nameX + font.width(name) + 11, textY,
                    Colours.fade(theme.textDim, 0.6f));
        }

        if (module.hasSettings()) {
            boolean hoverGear = cursorInList && inGear(row, mouseX, mouseY);
            int gearColour = module.isExpanded()
                    ? theme.accent
                    : Colours.mix(theme.textDim, theme.text, hoverGear ? 1f : hover * 0.5f);
            Icons.settings(gfx, gearX(), y + (BeanGui.ROW_H - BeanGui.GEAR) / 2, BeanGui.GEAR, gearColour);
        }

        Draw.toggleSwitch(gfx, toggleX(), y + (BeanGui.ROW_H - BeanGui.TOGGLE_H) / 2.0,
                BeanGui.TOGGLE_W, BeanGui.TOGGLE_H, on, theme);

        if (module.drawerProgress() > 0.01f) {
            drawDrawer(gfx, font, row, mouseX, mouseY, cursorInList, theme, lx + offset, lw - offset);
        }
        Draw.setOpacity(baseOpacity);
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
        Draw.rect(gfx, lx + 15, drawerTop, lw - 30, 1, Colours.withAlpha(theme.text, 18));

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

        Draw.text(gfx, font, Draw.clip(font, setting.name(), 90), sx, textY,
                hovered ? theme.text : theme.textDim);

        switch (setting.type()) {
            case TOGGLE -> {
                int tw = 22;
                int th = 12;
                Draw.toggleSwitch(gfx, sx + sw - tw, y + (BeanGui.SETTING_H - th) / 2.0, tw, th,
                        setting.boolValue() ? 1f : 0f, theme);
            }
            case SLIDER -> {
                int[] bounds = sliderBounds();
                Draw.slider(gfx, bounds[0], y + 3, bounds[1], BeanGui.SETTING_H - 6,
                        (float) setting.fraction(), theme);
                Draw.textRight(gfx, font, setting.displayValue(), sx + sw, textY, theme.text);
            }
            case MODE -> {
                Draw.textRight(gfx, font, setting.displayValue(), sx + sw - 14, textY, theme.accent);
                Icons.chevron(gfx, sx + sw - 10, y + (BeanGui.SETTING_H - 9) / 2, 9, 0,
                        hovered ? theme.accent : theme.textDim);
            }
        }
    }

    public static boolean inGear(Row row, double mx, double my) {
        return row.module().hasSettings()
                && BeanGui.hit(mx, my, gearX() - 5, row.y() + (BeanGui.ROW_H - BeanGui.GEAR) / 2 - 5,
                        BeanGui.GEAR + 10, BeanGui.GEAR + 10);
    }

    public static boolean inToggle(Row row, double mx, double my) {
        return BeanGui.hit(mx, my, toggleX() - 4, row.y() + (BeanGui.ROW_H - BeanGui.TOGGLE_H) / 2 - 4,
                BeanGui.TOGGLE_W + 8, BeanGui.TOGGLE_H + 8);
    }

    private static void drawScrollbar(GuiGraphicsExtractor gfx, Theme theme, int top, int bottom) {
        float max = BeanGui.maxScroll();
        if (max <= 0.5f) {
            return;
        }
        int trackH = bottom - top;
        float viewRatio = trackH / (trackH + max);
        int thumbH = Math.max(20, Math.round(trackH * viewRatio));
        int thumbY = top + Math.round((trackH - thumbH) * (BeanGui.scroll() / max));
        // Sits in the panel's right padding rather than over the rows.
        int barX = BeanGui.panelX() + BeanGui.panelW() - 7;
        Draw.roundRect(gfx, barX, top, 3, trackH, 1.5, Colours.withAlpha(theme.text, 13));
        Draw.roundRect(gfx, barX, thumbY, 3, thumbH, 1.5, Colours.withAlpha(theme.accent, 170));
    }

    private static void drawGrip(GuiGraphicsExtractor gfx, Theme theme, int mouseX, int mouseY,
                                 boolean interactive) {
        int gx = BeanGui.x() + BeanGui.width() - BeanGui.GRIP;
        int gy = BeanGui.y() + BeanGui.height() - BeanGui.GRIP;
        boolean hovered = interactive && BeanGui.inGrip(mouseX, mouseY);
        Icons.grip(gfx, gx, gy, BeanGui.GRIP,
                hovered ? theme.accent : Colours.withAlpha(theme.textDim, 110));
    }
}
