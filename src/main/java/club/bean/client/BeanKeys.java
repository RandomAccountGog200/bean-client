package club.bean.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * The one key the client binds: Right Shift opens the GUI.
 *
 * <p>It is a normal Minecraft keybind, so it shows up under Controls and can be
 * rebound. The GUI checks {@code openMenu.matches(event)} rather than the raw
 * key code, so rebinding also changes the key that closes it.
 */
public final class BeanKeys {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(BeanClient.id("main"));

    public static KeyMapping openMenu;

    private BeanKeys() {
    }

    public static void register() {
        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.beanclient.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
    }
}
