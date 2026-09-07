package club.bean.client;

import club.bean.client.feature.AimAssist;
import club.bean.client.feature.AutoArmor;
import club.bean.client.feature.AutoEat;
import club.bean.client.feature.AutoTool;
import club.bean.client.feature.AutoTotem;
import club.bean.client.feature.ChestStealer;
import club.bean.client.feature.Brightness;
import club.bean.client.feature.ChatFilter;
import club.bean.client.feature.Combat;
import club.bean.client.feature.CrystalAura;
import club.bean.client.feature.FrameLimit;
import club.bean.client.feature.Movement;
import club.bean.client.feature.MovementExtras;
import club.bean.client.feature.Nuker;
import club.bean.client.feature.Scaffold;
import club.bean.client.feature.Trackers;
import club.bean.client.feature.Zoom;
import club.bean.client.gui.BeanGui;
import club.bean.client.gui.Draw;
import club.bean.client.gui.BeanGuiScreen;
import club.bean.client.hud.BeanHudOverlay;
import club.bean.client.module.DefaultModules;
import club.bean.client.module.ModuleRegistry;
import club.bean.client.module.Settings;
import club.bean.client.theme.ThemeManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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
 * already holds, it may draw to your own screen, and it may move a vanilla
 * option you could have moved yourself. It never writes to the network, never
 * acts on your behalf, and never derives information the client was not already
 * given. There are no mixins - nothing here reaches into the render or network
 * path.
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
        ClientLifecycleEvents.CLIENT_STOPPING.register(BeanClient::onStopping);

        // Chat filtering is a display filter on messages already delivered.
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || ChatFilter.allow(message));
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, type, received) ->
                ChatFilter.allow(message));

        LOGGER.info("Bean Client ready - {} modules, {} theme(s).",
                ModuleRegistry.count(), ThemeManager.pickerIds().size());
    }

    /** Cleared until the first tick, when {@code mc.options} is definitely usable. */
    private static boolean bootstrapped;

    /**
     * Runs once, on the first client tick.
     *
     * <p>Two things have to happen here rather than in
     * {@link #onInitializeClient()}. A vanilla option left borrowed by a session
     * that never got to shut down cleanly has to be handed back before anything
     * takes it again; and a module whose toggle was restored from the config
     * never fired its own listener, so anything that acts on toggle rather than
     * on tick - the frame cap - has to be applied by hand.
     */
    private static void bootstrap() {
        Zoom.init();
        Brightness.init();
        FrameLimit.init();
        Draw.setFast(Settings.enabled("fast_gui"));
    }

    /**
     * Hands everything back on the way out.
     *
     * <p>This matters because vanilla writes {@code options.txt} during shutdown,
     * so an option still overridden at this point would be saved as the player's
     * own setting. Restoring here covers the clean exit; the value parked in our
     * own config by {@code VanillaOption} covers the rest.
     */
    private static void onStopping(Minecraft mc) {
        Zoom.reset();
        Brightness.reset();
        FrameLimit.reset();
        Combat.reset(mc);
        Movement.reset(mc);
        MovementExtras.reset(mc);
        AutoEat.stop(mc);
        Trackers.flushPlaytime();
        BeanConfig.save();
    }

    private static void onTick(Minecraft mc) {
        if (!bootstrapped) {
            bootstrapped = true;
            bootstrap();
        }

        // consumeClick() only fires while no screen has focus, which is what we
        // want - the open GUI handles its own close key so rebinding works in
        // both directions.
        while (BeanKeys.openMenu.consumeClick()) {
            if (!BeanGui.isOpen()) {
                BeanGui.setOpen(true);
                mc.setScreenAndShow(new BeanGuiScreen());
            }
        }

        if (mc.level == null) {
            // Left the world - hand everything borrowed back. The attribute
            // holds die with the player anyway, but clearing them keeps the
            // bookkeeping honest for the next world.
            Zoom.reset();
            Brightness.reset();
            Combat.reset(mc);
            Movement.reset(mc);
        } else {
            Zoom.setHeld(BeanKeys.zoom.isDown());
            Zoom.tick();
            Brightness.tick();
            Combat.tick(mc);
            AimAssist.tick(mc);
            CrystalAura.tick(mc);
            Movement.tick(mc);
            MovementExtras.tick(mc, mc.player);
            AutoTotem.tick(mc);
            AutoTool.tick(mc);
            AutoEat.tick(mc);
            AutoArmor.tick(mc);
            ChestStealer.tick(mc);
            Scaffold.tick(mc);
            Nuker.tick(mc);
        }
        Trackers.tick(mc);
        BeanConfig.flush();
    }
}
