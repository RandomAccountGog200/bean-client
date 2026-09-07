package club.bean.client.module;

import club.bean.client.BeanConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One row in the module list.
 *
 * <p>A module in this shell is a name, a category, a boolean and a callback.
 * It deliberately knows nothing about rendering, and the renderer knows nothing
 * about what a module does - which is the whole point: dropping real behaviour
 * in later means writing a listener, not touching draw code.
 *
 * <pre>{@code
 * ModuleRegistry.registerModule("Zoom", Category.VISUAL, on -> {
 *     if (on) SprintFeature.start(); else SprintFeature.stop();
 * });
 * }</pre>
 */
public final class Module {
    /** Notified on every toggle, with the new state. */
    @FunctionalInterface
    public interface ToggleListener {
        void onToggle(boolean enabled);
    }

    /** Notified whenever one of this module's settings moves. */
    @FunctionalInterface
    public interface SettingListener {
        void onSettingChanged(Setting setting);
    }

    private final String name;
    private final String id;
    private final Category category;
    private final ToggleListener onToggle;
    private final List<Setting> settings = new ArrayList<>();

    private SettingListener onSettingChange;

    private String description = "";
    private boolean enabled;

    /** Per-row drawer state, owned by the module so it survives category switches. */
    private boolean expanded;
    /** Eased 0-1 drawer height, driven by the renderer each frame. */
    private float drawerProgress;
    /** Eased 0-1 hover state, so rows light up rather than snapping. */
    private float hoverProgress;

    Module(String name, Category category, ToggleListener onToggle) {
        this.name = name;
        this.id = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        this.category = category;
        this.onToggle = onToggle;
    }

    public String name() {
        return name;
    }

    public String id() {
        return id;
    }

    public Category category() {
        return category;
    }

    public String description() {
        return description;
    }

    public Module description(String text) {
        this.description = text == null ? "" : text;
        return this;
    }

    /** Adds a settings row and returns {@code this}, so registrations chain. */
    public Module setting(Setting setting) {
        settings.add(setting);
        setting.attach(this);
        return this;
    }

    /**
     * Registers a listener for setting changes and returns {@code this}, so it
     * chains after the settings it watches:
     *
     * <pre>{@code
     * registerModule("FPS Limiter", Category.MISC, on -> FrameLimit.apply(), ...)
     *         .setting(Setting.slider("Limit", 60, 10, 260, 0))
     *         .onSettingChange(setting -> FrameLimit.apply());
     * }</pre>
     *
     * <p>Without this a slider would only take effect on the next toggle, which
     * is why {@link Setting} routes every change back through the owning module
     * rather than knowing about any particular one.
     */
    public Module onSettingChange(SettingListener listener) {
        this.onSettingChange = listener;
        return this;
    }

    public List<Setting> settings() {
        return settings;
    }

    public boolean hasSettings() {
        return !settings.isEmpty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public float drawerProgress() {
        return drawerProgress;
    }

    public void setDrawerProgress(float progress) {
        this.drawerProgress = progress;
    }

    public float hoverProgress() {
        return hoverProgress;
    }

    public void setHoverProgress(float progress) {
        this.hoverProgress = progress;
    }

    /** Restores the saved state without firing the callback. Called once at registration. */
    void restore() {
        enabled = BeanConfig.getBool("module." + id, false);
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }
        enabled = value;
        BeanConfig.setBool("module." + id, enabled);
        BeanConfig.saveSoon();
        if (onToggle != null) {
            onToggle.onToggle(enabled);
        }
        ModuleRegistry.playToggleSound(enabled);
    }

    void onSettingChanged(Setting setting) {
        ModuleRegistry.logSettingChange(this, setting);
        if (onSettingChange != null) {
            onSettingChange.onSettingChanged(setting);
        }
    }
}
