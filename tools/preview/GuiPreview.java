import club.bean.client.gui.Draw;
import club.bean.client.gui.Icons;
import club.bean.client.theme.Colours;
import club.bean.client.theme.Theme;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the client's GUI shapes to a PNG, outside Minecraft.
 *
 * <p>The rasteriser in {@code Draw} is pure arithmetic whose only output is a
 * stream of axis-aligned rectangles. Pointing that stream at a bitmap produces
 * exactly the pixels the game would draw, which makes it possible to look at
 * the GUI - and count its draw calls - without launching anything. Render
 * changes had otherwise been shipped unseen.
 *
 * <p>Everything here goes through the real widgets and the real theme file.
 * Text is the one omission, because fonts live in Minecraft rather than in
 * {@code Draw}.
 *
 * <p>Run it with {@code tools/preview/run.sh}.
 */
public final class GuiPreview {

    /** Collects rectangles into a bitmap, and counts them. */
    static final class Bitmap implements Draw.Sink {
        final BufferedImage image;
        int fills;

        Bitmap(int width, int height, int background) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics g = image.getGraphics();
            g.setColor(new java.awt.Color(background));
            g.fillRect(0, 0, width, height);
            g.dispose();
        }

        @Override
        public void fill(int x0, int y0, int x1, int y1, int argb) {
            fills++;
            int alpha = (argb >>> 24) & 0xFF;
            if (alpha == 0) {
                return;
            }
            int left = Math.max(0, x0);
            int right = Math.min(image.getWidth(), x1);
            int top = Math.max(0, y0);
            int bottom = Math.min(image.getHeight(), y1);
            for (int y = top; y < bottom; y++) {
                for (int x = left; x < right; x++) {
                    image.setRGB(x, y, blend(image.getRGB(x, y), argb, alpha));
                }
            }
        }

        private static int blend(int under, int over, int alpha) {
            float a = alpha / 255f;
            int r = Math.round(((over >> 16) & 0xFF) * a + ((under >> 16) & 0xFF) * (1 - a));
            int g = Math.round(((over >> 8) & 0xFF) * a + ((under >> 8) & 0xFF) * (1 - a));
            int b = Math.round((over & 0xFF) * a + (under & 0xFF) * (1 - a));
            return (r << 16) | (g << 8) | b;
        }
    }

    private static final int WIDTH = 380;
    private static final int HEIGHT = 250;

    public static void main(String[] args) throws Exception {
        Path themeFile = Path.of(args.length > 0 ? args[0] : "themes/bean.json");
        File out = new File(args.length > 1 ? args[1] : "build/preview");
        out.mkdirs();

        JsonObject json = JsonParser
                .parseString(Files.readString(themeFile, StandardCharsets.UTF_8))
                .getAsJsonObject();
        Theme theme = Theme.fromJson("bean", json);
        System.out.println("theme: " + theme.name + "  accent " + Colours.toHex(theme.accent));

        List<String> report = new ArrayList<>();
        for (int scale : new int[] { 1, 2, 3 }) {
            report.add(render(out, theme, scale, false));
        }
        report.add(render(out, theme, 3, true));

        System.out.println();
        System.out.printf("%-22s %8s %10s %12s%n", "preview", "fills", "px", "px per fill");
        System.out.println("-".repeat(56));
        report.forEach(System.out::println);
    }

    private static String render(File dir, Theme theme, int scale, boolean fast) throws Exception {
        // Size the bitmap by what will actually be rasterised, not by what was
        // asked for: the renderer caps its multiplier, and a larger canvas would
        // just leave dead space that looks like a layout bug.
        int effective = fast ? 1 : Draw.captureScale(scale);
        Bitmap bitmap = new Bitmap(WIDTH * effective, HEIGHT * effective,
                theme.background & 0xFFFFFF);

        Draw.setFast(fast);
        Draw.beginCapture(bitmap, scale, HEIGHT);
        drawWindow(theme);
        Draw.endCapture();
        Draw.setFast(false);

        String name = fast ? "gui-scale3-fast" : "gui-scale" + scale;
        ImageIO.write(bitmap.image, "png", new File(dir, name + ".png"));

        long pixels = (long) WIDTH * effective * HEIGHT * effective;
        return String.format("%-22s %8d %10d %12.1f", name, bitmap.fills, pixels,
                bitmap.fills == 0 ? 0 : (double) pixels / bitmap.fills);
    }

    /** A representative window: panel, rail, rows, toggles, icons, a slider. */
    private static void drawWindow(Theme theme) {
        int radius = theme.cornerRadius;
        int dim = Colours.darken(theme.panelAlt, 0.25f);

        Draw.roundRect(null, 6, 6, WIDTH - 12, HEIGHT - 12, radius, theme.panel);
        Draw.backgroundPattern(null, 6, 6, WIDTH - 12, HEIGHT - 12, theme);
        Draw.roundRect(null, 12, 32, 104, HEIGHT - 44, radius - 2, theme.panelAlt);

        // Title bar.
        Draw.bean(null, 26, 19, 20, 13, theme.accent, theme.panel);
        Icons.cross(null, WIDTH - 20, 19, 9, theme.text);

        // Category rail: one pill per category, first selected. Driven off the
        // enum so adding a tab shows up here without editing the preview.
        var categories = club.bean.client.module.Category.values();
        for (int i = 0; i < categories.length; i++) {
            int y = 38 + i * 24;
            boolean selected = i == 0;
            if (selected) {
                Draw.roundRect(null, 18, y, 92, 20, 6, theme.accent);
            }
            int tint = selected ? Colours.contrastOn(theme.accent) : theme.textDim;
            Icons.category(null, categories[i], 26, y + 5, 11, tint);
        }

        // Module rows.
        for (int i = 0; i < 5; i++) {
            int y = 38 + i * 32;
            boolean on = i % 2 == 0;
            Draw.roundRect(null, 124, y, WIDTH - 138, 26, 5, theme.panelAlt);
            if (on) {
                Draw.roundRect(null, 124, y, 3, 26, 1.5, theme.accent);
            }
            Draw.toggleSwitch(null, WIDTH - 48, y + 6, 26, 14, on ? 1f : 0f, theme);
            Icons.settings(null, WIDTH - 74, y + 7, 12, theme.textDim);
        }

        // Settings drawer: a slider and a checkbox-sized circle.
        int drawerY = 38 + 5 * 32 + 8;
        Draw.slider(null, 132, drawerY, WIDTH - 190, 8, 0.62f, theme);
        Draw.circle(null, WIDTH - 40, drawerY + 4, 5, theme.accent);
        // Unused locals kept honest.
        if (dim == 0) {
            throw new IllegalStateException();
        }
    }
}
