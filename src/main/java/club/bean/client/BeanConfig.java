package club.bean.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flat key/value JSON config at {@code config/beanclient/config.json}.
 *
 * <p>Holds the window rectangle, the selected theme and any accent overrides,
 * plus one boolean per module so toggles survive a restart. Writes are
 * debounced by {@link #saveSoon()} - the GUI calls it on every interaction and
 * the actual file write happens at most once a second.
 */
public final class BeanConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Boolean> BOOLS = new LinkedHashMap<>();
    private static final Map<String, Double> NUMBERS = new LinkedHashMap<>();
    private static final Map<String, String> STRINGS = new LinkedHashMap<>();

    private static long dirtySince = -1L;

    private BeanConfig() {
    }

    /** {@code config/beanclient/} - also the parent of the user's {@code themes/} folder. */
    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve(BeanClient.MOD_ID);
    }

    public static Path file() {
        return dir().resolve("config.json");
    }

    public static boolean getBool(String key, boolean def) {
        return BOOLS.getOrDefault(key, def);
    }

    public static void setBool(String key, boolean value) {
        BOOLS.put(key, value);
    }

    public static double getNumber(String key, double def) {
        return NUMBERS.getOrDefault(key, def);
    }

    public static void setNumber(String key, double value) {
        NUMBERS.put(key, value);
    }

    public static int getInt(String key, int def) {
        return (int) Math.round(getNumber(key, def));
    }

    public static void setInt(String key, int value) {
        setNumber(key, value);
    }

    public static String getString(String key, String def) {
        String v = STRINGS.get(key);
        return v == null ? def : v;
    }

    public static void setString(String key, String value) {
        if (value == null) {
            STRINGS.remove(key);
        } else {
            STRINGS.put(key, value);
        }
    }

    public static void remove(String key) {
        BOOLS.remove(key);
        NUMBERS.remove(key);
        STRINGS.remove(key);
    }

    /** Marks the config dirty; {@link #flush()} writes it on the next client tick past the delay. */
    public static void saveSoon() {
        if (dirtySince < 0) {
            dirtySince = System.currentTimeMillis();
        }
    }

    /** Called every client tick. Writes at most once per second, and only when something changed. */
    public static void flush() {
        if (dirtySince >= 0 && System.currentTimeMillis() - dirtySince > 1000L) {
            save();
        }
    }

    public static void load() {
        BOOLS.clear();
        NUMBERS.clear();
        STRINGS.clear();
        dirtySince = -1L;

        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                JsonElement value = entry.getValue();
                if (!value.isJsonPrimitive()) {
                    continue;
                }
                var primitive = value.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    BOOLS.put(entry.getKey(), primitive.getAsBoolean());
                } else if (primitive.isNumber()) {
                    NUMBERS.put(entry.getKey(), primitive.getAsDouble());
                } else {
                    STRINGS.put(entry.getKey(), primitive.getAsString());
                }
            }
        } catch (IOException | RuntimeException ex) {
            BeanClient.LOGGER.warn("Could not read {} - starting from defaults", path, ex);
        }
    }

    public static void save() {
        dirtySince = -1L;
        JsonObject root = new JsonObject();
        BOOLS.forEach(root::addProperty);
        NUMBERS.forEach(root::addProperty);
        STRINGS.forEach(root::addProperty);
        try {
            Files.createDirectories(dir());
            Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            BeanClient.LOGGER.warn("Could not write {}", file(), ex);
        }
    }
}
