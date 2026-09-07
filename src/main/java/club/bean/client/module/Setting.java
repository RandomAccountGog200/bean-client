package club.bean.client.module;

import club.bean.client.BeanConfig;

import java.util.List;
import java.util.Locale;

/**
 * One row inside a module's settings drawer.
 *
 * <p>Three shapes cover everything the shell needs to draw: a checkbox, a
 * slider and a left/right mode cycler. Values persist to the config under
 * {@code setting.<module>.<setting>} - the shell has no logic of its own, but a
 * real module wired in later gets its settings restored for free.
 */
public final class Setting {
    public enum Type { TOGGLE, SLIDER, MODE }

    private final String name;
    private final Type type;

    // TOGGLE
    private boolean boolValue;

    // SLIDER
    private double value;
    private final double min;
    private final double max;
    private final int decimals;

    // MODE
    private final List<String> options;
    private int index;

    private Module owner;

    private Setting(String name, Type type, boolean boolValue, double value,
                    double min, double max, int decimals, List<String> options) {
        this.name = name;
        this.type = type;
        this.boolValue = boolValue;
        this.value = value;
        this.min = min;
        this.max = max;
        this.decimals = decimals;
        this.options = options;
    }

    public static Setting toggle(String name, boolean def) {
        return new Setting(name, Type.TOGGLE, def, 0, 0, 1, 0, List.of());
    }

    public static Setting slider(String name, double def, double min, double max, int decimals) {
        return new Setting(name, Type.SLIDER, false, def, min, max, decimals, List.of());
    }

    public static Setting mode(String name, String... options) {
        return new Setting(name, Type.MODE, false, 0, 0, 1, 0, List.of(options));
    }

    void attach(Module module) {
        this.owner = module;
        String key = configKey();
        switch (type) {
            case TOGGLE -> boolValue = BeanConfig.getBool(key, boolValue);
            case SLIDER -> value = clamp(BeanConfig.getNumber(key, value));
            case MODE -> {
                String saved = BeanConfig.getString(key, null);
                int found = saved == null ? -1 : options.indexOf(saved);
                index = found < 0 ? 0 : found;
            }
        }
    }

    private String configKey() {
        return "setting." + (owner == null ? "?" : owner.id()) + "." + id();
    }

    public String id() {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public String name() {
        return name;
    }

    public Type type() {
        return type;
    }

    public boolean boolValue() {
        return boolValue;
    }

    public double value() {
        return value;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public List<String> options() {
        return options;
    }

    public String modeValue() {
        return options.isEmpty() ? "" : options.get(Math.floorMod(index, options.size()));
    }

    /** Slider position as 0-1, for the renderer. */
    public double fraction() {
        return max - min <= 0 ? 0 : (value - min) / (max - min);
    }

    public String displayValue() {
        return switch (type) {
            case TOGGLE -> boolValue ? "on" : "off";
            case SLIDER -> decimals <= 0
                    ? String.valueOf((int) Math.round(value))
                    : String.format(Locale.ROOT, "%." + decimals + "f", value);
            case MODE -> modeValue();
        };
    }

    public void toggle() {
        boolValue = !boolValue;
        BeanConfig.setBool(configKey(), boolValue);
        persist();
    }

    /** Sets the slider from a 0-1 drag position, snapping integer sliders. */
    public void setFraction(double fraction) {
        double raw = min + (max - min) * Math.max(0, Math.min(1, fraction));
        value = clamp(decimals <= 0 ? Math.round(raw) : raw);
        BeanConfig.setNumber(configKey(), value);
        persist();
    }

    public void cycle(int delta) {
        if (options.isEmpty()) {
            return;
        }
        index = Math.floorMod(index + delta, options.size());
        BeanConfig.setString(configKey(), modeValue());
        persist();
    }

    private void persist() {
        BeanConfig.saveSoon();
        if (owner != null) {
            owner.onSettingChanged(this);
            if (owner.id().equals("fps_limiter")) {
                DefaultModules.onFrameLimitChanged();
            }
        }
    }

    private double clamp(double v) {
        return Math.max(min, Math.min(max, v));
    }
}
