package club.bean.client.module;

/**
 * The tabs down the left-hand rail, in display order.
 *
 * <p>Adding a category is a one-line change here - the rail, the search filter
 * and the module list all iterate {@link #values()}. The {@code icon} is a 9x9
 * pixel mask drawn by {@code Draw.glyph}; {@code #} is on, anything else is off.
 */
public enum Category {
    COMBAT("Combat", new String[] {
            "..#....#.",
            "..##..##.",
            "...####..",
            "....##...",
            "...####..",
            "..##..##.",
            ".##....##",
            ".#......#",
            "........."
    }),
    MOVEMENT("Movement", new String[] {
            "....#....",
            "...###...",
            "..##.##..",
            ".##...##.",
            ".........",
            "....#....",
            "...###...",
            "..##.##..",
            ".##...##."
    }),
    VISUAL("Visual", new String[] {
            ".........",
            "..#####..",
            ".#.....#.",
            "#..###..#",
            "#.##.##.#",
            "#..###..#",
            ".#.....#.",
            "..#####..",
            "........."
    }),
    PLAYER("Player", new String[] {
            "...###...",
            "..#...#..",
            "..#...#..",
            "...###...",
            ".#######.",
            "#..###..#",
            "#..###..#",
            "...#.#...",
            "..##.##.."
    }),
    WORLD("World", new String[] {
            "..#####..",
            ".##...##.",
            "#.#.#.#.#",
            "#..#.#..#",
            "#########",
            "#..#.#..#",
            "#.#.#.#.#",
            ".##...##.",
            "..#####.."
    }),
    MISC("Misc", new String[] {
            ".........",
            ".........",
            ".........",
            ".........",
            ".##.##.##",
            ".##.##.##",
            ".........",
            ".........",
            "........."
    }),
    /**
     * The one category that does not list modules. The main panel swaps to the
     * theme editor instead - see {@code ThemeTab}.
     */
    THEMES("Themes", new String[] {
            "..#####..",
            ".##...##.",
            "##..#..##",
            "#.#####.#",
            "#.##.##.#",
            "#..###..#",
            "##.....##",
            ".##...##.",
            "..#####.."
    }, true);

    private final String label;
    private final String[] icon;
    private final boolean special;

    Category(String label, String[] icon) {
        this(label, icon, false);
    }

    Category(String label, String[] icon, boolean special) {
        this.label = label;
        this.icon = icon;
        this.special = special;
    }

    public String label() {
        return label;
    }

    public String[] icon() {
        return icon;
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
