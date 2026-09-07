package club.bean.client.feature;

import club.bean.client.BeanClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Drives a vanilla video option and puts it back afterwards.
 *
 * <p>Zoom and Brightness are both "move a slider you could have moved yourself",
 * so that is literally how they are implemented — no mixin, no injection into
 * the render path. The catch is that {@link OptionInstance#set} validates: hand
 * it a value outside the option's own range and it silently substitutes the
 * default instead. So every write is read straight back, and if it did not take
 * the override is abandoned rather than leaving the game on some value nobody
 * asked for.
 *
 * @param <T> the option's value type
 */
public final class VanillaOption<T> {
    private final String name;
    private final java.util.function.Supplier<OptionInstance<T>> option;

    private T saved;
    private boolean overriding;
    private boolean unavailable;

    public VanillaOption(String name, java.util.function.Supplier<OptionInstance<T>> option) {
        this.name = name;
        this.option = option;
    }

    private OptionInstance<T> instance() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.options == null ? null : option.get();
    }

    /** The player's own setting, ignoring any override we have applied. */
    public T baseValue() {
        OptionInstance<T> instance = instance();
        if (instance == null) {
            return null;
        }
        return overriding ? saved : instance.get();
    }

    /**
     * Applies {@code value}, remembering the player's setting the first time.
     *
     * @return false when the option refused the value, in which case nothing changed
     */
    public boolean override(T value) {
        OptionInstance<T> instance = instance();
        if (instance == null || unavailable || value == null) {
            return false;
        }
        if (!overriding) {
            saved = instance.get();
            overriding = true;
        }
        if (value.equals(instance.get())) {
            return true;
        }
        instance.set(value);

        if (!value.equals(instance.get())) {
            // Rejected - the option validated it away. Undo and stop trying.
            instance.set(saved);
            overriding = false;
            unavailable = true;
            BeanClient.LOGGER.warn("{} refused the value {} - leaving it alone", name, value);
            return false;
        }
        return true;
    }

    /** Restores the player's own setting. Safe to call when not overriding. */
    public void restore() {
        if (!overriding) {
            return;
        }
        OptionInstance<T> instance = instance();
        if (instance != null && saved != null) {
            instance.set(saved);
        }
        overriding = false;
    }

    public boolean isOverriding() {
        return overriding;
    }
}
