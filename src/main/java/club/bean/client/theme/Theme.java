package club.bean.client.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * One skin, loaded from a JSON file in {@code config/beanclient/themes/}.
 *
 * <p>Only five fields are actually required of a theme file - background,
 * panel, text, accent and cornerRadius. Everything else is derived from those
 * so that a hand-written five-line theme still looks deliberate. See
 * {@code themes/_example_minimal.json} in the repo.
 */
public final class Theme {
    /** Filename stem, e.g. {@code bean} for {@code bean.json}. Unique; used as the config key. */
    public final String id;
    public final String name;
    public final String author;
    public final String description;

    public final int background;
    public final int panel;
    public final int panelAlt;
    public final int text;
    public final int textDim;
    /** Base accent from the file. The live accent may be overridden per-theme by the colour picker. */
    public final int accent;

    public final int cornerRadius;

    public final Pattern pattern;
    public final float patternOpacity;
    /** Optional {@code namespace:path} texture drawn behind the panels, or null. */
    public final String backgroundImage;

    public enum Pattern {
        NONE, BEAN, DOTS, GRID;

        static Pattern parse(String raw) {
            if (raw == null) {
                return NONE;
            }
            for (Pattern p : values()) {
                if (p.name().equalsIgnoreCase(raw.trim())) {
                    return p;
                }
            }
            return NONE;
        }
    }

    private Theme(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.author = b.author;
        this.description = b.description;
        this.background = b.background;
        this.panel = b.panel;
        this.panelAlt = b.panelAlt;
        this.text = b.text;
        this.textDim = b.textDim;
        this.accent = b.accent;
        this.cornerRadius = b.cornerRadius;
        this.pattern = b.pattern;
        this.patternOpacity = b.patternOpacity;
        this.backgroundImage = b.backgroundImage;
    }

    /**
     * Reads a theme file. Missing optional fields are filled in from the
     * required ones rather than from a global default, so a theme is always
     * internally consistent.
     */
    public static Theme fromJson(String id, JsonObject json) {
        Builder b = new Builder();
        b.id = id;
        b.name = string(json, "name", prettify(id));
        b.author = string(json, "author", "");
        b.description = string(json, "description", "");

        b.background = colour(json, "background", 0xFF1E150F);
        b.panel = colour(json, "panel", Colours.lighten(b.background, 0.06f));
        b.panelAlt = colour(json, "panelAlt", Colours.lighten(b.panel, 0.06f));
        b.text = colour(json, "text", 0xFFF2E3CC);
        // A dimmed label colour is just the text colour pulled most of the way
        // toward the panel it sits on.
        b.textDim = colour(json, "textDim", Colours.mix(b.text, b.panel, 0.55f));
        b.accent = colour(json, "accent", 0xFFE8912F);

        b.cornerRadius = Math.max(0, Math.min(20, integer(json, "cornerRadius", 8)));

        b.pattern = Pattern.parse(string(json, "pattern", "none"));
        b.patternOpacity = Math.max(0f, Math.min(1f, (float) number(json, "patternOpacity", 0.055)));
        b.backgroundImage = string(json, "backgroundImage", null);
        if (b.backgroundImage != null && b.backgroundImage.isBlank()) {
            b.backgroundImage = null;
        }
        return new Theme(b);
    }

    /** Returns a copy of this theme with a different accent - used by the live colour picker. */
    public Theme withAccent(int newAccent) {
        Builder b = new Builder();
        b.id = id;
        b.name = name;
        b.author = author;
        b.description = description;
        b.background = background;
        b.panel = panel;
        b.panelAlt = panelAlt;
        b.text = text;
        b.textDim = textDim;
        b.accent = 0xFF000000 | newAccent;
        b.cornerRadius = cornerRadius;
        b.pattern = pattern;
        b.patternOpacity = patternOpacity;
        b.backgroundImage = backgroundImage;
        return new Theme(b);
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        if (!author.isEmpty()) {
            o.addProperty("author", author);
        }
        if (!description.isEmpty()) {
            o.addProperty("description", description);
        }
        o.addProperty("background", Colours.toHex(background));
        o.addProperty("panel", Colours.toHex(panel));
        o.addProperty("panelAlt", Colours.toHex(panelAlt));
        o.addProperty("text", Colours.toHex(text));
        o.addProperty("textDim", Colours.toHex(textDim));
        o.addProperty("accent", Colours.toHex(accent));
        o.addProperty("cornerRadius", cornerRadius);
        o.addProperty("pattern", pattern.name().toLowerCase(java.util.Locale.ROOT));
        o.addProperty("patternOpacity", patternOpacity);
        if (backgroundImage == null) {
            o.add("backgroundImage", com.google.gson.JsonNull.INSTANCE);
        } else {
            o.addProperty("backgroundImage", backgroundImage);
        }
        return o;
    }

    // ---- json helpers -----------------------------------------------------

    private static String string(JsonObject o, String key, String def) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? def : e.getAsString();
    }

    private static int integer(JsonObject o, String key, int def) {
        JsonElement e = o.get(key);
        try {
            return e == null || e.isJsonNull() ? def : e.getAsInt();
        } catch (RuntimeException ex) {
            return def;
        }
    }

    private static double number(JsonObject o, String key, double def) {
        JsonElement e = o.get(key);
        try {
            return e == null || e.isJsonNull() ? def : e.getAsDouble();
        } catch (RuntimeException ex) {
            return def;
        }
    }

    private static int colour(JsonObject o, String key, int def) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) {
            return def;
        }
        Integer parsed = Colours.parse(e.getAsString());
        return parsed == null ? def : parsed;
    }

    private static String prettify(String id) {
        String spaced = id.replace('_', ' ').replace('-', ' ').trim();
        if (spaced.isEmpty()) {
            return id;
        }
        StringBuilder out = new StringBuilder();
        for (String word : spaced.split("\s+")) {
            out.append(Character.toUpperCase(word.charAt(0)))
               .append(word.substring(1))
               .append(' ');
        }
        return out.toString().trim();
    }

    private static final class Builder {
        String id;
        String name;
        String author;
        String description;
        int background;
        int panel;
        int panelAlt;
        int text;
        int textDim;
        int accent;
        int cornerRadius;
        Pattern pattern;
        float patternOpacity;
        String backgroundImage;
    }
}
