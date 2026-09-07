package club.bean.client.module;

/**
 * Reads a module's toggle and its settings by name.
 *
 * <p>Every feature needs the same three lines - find the module, find the
 * setting on it, fall back to a default if either is missing - and before this
 * existed each one carried its own copy. Looking settings up by name rather
 * than by index means reordering a registration cannot silently repoint a
 * feature at the wrong slider.
 *
 * <p>The fallbacks matter more than they look: a feature must not explode if a
 * module was renamed or a setting removed, because the config on disk can be
 * older than the code reading it.
 */
public final class Settings {
    private Settings() {
    }

    /** Whether the module exists and is switched on. */
    public static boolean enabled(String moduleId) {
        Module module = ModuleRegistry.get(moduleId);
        return module != null && module.isEnabled();
    }

    public static boolean flag(String moduleId, String settingName, boolean fallback) {
        Setting setting = find(moduleId, settingName, Setting.Type.TOGGLE);
        return setting == null ? fallback : setting.boolValue();
    }

    public static double number(String moduleId, String settingName, double fallback) {
        Setting setting = find(moduleId, settingName, Setting.Type.SLIDER);
        return setting == null ? fallback : setting.value();
    }

    public static int integer(String moduleId, String settingName, int fallback) {
        return (int) Math.round(number(moduleId, settingName, fallback));
    }

    public static String mode(String moduleId, String settingName, String fallback) {
        Setting setting = find(moduleId, settingName, Setting.Type.MODE);
        return setting == null ? fallback : setting.modeValue();
    }

    private static Setting find(String moduleId, String settingName, Setting.Type type) {
        Module module = ModuleRegistry.get(moduleId);
        if (module == null) {
            return null;
        }
        for (Setting setting : module.settings()) {
            if (setting.type() == type && setting.name().equalsIgnoreCase(settingName)) {
                return setting;
            }
        }
        return null;
    }
}
