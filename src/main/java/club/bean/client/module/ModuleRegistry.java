package club.bean.client.module;

import club.bean.client.BeanClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The single seam between this GUI shell and any real functionality.
 *
 * <p>Nothing in {@code club.bean.client.gui} imports a concrete module. The
 * renderer walks whatever is in here, so wiring up a real feature never means
 * editing draw code:
 *
 * <pre>{@code
 * ModuleRegistry.registerModule("Fullbright", Category.VISUAL, on ->
 *         Minecraft.getInstance().options.gamma().set(on ? 15.0 : 0.5));
 * }</pre>
 */
public final class ModuleRegistry {
    private static final Map<String, Module> BY_ID = new LinkedHashMap<>();
    private static final Map<Category, List<Module>> BY_CATEGORY = new EnumMap<>(Category.class);

    private ModuleRegistry() {
    }

    /**
     * Registers a module and returns it, so settings can be chained on:
     *
     * <pre>{@code
     * registerModule("Zoom", Category.VISUAL, on -> {})
     *         .description("Hold to narrow your FOV")
     *         .setting(Setting.slider("Factor", 4, 1, 10, 1));
     * }</pre>
     *
     * @param name     shown in the list, and the source of the config key
     * @param category which rail tab the row lives under
     * @param onToggle fired with the new state on every flip; may be null
     */
    public static Module registerModule(String name, Category category, Module.ToggleListener onToggle) {
        Module module = new Module(name, category, onToggle);
        if (BY_ID.containsKey(module.id())) {
            BeanClient.LOGGER.warn("Module id '{}' registered twice - the second one wins", module.id());
        }
        module.restore();
        BY_ID.put(module.id(), module);
        BY_CATEGORY.computeIfAbsent(category, key -> new ArrayList<>()).add(module);
        return module;
    }

    /** Convenience overload for callers that would rather pass the category by name. */
    public static Module registerModule(String name, String category, Module.ToggleListener onToggle) {
        return registerModule(name, Category.byName(category), onToggle);
    }

    public static List<Module> byCategory(Category category) {
        return BY_CATEGORY.getOrDefault(category, List.of());
    }

    /**
     * Rows for a category, filtered by the search box. An empty query returns
     * the category as-is; a non-empty one searches every category so the box
     * works as a client-wide jump-to.
     */
    public static List<Module> search(Category category, String query) {
        if (query == null || query.isBlank()) {
            return byCategory(category);
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module module : BY_ID.values()) {
            if (module.name().toLowerCase(Locale.ROOT).contains(needle)) {
                out.add(module);
            }
        }
        return out;
    }

    public static Module get(String id) {
        return BY_ID.get(id);
    }

    public static java.util.Collection<Module> all() {
        return BY_ID.values();
    }

    public static int count() {
        return BY_ID.size();
    }

    static void logSettingChange(Module module, Setting setting) {
        BeanClient.LOGGER.debug("{} :: {} = {}", module.name(), setting.name(), setting.displayValue());
    }

    /**
     * Clicks when a module is toggled, if Toggle Sounds is on. Pitched up for
     * on and down for off, so the two are distinguishable without looking.
     */
    static void playToggleSound(boolean enabled) {
        Module sounds = BY_ID.get("toggle_sounds");
        if (sounds == null || !sounds.isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) {
            return;
        }
        mc.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, enabled ? 1.5f : 1.1f));
    }
}
