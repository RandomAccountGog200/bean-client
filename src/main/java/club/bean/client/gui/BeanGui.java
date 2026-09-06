package club.bean.client.gui;

import club.bean.client.BeanConfig;
import club.bean.client.module.Category;
import club.bean.client.module.Module;
import club.bean.client.module.ModuleRegistry;

import java.util.List;

/**
 * All the state the window has, plus the geometry everything else derives from.
 *
 * <p>It lives outside both hosts on purpose. {@code BeanGuiScreen} owns input
 * while the window is open, and {@code BeanHudOverlay} keeps drawing the same
 * window for the last few frames of the close animation after that screen is
 * gone - they agree on what to draw because they both read this.
 */
public final class BeanGui {
    // ---- fixed layout metrics --------------------------------------------
    public static final int PAD = 11;
    public static final int TITLE_H = 36;
    public static final int RAIL_W = 122;
    public static final int RAIL_ROW_H = 28;
    public static final int RAIL_GAP = 3;
    public static final int GUTTER = 9;

    public static final int SEARCH_H = 27;
    public static final int ROW_H = 32;
    public static final int ROW_GAP = 5;
    public static final int SETTING_H = 24;
    public static final int DRAWER_PAD = 7;

    public static final int TOGGLE_W = 30;
    public static final int TOGGLE_H = 16;
    public static final int GEAR = 10;
    public static final int GRIP = 13;

    public static final int MIN_W = 440;
    public static final int MIN_H = 260;
    public static final int MAX_W = 920;
    public static final int MAX_H = 660;

    // ---- window state -----------------------------------------------------
    private static int x = 60;
    private static int y = 60;
    private static int width = 560;
    private static int height = 360;

    private static boolean open;
    /** 0 = fully hidden, 1 = fully shown. Drives both the fade and the scale. */
    private static float progress;
    private static long lastFrameNanos;

    private static Category selected = Category.COMBAT;
    private static String query = "";
    private static boolean searchFocused;

    private static float scroll;
    private static float scrollTarget;

    /** Eased 0-1 per module, so toggles slide instead of snapping. */
    private static final java.util.Map<String, Float> TOGGLE_ANIM = new java.util.HashMap<>();

    /** Y of the sliding pill behind the selected rail tab. */
    private static float railIndicator = Float.NaN;
    /** When the current list last changed, for the staggered row entrance. */
    private static long listRevealAt;
    /** Seconds elapsed in the last frame, so the renderer can ease hover states. */
    private static float lastDelta = 1f / 60f;

    private BeanGui() {
    }

    // ---- persistence ------------------------------------------------------

    public static void load() {
        x = BeanConfig.getInt("gui.x", 60);
        y = BeanConfig.getInt("gui.y", 60);
        width = Anim.clamp(BeanConfig.getInt("gui.w", 560), MIN_W, MAX_W);
        height = Anim.clamp(BeanConfig.getInt("gui.h", 360), MIN_H, MAX_H);
        selected = Category.byName(BeanConfig.getString("gui.category", Category.COMBAT.name()));
    }

    private static void persistWindow() {
        BeanConfig.setInt("gui.x", x);
        BeanConfig.setInt("gui.y", y);
        BeanConfig.setInt("gui.w", width);
        BeanConfig.setInt("gui.h", height);
        BeanConfig.saveSoon();
    }

    /** Nudges the window back on-screen after a resolution change. */
    public static void clampToScreen(int screenW, int screenH) {
        width = Anim.clamp(width, MIN_W, Math.max(MIN_W, Math.min(MAX_W, screenW - 8)));
        height = Anim.clamp(height, MIN_H, Math.max(MIN_H, Math.min(MAX_H, screenH - 8)));
        x = Anim.clamp(x, 4 - width + 60, Math.max(4, screenW - 60));
        y = Anim.clamp(y, 4, Math.max(4, screenH - TITLE_H));
    }

    // ---- open / close -----------------------------------------------------

    public static boolean isOpen() {
        return open;
    }

    public static void setOpen(boolean value) {
        if (open == value) {
            return;
        }
        open = value;
        if (open) {
            searchFocused = true;
            query = "";
            revealList();
            railIndicator = Float.NaN;
        } else {
            searchFocused = false;
            persistWindow();
        }
    }

    /** Restarts the staggered row entrance. */
    public static void revealList() {
        listRevealAt = System.nanoTime();
    }

    /**
     * How far row {@code index} has slid into place, 0-1. Each row starts a
     * little after the one above it, which is what makes a list change read as
     * a sweep rather than a jump.
     */
    public static float rowReveal(int index) {
        float elapsed = (System.nanoTime() - listRevealAt) / 1_000_000_000f;
        float delayed = elapsed - index * 0.025f;
        return Anim.easeOutCubic(Anim.clamp01(delayed / 0.19f));
    }

    public static float lastDelta() {
        return lastDelta;
    }

    /** Animated Y of the rail's selection pill. */
    public static float railIndicator() {
        return Float.isNaN(railIndicator) ? railRowY(selected.ordinal()) : railIndicator;
    }

    /** True while the window is still worth drawing - open, or mid fade-out. */
    public static boolean isVisible() {
        return open || progress > 0.001f;
    }

    public static float progress() {
        return progress;
    }

