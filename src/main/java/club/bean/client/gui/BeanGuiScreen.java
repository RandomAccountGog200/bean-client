package club.bean.client.gui;

import club.bean.client.BeanConfig;
import club.bean.client.BeanKeys;
import club.bean.client.module.Category;
import club.bean.client.module.Module;
import club.bean.client.module.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The interactive host for the window.
 *
 * <p>{@link #isPauseScreen()} returns false, so opening the GUI does not stop
 * the world - the game keeps ticking underneath exactly as it does with the
 * chat box open. All this class does is translate input into calls on
 * {@link BeanGui} and {@link club.bean.client.module.ModuleRegistry}; every
 * pixel is drawn by {@link BeanGuiRenderer}, which the HUD overlay also uses.
 */
public class BeanGuiScreen extends Screen {
    private boolean draggingWindow;
    private boolean resizingWindow;
    private double grabX;
    private double grabY;

    /** The slider being dragged, if any, so the drag keeps working off the track. */
    private Setting activeSlider;

    public BeanGuiScreen() {
        super(Component.literal("Bean Client"));
    }

    @Override
    protected void init() {
        BeanGui.fitTo(this.width, this.height);
        BeanGui.clampScroll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        BeanGuiRenderer.render(gfx, this.font, (int) BeanGuiRenderer.localX(mouseX),
                (int) BeanGuiRenderer.localY(mouseY), true);
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    // ---- mouse -------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = BeanGuiRenderer.localX(event.x());
        double my = BeanGuiRenderer.localY(event.y());
        int button = event.button();

        if (BeanGuiRenderer.inCloseButton(mx, my)) {
            close();
            return true;
        }

        if (BeanGui.inGrip(mx, my)) {
            resizingWindow = true;
            grabX = mx - BeanGui.width();
            grabY = my - BeanGui.height();
            return true;
        }

        if (BeanGui.inTitleBar(mx, my)) {
            draggingWindow = true;
            grabX = mx - BeanGui.x();
            grabY = my - BeanGui.y();
            return true;
        }

        Category rail = BeanGuiRenderer.railHit(mx, my);
        if (rail != null) {
            BeanGui.select(rail);
            ThemeTab.closeDropdown();
            BeanGui.setSearchFocused(false);
            return true;
        }

        if (BeanGui.selected() == Category.THEMES) {
            if (ThemeTab.mouseClicked(mx, my, button)) {
                return true;
            }
        } else if (handleModuleClick(mx, my, button)) {
            return true;
        }

        // Anything else inside the window is swallowed so clicks never reach
        // the world behind it.
        if (BeanGui.inWindow(mx, my)) {
            BeanGui.setSearchFocused(false);
            return true;
        }

        BeanGui.setSearchFocused(false);
        ThemeTab.closeDropdown();
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleModuleClick(double mx, double my, int button) {
        if (BeanGuiRenderer.inSearchClear(mx, my) && !BeanGui.query().isEmpty()) {
            BeanGui.setQuery("");
            BeanGui.setSearchFocused(true);
            return true;
        }
        if (BeanGuiRenderer.inSearchBar(mx, my)) {
            BeanGui.setSearchFocused(true);
            return true;
        }

        if (my < BeanGui.listTop() || my >= BeanGui.listBottom()) {
            return false;
        }

        int lx = BeanGui.listX();
        int lw = BeanGui.listW();
        for (BeanGuiRenderer.Row row : BeanGuiRenderer.layoutRows()) {
            Module module = row.module();

            if (BeanGui.hit(mx, my, lx, row.y(), lw, BeanGui.ROW_H)) {
                if (BeanGuiRenderer.inGear(row, mx, my) || button == 1) {
                    if (module.hasSettings()) {
                        module.setExpanded(!module.isExpanded());
                    }
                } else if (button == 0) {
                    module.toggle();
                }
                BeanGui.setSearchFocused(false);
                return true;
            }

            if (module.isExpanded() && module.drawerProgress() > 0.5f
                    && handleSettingClick(row, mx, my, button)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleSettingClick(BeanGuiRenderer.Row row, double mx, double my, int button) {
        List<Setting> settings = row.module().settings();
        int sx = BeanGuiRenderer.settingX();
        int sw = BeanGuiRenderer.settingW();

        for (int i = 0; i < settings.size(); i++) {
            int y = BeanGuiRenderer.settingY(row, i);
            if (!BeanGui.hit(mx, my, sx, y, sw, BeanGui.SETTING_H)) {
                continue;
            }
            Setting setting = settings.get(i);
            switch (setting.type()) {
                case TOGGLE -> setting.toggle();
                case MODE -> setting.cycle(button == 1 ? -1 : 1);
                case SLIDER -> {
                    activeSlider = setting;
                    dragSlider(mx);
                }
            }
            BeanGui.setSearchFocused(false);
            return true;
        }
        return false;
    }

    private void dragSlider(double mx) {
        if (activeSlider == null) {
            return;
        }
        int[] bounds = BeanGuiRenderer.sliderBounds();
        activeSlider.setFraction((mx - bounds[0]) / Math.max(1, bounds[1]));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mx = BeanGuiRenderer.localX(event.x());
        double my = BeanGuiRenderer.localY(event.y());

        if (draggingWindow) {
            BeanGui.moveTo((int) Math.round(mx - grabX), (int) Math.round(my - grabY));
            BeanGui.clampPosition();
            return true;
        }
        if (resizingWindow) {
            BeanGui.resizeTo((int) Math.round(mx - grabX), (int) Math.round(my - grabY));
            BeanGui.clampPosition();
            return true;
        }
        if (activeSlider != null) {
            dragSlider(mx);
            return true;
        }
        if (BeanGui.selected() == Category.THEMES && ThemeTab.mouseDragged(mx, my)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingWindow || resizingWindow) {
            BeanGui.commitGeometry();
        }
        draggingWindow = false;
        resizingWindow = false;
        activeSlider = null;
        ThemeTab.mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double my = BeanGuiRenderer.localY(mouseY);
        if (BeanGui.selected() != Category.THEMES
                && my >= BeanGui.listTop() && my < BeanGui.listBottom()) {
            BeanGui.scrollBy(-scrollY * 22);
            return true;
        }
        return BeanGui.inWindow(BeanGuiRenderer.localX(mouseX), my);
    }

    // ---- keyboard ----------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Honour whatever the toggle is actually bound to, not a hard-coded key.
        if (BeanKeys.openMenu != null && BeanKeys.openMenu.matches(event)) {
            close();
            return true;
        }

        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (ThemeTab.isDropdownOpen()) {
                ThemeTab.closeDropdown();
            } else if (BeanGui.isSearchFocused() && !BeanGui.query().isEmpty()) {
                BeanGui.setQuery("");
            } else if (BeanGui.isSearchFocused()) {
                BeanGui.setSearchFocused(false);
            } else {
                close();
            }
            return true;
        }

        if (BeanGui.isSearchFocused()) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String query = BeanGui.query();
                if (!query.isEmpty()) {
                    BeanGui.setQuery(query.substring(0, query.length() - 1));
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                BeanGui.setSearchFocused(false);
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (BeanGui.isSearchFocused() && event.isAllowedChatCharacter()) {
            BeanGui.setQuery(BeanGui.query() + event.codepointAsString());
            return true;
        }
        return super.charTyped(event);
    }

    // ---- lifecycle ---------------------------------------------------------

    /**
     * Starts the fade-out and drops the screen immediately. The last frames of
     * the animation are drawn by {@code BeanHudOverlay}, so the window still
     * shrinks away after the mouse is recaptured.
     */
    private void close() {
        BeanGui.setOpen(false);
        ThemeTab.reset();
        this.minecraft.setScreenAndShow(null);
    }

    @Override
    public void onClose() {
        BeanGui.setOpen(false);
        ThemeTab.reset();
        BeanConfig.save();
        super.onClose();
    }
}
