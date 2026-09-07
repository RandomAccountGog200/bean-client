package club.bean.client;

import club.bean.client.feature.ChatFilter;
import club.bean.client.feature.Trackers;
import club.bean.client.feature.Zoom;
import club.bean.client.gui.BeanGui;
import club.bean.client.gui.BeanGuiScreen;
import club.bean.client.hud.BeanHudOverlay;
import club.bean.client.module.DefaultModules;
import club.bean.client.module.ModuleRegistry;
import club.bean.client.theme.ThemeManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean Client entry point.
 *
 * <p>Design rule for the whole client: it may read state the vanilla client
 * already holds, and it may draw to your own screen. It never writes to the
 * network, never acts on your behalf, and never derives information the client
 * was not already given.
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

        DefaultModules.registerAll();
        BeanKeys.register();

        HudElementRegistry.addLast(id("hud"), new BeanHudOverlay());
        ClientTickEvents.END_CLIENT_TICK.register(BeanClient::onTick);

        // Chat filtering is a display filter on messages already delivered.
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || ChatFilter.allow(message));
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, type, received) ->
                ChatFilter.allow(message));

        LOGGER.info("Bean Client ready - {} modules, {} theme(s).",
                ModuleRegistry.count(), ThemeManager.pickerIds().size());
    }

    private static void onTick(Minecraft mc) {
        // consumeClick() only fires while no screen has focus, which is what we
        // want - the open GUI handles its own close key so rebinding works in
        // both directions.
        while (BeanKeys.openMenu.consumeClick()) {
            if (!BeanGui.isOpen()) {
                BeanGui.setOpen(true);
                mc.setScreenAndShow(new BeanGuiScreen());
            }
        }

        Zoom.setHeld(BeanKeys.zoom.isDown());
        Trackers.tick(mc);
        BeanConfig.flush();
    }
}