    /** Advances every animation. Called once per frame by whichever host is drawing. */
    public static void tickAnimations() {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 1f / 60f : (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        delta = Math.min(delta, 0.1f);
        lastDelta = delta;

        progress = Anim.approach(progress, open ? 1f : 0f, delta, 15f);
        scroll = Anim.approach(scroll, scrollTarget, delta, 20f);

        // The rail's selection pill slides between tabs. Eased here rather than
        // while drawing, so it keeps moving even on a frame nothing renders.
        float railTarget = railRowY(selected.ordinal());
        railIndicator = Float.isNaN(railIndicator)
                ? railTarget
                : Anim.approach(railIndicator, railTarget, delta, 19f);

        for (Module module : ModuleRegistry.all()) {
            float target = module.isEnabled() ? 1f : 0f;
            TOGGLE_ANIM.put(module.id(), Anim.approach(toggleAnim(module), target, delta, 18f));
            module.setDrawerProgress(
                    Anim.approach(module.drawerProgress(), module.isExpanded() ? 1f : 0f, delta, 16f));
        }
    }

    public static float toggleAnim(Module module) {
        return TOGGLE_ANIM.getOrDefault(module.id(), module.isEnabled() ? 1f : 0f);
    }

    // ---- selection & search -----------------------------------------------

    public static Category selected() {
        return selected;
    }

    public static void select(Category category) {
        if (selected == category) {
            return;
        }
        selected = category;
        // Picking a tab means "show me this tab" - leaving the client-wide
        // search active would show the same cross-category results instead.
        query = "";
        scrollTarget = 0;
        scroll = 0;
        revealList();
        BeanConfig.setString("gui.category", category.name());
        BeanConfig.saveSoon();
    }

    public static String query() {
        return query;
    }

    public static void setQuery(String value) {
        String next = value == null ? "" : value;
        if (!next.equals(query)) {
            revealList();
        }
        query = next;
        scrollTarget = 0;
    }

    public static boolean isSearchFocused() {
        return searchFocused;
    }

    public static void setSearchFocused(boolean value) {
        searchFocused = value;
    }

    public static List<Module> visibleModules() {
        return ModuleRegistry.search(selected, query);
    }

    // ---- scrolling --------------------------------------------------------

    public static float scroll() {
        return scroll;
    }

    public static void scrollBy(double amount) {
        scrollTarget = (float) Anim.clamp(scrollTarget + amount, 0, maxScroll());
    }

    public static void clampScroll() {
        scrollTarget = (float) Anim.clamp(scrollTarget, 0, maxScroll());
        scroll = (float) Anim.clamp(scroll, 0, maxScroll());
    }

    public static float maxScroll() {
        return Math.max(0, contentHeight() - listH());
    }

    /** Total height of every row plus its open drawer, in the current category. */
    public static float contentHeight() {
        if (selected.isSpecial()) {
            return 0;
        }
        float total = 0;
        for (Module module : visibleModules()) {
            total += blockHeight(module) + ROW_GAP;
        }
        return Math.max(0, total - ROW_GAP);
    }

    public static float blockHeight(Module module) {
        return ROW_H + drawerHeight(module) * module.drawerProgress();
    }

    public static float drawerHeight(Module module) {
        if (!module.hasSettings()) {
            return 0;
        }
        return module.settings().size() * SETTING_H + DRAWER_PAD * 2;
    }

    // ---- geometry ---------------------------------------------------------

    public static int x() {
        return x;
    }

    public static int y() {
        return y;
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }

    public static void moveTo(int newX, int newY) {
        x = newX;
        y = newY;
    }

    public static void resizeTo(int newW, int newH) {
        width = Anim.clamp(newW, MIN_W, MAX_W);
        height = Anim.clamp(newH, MIN_H, MAX_H);
        clampScroll();
    }

    public static void commitGeometry() {
        persistWindow();
    }

    public static int titleH() {
        return TITLE_H;
    }

    public static int railX() {
        return x + PAD;
    }

    public static int railY() {
        return y + TITLE_H;
    }

    public static int railH() {
        return height - TITLE_H - PAD;
    }

    /** Top of the {@code index}-th tab in the rail. */
    public static int railRowY(int index) {
        return railY() + 11 + index * (RAIL_ROW_H + RAIL_GAP);
    }

    public static int panelX() {
        return x + PAD + RAIL_W + GUTTER;
    }

    public static int panelY() {
        return y + TITLE_H;
    }

    public static int panelW() {
        return width - PAD * 2 - RAIL_W - GUTTER;
    }

    public static int panelH() {
        return height - TITLE_H - PAD;
    }

    public static int searchX() {
        return panelX() + 10;
    }

    public static int searchY() {
        return panelY() + 10;
    }

    public static int searchW() {
        return panelW() - 20;
    }

    public static int listX() {
        return panelX() + 10;
    }

    public static int listW() {
        return panelW() - 20;
    }

    public static int listTop() {
        return searchY() + SEARCH_H + 9;
    }

    public static int listBottom() {
        return panelY() + panelH() - 9;
    }

    public static int listH() {
        return Math.max(0, listBottom() - listTop());
    }

    /** Title-bar rectangle - the drag handle. */
    public static boolean inTitleBar(double mx, double my) {
        return hit(mx, my, x, y, width, TITLE_H);
    }

    /** Bottom-right resize grip. */
    public static boolean inGrip(double mx, double my) {
        return hit(mx, my, x + width - GRIP, y + height - GRIP, GRIP, GRIP);
    }

    public static boolean inWindow(double mx, double my) {
        return hit(mx, my, x, y, width, height);
    }

    public static boolean hit(double mx, double my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
    }
}
