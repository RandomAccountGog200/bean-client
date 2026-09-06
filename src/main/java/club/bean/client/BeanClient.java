package club.bean.client;

import club.bean.client.gui.BeanGui;
import club.bean.client.gui.BeanGuiScreen;
import club.bean.client.hud.BeanHudOverlay;
import club.bean.client.module.DefaultModules;
import club.bean.client.module.ModuleRegistry;
import club.bean.client.theme.ThemeManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean Client - a click-GUI shell with nothing behind it.
 *
 * <p>This is the whole wiring: load the config, load the themes, register the
 * placeholder rows, bind Right Shift, and hang the fade-out overlay off the HUD
 * hook. Real features slot in through
 * {@link ModuleRegistry#registerModule} without any of the code in
 * {@code club.bean.client.gui} needing to know they exist.
 */
public class BeanClient implements ClientModInitializer {
    public static final String MOD_ID = "beanclient";
    public static final Logger LOGGER = LoggerFactory.getLogger("BeanClient");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        BeanConfig.load();
        ThemeManager.init();
        BeanGui.load();

        // Swap this line for your own registrations - see DefaultModules for
        // the shape, and README.md for a worked example.
        DefaultModules.registerAll();

        BeanKeys.register();

        HudElementRegistry.addLast(id("gui_overlay"), new BeanHudOverlay());
        ClientTickEvents.END_CLIENT_TICK.register(BeanClient::onTick);

        LOGGER.info("Bean Client ready - {} placeholder modules, {} theme(s), no logic behind any of it.",
                ModuleRegistry.count(), ThemeManager.pickerIds().size());
    }

    private static void onTick(Minecraft mc) {
        // consumeClick() only fires while no screen has focus, which is what we
        // want here - the open GUI handles its own close key so that rebinding
        // works in both directions.
        while (BeanKeys.openMenu.consumeClick()) {
            if (!BeanGui.isOpen()) {
                BeanGui.setOpen(true);
                mc.setScreenAndShow(new BeanGuiScreen());
            }
        }
        BeanConfig.flush();
    }
}
