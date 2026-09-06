package club.bean.client.hud;

import club.bean.client.gui.BeanGui;
import club.bean.client.gui.BeanGuiRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The second host for the window, on Fabric's HUD render hook.
 *
 * <p>Closing the GUI drops the screen straight away so the mouse goes back to
 * the game immediately - but the window still owes the user a fade-out. Fabric
 * only extracts HUD elements when no screen is open, which is exactly the
 * window this needs: it picks the animation up on the frame after the screen
 * disappears and draws the remaining frames over the running game.
 *
 * <p>It calls the same {@link BeanGuiRenderer#render} the screen does, with
 * interaction turned off, so the closing window is pixel-identical to the open
 * one and cannot be hovered or clicked.
 */
public final class BeanHudOverlay implements HudElement {
    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        // Nothing to do while the GUI is open (the screen is drawing it) or once
        // the fade has finished.
        if (BeanGui.isOpen() || !BeanGui.isVisible()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        // -1 keeps every hover highlight off; the cursor is the game's again.
        BeanGuiRenderer.render(gfx, mc.font, -1, -1, false);
    }
}
