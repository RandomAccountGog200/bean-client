package club.bean.client.feature;

import club.bean.client.BeanClient;
import club.bean.client.BeanConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

import java.util.function.DoubleFunction;
import java.util.function.Supplier;

/**
 * Borrows a vanilla video option, and gives it back.
 *
 * <p>Zoom, Brightness and the FPS Limiter are all "move a slider you could have
 * moved yourself", so that is literally how they are implemented - no mixin, no
 * injection into the render path.
 *
 * <p>Two things make that harder than it sounds.
 *
 * <p>The first is that {@link OptionInstance#set} validates: hand it a value
 * outside the option's own range and it silently substitutes the default
 * instead. So every write is read straight back, and if it did not take, the
 * override is abandoned rather than leaving the game on some value nobody asked
 * for.
 *
 * <p>The second is that the option we are holding is the same field vanilla
 * writes to {@code options.txt} when the game shuts down. Restoring on a clean
 * exit is just a matter of calling {@link #restore()} early enough - but a crash
 * or a killed process would bake our override into the player's own settings
 * permanently. So the moment an override is taken, the value we displaced is
 * written to our config under {@code configKey}, and {@link #recover()} at
 * startup puts it back if it is still there. The key is cleared on restore, so
 * in the normal case it never survives a single session.
 *
 * @param <T> the option's value type - always numeric, so it can be persisted
 */
public final class VanillaOption<T extends Number> {
    private final String name;
    private final String configKey;
    private final Supplier<OptionInstance<T>> option;
    private final DoubleFunction<T> decode;

    private T saved;
    private boolean overriding;
    private boolean unavailable;

    /**
     * @param name      for log messages only
     * @param configKey where the displaced value is parked while we hold the option
     * @param option    looked up lazily, because {@code Minecraft.getInstance()}
     *                  is not usable at class-init time
     * @param decode    turns the parked number back into the option's own type
     */
    public VanillaOption(String name, String configKey, Supplier<OptionInstance<T>> option,
                         DoubleFunction<T> decode) {
        this.name = name;
        this.configKey = configKey;
        this.option = option;
        this.decode = decode;
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
            park(saved);
        }
        if (value.equals(instance.get())) {
            return true;
        }
        instance.set(value);

        if (!value.equals(instance.get())) {
            // Rejected - the option validated it away. Undo and stop trying.
            instance.set(saved);
            overriding = false;
            unpark();
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
        unpark();
    }

    /**
     * Puts back a value left parked by a session that did not exit cleanly.
     * Called once at startup, before anything has a chance to override.
     */
    public void recover() {
        if (!BeanConfig.has(configKey)) {
            return;
        }
        double parked = BeanConfig.getNumber(configKey, Double.NaN);
        unpark();
        if (Double.isNaN(parked)) {
            return;
        }
        OptionInstance<T> instance = instance();
        if (instance == null) {
            return;
        }
        T value = decode.apply(parked);
        instance.set(value);
        BeanClient.LOGGER.info("Handing {} back to {} - last session did not exit cleanly", name, value);
    }

    public boolean isOverriding() {
        return overriding;
    }

    private void park(T value) {
        if (value == null) {
            return;
        }
        BeanConfig.setNumber(configKey, value.doubleValue());
        BeanConfig.saveSoon();
    }

    private void unpark() {
        BeanConfig.remove(configKey);
        BeanConfig.saveSoon();
    }
}
