package club.bean.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * The keys the client binds: Right Shift opens the GUI, C holds zoom.
 *
 * <p>It is a normal Minecraft keybind, so it shows up under Controls and can be
 * rebound. The GUI checks {@code openMenu.matches(event)} rather than the raw
 * key code, so rebinding also changes the key that closes it.
 */
public final class BeanKeys {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(BeanClient.id("main"));

    public static KeyMapping openMenu;
    public static KeyMapping zoom;
    public static KeyMapping pearl;

    private BeanKeys() {
    }

    public static void register() {
        openMenu = bind("key.beanclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        zoom = bind("key.beanclient.zoom", GLFW.GLFW_KEY_C);
        pearl = bind("key.beanclient.pearl", GLFW.GLFW_KEY_R);
    }

    private static KeyMapping bind(String translation, int key) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translation, InputConstants.Type.KEYSYM, key, CATEGORY));
    }
}
