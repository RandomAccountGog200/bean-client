package club.bean.client.module;

/**
 * The tabs down the left-hand rail, in display order.
 *
 * <p>Adding a category is a one-line change here — the rail, the search filter
 * and the module list all iterate {@link #values()}.
 *
 * <p>The seven built-ins are drawn as vectors by {@code Icons}, so they stay
 * smooth at any GUI scale. A category added with the second constructor
 * supplies a 9x9 pixel mask instead ({@code '#'} is on), which is a lot less
 * work than drawing vectors and is all most people will want.
 */
public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    VISUAL("Visual"),
    PLAYER("Player"),
    WORLD("World"),
    MISC("Misc"),
    /**
     * The one category that does not list modules. The main panel swaps to the
     * theme editor instead — see {@code ThemeTab}.
     */
    THEMES("Themes", true);

    private final String label;
    private final boolean special;
    private final String[] fallbackIcon;

    Category(String label) {
        this(label, false, null);
    }

    Category(String label, boolean special) {
        this(label, special, null);
    }

    /** For categories added by hand that want a pixel-mask icon. */
    Category(String label, String[] fallbackIcon) {
        this(label, false, fallbackIcon);
    }

    Category(String label, boolean special, String[] fallbackIcon) {
        this.label = label;
        this.special = special;
        this.fallbackIcon = fallbackIcon;
    }

    public String label() {
        return label;
    }

    /** A 9x9 mask to draw instead of the built-in vector icon, or null. */
    public String[] fallbackIcon() {
        return fallbackIcon;
    }

    /** True for categories that render their own panel rather than a module list. */
    public boolean isSpecial() {
        return special;
    }

    /** Matches {@code registerModule("...", "combat", ...)} case-insensitively. */
    public static Category byName(String name) {
        for (Category category : values()) {
            if (category.name().equalsIgnoreCase(name) || category.label.equalsIgnoreCase(name)) {
                return category;
            }
        }
        return MISC;
    }
}
