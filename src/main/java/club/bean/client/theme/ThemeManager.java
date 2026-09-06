package club.bean.client.theme;

import club.bean.client.BeanClient;
import club.bean.client.BeanConfig;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Loads every {@code *.json} in {@code config/beanclient/themes/} and keeps
 * track of which one is live.
 *
 * <p>The themes that ship with the client are written out of the jar on first
 * run and never overwritten afterwards, so editing {@code bean.json} in place
 * is a supported way to reskin the GUI. Drop in a new file and hit
 * <em>Reload</em> on the Themes tab to pick it up without restarting.
 *
 * <p>Files whose name starts with {@code _} are still extracted but hidden from
 * the picker - that is how {@code _example_minimal.json} stays available as a
 * template without cluttering the dropdown.
 */
public final class ThemeManager {
    private static final String JAR_THEMES = "assets/" + BeanClient.MOD_ID + "/themes";
    private static final String SUFFIX = ".json";

    /** Insertion-ordered so the dropdown is stable across restarts. */
    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();

    private static String activeId = "bean";
    private static Theme cached;

    private ThemeManager() {
    }

    public static Path themesDir() {
        return BeanConfig.dir().resolve("themes");
    }

    public static void init() {
        extractBundledThemes();
        reload();
        activeId = BeanConfig.getString("theme", "bean");
        if (!THEMES.containsKey(activeId)) {
            activeId = THEMES.keySet().iterator().next();
        }
        cached = null;
    }

    /** Re-reads the themes folder from disk. Keeps the current selection when it still exists. */
    public static void reload() {
        THEMES.clear();
        Path dir = themesDir();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(ThemeManager::isThemeFile).sorted().forEach(ThemeManager::readTheme);
            } catch (IOException ex) {
                BeanClient.LOGGER.warn("Could not list {}", dir, ex);
            }
        }
        if (THEMES.isEmpty()) {
            // Never leave the renderer without a theme, even if the folder was
            // emptied mid-session. An empty object gets all the built-in defaults.
            THEMES.put("bean", Theme.fromJson("bean", new JsonObject()));
        }
        if (!THEMES.containsKey(activeId)) {
            activeId = THEMES.keySet().iterator().next();
        }
        cached = null;
        BeanClient.LOGGER.info("Loaded {} theme(s) from {}", THEMES.size(), dir);
    }

    private static boolean isThemeFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(SUFFIX);
    }

    private static void readTheme(Path path) {
        String file = path.getFileName().toString();
        String id = file.substring(0, file.length() - SUFFIX.length());
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            THEMES.put(id, Theme.fromJson(id, json));
        } catch (IOException | RuntimeException ex) {
            BeanClient.LOGGER.warn("Skipping malformed theme {}", file, ex);
        }
    }

    /** Copies the jar's themes into the config folder, without clobbering user edits. */
    private static void extractBundledThemes() {
        Path dir = themesDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            BeanClient.LOGGER.warn("Could not create {}", dir, ex);
            return;
        }

        Optional<Path> bundled = FabricLoader.getInstance()
                .getModContainer(BeanClient.MOD_ID)
                .flatMap(container -> container.findPath(JAR_THEMES));
        if (bundled.isEmpty()) {
            return;
        }

        try (Stream<Path> files = Files.list(bundled.get())) {
            files.filter(ThemeManager::isThemeFile).forEach(source -> {
                Path target = dir.resolve(source.getFileName().toString());
                if (Files.exists(target)) {
                    return;
                }
                try {
                    Files.copy(source, target);
                } catch (IOException ex) {
                    BeanClient.LOGGER.warn("Could not extract theme {}", target, ex);
                }
            });
        } catch (IOException ex) {
            BeanClient.LOGGER.warn("Could not read bundled themes", ex);
        }
    }

    // ---- selection --------------------------------------------------------

    /** Ids of every theme the picker should offer, in file order. */
    public static List<String> pickerIds() {
        List<String> out = new ArrayList<>();
        for (String id : THEMES.keySet()) {
            if (!id.startsWith("_") || id.equals(activeId)) {
                out.add(id);
            }
        }
        return out;
    }

    public static Theme byId(String id) {
        return THEMES.get(id);
    }

    public static String activeId() {
        return activeId;
    }

    public static void select(String id) {
        if (!THEMES.containsKey(id) || id.equals(activeId)) {
            return;
        }
        activeId = id;
        cached = null;
        BeanConfig.setString("theme", id);
        BeanConfig.saveSoon();
        BeanClient.LOGGER.info("Theme -> {}", current().name);
    }

    /** The live theme, with the picker's accent override folded in. */
    public static Theme current() {
        if (cached == null) {
            Theme base = THEMES.get(activeId);
            if (base == null) {
                base = THEMES.values().iterator().next();
            }
            Integer override = Colours.parse(BeanConfig.getString(accentKey(activeId), null));
            cached = override == null ? base : base.withAccent(override);
        }
        return cached;
    }

    public static boolean hasAccentOverride() {
        return BeanConfig.getString(accentKey(activeId), null) != null;
    }

    /** Live-applies a new accent to the current theme and remembers it per theme. */
    public static void setAccent(int argb) {
        BeanConfig.setString(accentKey(activeId), Colours.toHex(argb));
        BeanConfig.saveSoon();
        cached = null;
    }

    public static void resetAccent() {
        BeanConfig.remove(accentKey(activeId));
        BeanConfig.saveSoon();
        cached = null;
    }

    /**
     * Writes the current theme - accent override included - back to its own
     * file, so a colour picked in-game becomes part of the theme on disk.
     */
    public static boolean saveCurrentToFile() {
        Theme theme = current();
        Path path = themesDir().resolve(theme.id + SUFFIX);
        try {
            Files.createDirectories(themesDir());
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(theme.toJson());
            Files.writeString(path, json, StandardCharsets.UTF_8);
            BeanConfig.remove(accentKey(theme.id));
            BeanConfig.saveSoon();
            reload();
            BeanClient.LOGGER.info("Saved theme to {}", path);
            return true;
        } catch (IOException ex) {
            BeanClient.LOGGER.warn("Could not save theme {}", path, ex);
            return false;
        }
    }

    private static String accentKey(String id) {
        return "accent." + id;
    }
}
