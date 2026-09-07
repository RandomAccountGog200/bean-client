package club.bean.client.gui;

import club.bean.client.module.Category;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The icon set, drawn as vectors rather than pixel masks.
 *
 * <p>Each one is composed from the anti-aliased primitives in {@link Draw} —
 * circles, rings, rotated bars and convex polygons — so they stay smooth at any
 * GUI scale and pick up the theme colour for free.
 *
 * <p>Every icon draws inside a {@code size x size} box whose top-left corner is
 * {@code (x, y)}. A category that supplies its own 9x9 mask falls back to
 * {@link Draw#glyph}, so adding a category by hand does not require drawing
 * vectors.
 */
public final class Icons {
    private Icons() {
    }

    public static void category(GuiGraphicsExtractor gfx, Category category, int x, int y, int size,
                                int colour) {
        String[] mask = category.fallbackIcon();
        if (mask != null) {
            Draw.glyph(gfx, mask, x, y, Math.max(1, size / 9), colour);
            return;
        }
        switch (category) {
            case HUD -> hudIcon(gfx, x, y, size, colour);
            case COMBAT -> combat(gfx, x, y, size, colour);
            case MOVEMENT -> movement(gfx, x, y, size, colour);
            case RENDER -> world(gfx, x, y, size, colour);
            case VISUAL -> visual(gfx, x, y, size, colour);
            case SMP -> smp(gfx, x, y, size, colour);
            case MISC -> misc(gfx, x, y, size, colour);
            case THEMES -> themes(gfx, x, y, size, colour);
        }
    }

    /** A screen with a readout on it. */
    public static void hudIcon(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double thick = Math.max(1.0, size * 0.10);
        Draw.roundBorder(gfx, x, y + size * 0.12, size, size * 0.72, size * 0.16, thick, colour);
        Draw.roundRect(gfx, x + size * 0.18, y + size * 0.34, size * 0.42, thick, thick / 2, colour);
        Draw.roundRect(gfx, x + size * 0.18, y + size * 0.52, size * 0.26, thick, thick / 2, colour);
    }

    /** Crossed swords. */
    public static void combat(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;
        double blade = size * 0.92;
        double thick = Math.max(1.4, size * 0.15);
        Draw.bar(gfx, cx, cy, blade, thick, 45, colour);
        Draw.bar(gfx, cx, cy, blade, thick, -45, colour);
        // Crossguards, so it reads as swords rather than an X. Each sits near
        // the lower end of a blade and runs across it, so its angle is the
        // blade's negated.
        double reach = blade * 0.30;
        Draw.bar(gfx, cx + reach, cy + reach, size * 0.40, thick * 0.85, -45, colour);
        Draw.bar(gfx, cx - reach, cy + reach, size * 0.40, thick * 0.85, 45, colour);
        // Pommels.
        Draw.circle(gfx, cx + blade * 0.46, cy + blade * 0.46, thick * 0.7, colour);
        Draw.circle(gfx, cx - blade * 0.46, cy + blade * 0.46, thick * 0.7, colour);
    }

    /** Two stacked chevrons pointing up. */
    public static void movement(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double cx = x + size / 2.0;
        double thick = Math.max(1.4, size * 0.15);
        double arm = size * 0.46;
        for (int i = 0; i < 2; i++) {
            double cy = y + size * (0.34 + i * 0.34);
            Draw.bar(gfx, cx - arm * 0.36, cy, arm, thick, -42, colour);
            Draw.bar(gfx, cx + arm * 0.36, cy, arm, thick, 42, colour);
        }
    }

    /** An eye: a lens outline with a pupil. */
    public static void visual(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;
        double halfW = size * 0.5;
        double halfH = size * 0.3;
        double thick = Math.max(1.0, size * 0.11);

        Draw.shape(gfx, cy - halfH - 1, cy + halfH + 1, yy -> {
            double[] outer = Shapes.lens(cx, cy, halfW, halfH).at(yy);
            double[] inner = Shapes.lens(cx, cy, halfW - thick, halfH - thick).at(yy);
            if (outer.length == 0) {
                return Shapes.EMPTY;
            }
            if (inner.length == 0) {
                return outer;
            }
            return new double[] { outer[0], inner[0], inner[1], outer[1] };
        }, colour);

        Draw.circle(gfx, cx, cy, size * 0.16, colour);
    }

    /** Head and shoulders. */
    public static void player(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double cx = x + size / 2.0;
        double headR = size * 0.21;
        Draw.circle(gfx, cx, y + size * 0.26, headR, colour);
        // Shoulders: the top half of a wide rounded rect, clipped by the box.
        double bodyW = size * 0.74;
        double bodyH = size * 0.44;
        double top = y + size * 0.56;
        Draw.shape(gfx, top, y + size, Shapes.roundRect(cx - bodyW / 2, top, bodyW, bodyH * 2,
                bodyW * 0.42), colour);
    }

    /** A globe: outer ring, meridian ellipse, equator. */
    public static void world(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;
        double r = size * 0.48;
        double thick = Math.max(1.0, size * 0.11);

        Draw.ring(gfx, cx, cy, r, r - thick, colour);
        Draw.shape(gfx, cy - r, cy + r,
                Shapes.ellipseRing(cx, cy, r * 0.46, r, r * 0.46 - thick * 0.85, r - thick * 0.85), colour);
        Draw.roundRect(gfx, cx - r, cy - thick / 2, r * 2, thick, thick / 2, colour);
    }

    /** Two figures side by side - other people on the server. */
    public static void smp(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double headR = size * 0.15;
        double bodyW = size * 0.42;
        for (int i = 0; i < 2; i++) {
            double cx = x + size * (0.28 + i * 0.44);
            double top = y + size * (i == 0 ? 0.30 : 0.38);
            Draw.circle(gfx, cx, top, headR, colour);
            Draw.shape(gfx, top + headR + size * 0.06, y + size,
                    Shapes.roundRect(cx - bodyW / 2, top + headR + size * 0.06, bodyW,
                            size * 0.7, bodyW * 0.42), colour);
        }
    }

    /** Three dots. */
    public static void misc(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double cy = y + size / 2.0;
        double r = size * 0.115;
        for (int i = 0; i < 3; i++) {
            Draw.circle(gfx, x + size * (0.2 + i * 0.3), cy, r, colour);
        }
    }

    /** A paint palette: a ring with a thumb hole and three wells. */
    public static void themes(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;
        double r = size * 0.48;
        double thick = Math.max(1.0, size * 0.13);

        Draw.ring(gfx, cx, cy, r, r - thick, colour);
        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(-150 + i * 60);
            Draw.circle(gfx, cx + Math.cos(angle) * r * 0.46, cy + Math.sin(angle) * r * 0.46,
                    size * 0.085, colour);
        }
        Draw.circle(gfx, cx + r * 0.42, cy + r * 0.44, size * 0.13, colour);
    }

    // ---- interface icons --------------------------------------------------

    /**
     * Settings: three sliders. Reads better than a gear at this size, and every
     * stroke is axis-aligned so it stays crisp.
     */
    public static void settings(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double thick = Math.max(1.0, size * 0.11);
        double[] knobAt = { 0.66, 0.34, 0.58 };
        for (int i = 0; i < 3; i++) {
            double cy = y + size * (0.2 + i * 0.3);
            Draw.roundRect(gfx, x, cy - thick / 2, size, thick, thick / 2, colour);
            Draw.circle(gfx, x + size * knobAt[i], cy, thick * 1.5, colour);
        }
    }

    /** Magnifying glass. */
    public static void search(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double r = size * 0.34;
        double cx = x + r + 1;
        double cy = y + r + 1;
        double thick = Math.max(1.0, size * 0.13);
        Draw.ring(gfx, cx, cy, r, r - thick, colour);
        Draw.bar(gfx, cx + r * 1.35, cy + r * 1.35, size * 0.42, thick, 45, colour);
    }

    /** Chevron pointing down; pass 180 for up. */
    public static void chevron(GuiGraphicsExtractor gfx, int x, int y, int size, double degrees,
                               int colour) {
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;
        double arm = size * 0.5;
        double thick = Math.max(1.2, size * 0.14);
        double flip = Math.cos(Math.toRadians(degrees));
        Draw.bar(gfx, cx - arm * 0.32, cy - thick * 0.1 * flip, arm, thick, 38 * flip, colour);
        Draw.bar(gfx, cx + arm * 0.32, cy - thick * 0.1 * flip, arm, thick, -38 * flip, colour);
    }

    /** Tick mark. */
    public static void check(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double thick = Math.max(1.2, size * 0.15);
        Draw.bar(gfx, x + size * 0.3, y + size * 0.62, size * 0.42, thick, 45, colour);
        Draw.bar(gfx, x + size * 0.62, y + size * 0.48, size * 0.72, thick, -45, colour);
    }

    /** Multiplication sign, for close and clear buttons. */
    public static void cross(GuiGraphicsExtractor gfx, double cx, double cy, double size, int colour) {
        double thick = Math.max(1.1, size * 0.16);
        Draw.bar(gfx, cx, cy, size, thick, 45, colour);
        Draw.bar(gfx, cx, cy, size, thick, -45, colour);
    }

    /** Circular arrow, for the reset button. */
    public static void reset(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;
        double r = size * 0.42;
        double thick = Math.max(1.0, size * 0.14);
        // Ring with a gap at the top right, plus an arrowhead.
        Draw.shape(gfx, cy - r - 1, cy + r + 1, yy -> {
            double[] band = Shapes.ring(cx, cy, r, r - thick).at(yy);
            if (band.length == 0 || yy > cy - r * 0.25) {
                return band;
            }
            // Clip the upper-right quadrant away to leave the gap.
            if (band.length == 4) {
                return new double[] { band[0], band[1] };
            }
            return new double[] { band[0], Math.min(band[1], cx) };
        }, colour);
        Draw.polygon(gfx,
                new double[] { cx, cx + thick * 2.1, cx },
                new double[] { cy - r - thick, cy - r + thick * 0.4, cy - r + thick * 1.8 }, colour);
    }

    /** Resize grip: a stack of dots in the corner. */
    public static void grip(GuiGraphicsExtractor gfx, int x, int y, int size, int colour) {
        double r = Math.max(0.9, size * 0.09);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j + i < 3; j++) {
                Draw.circle(gfx, x + size - 2 - j * size * 0.3, y + size - 2 - i * size * 0.3, r, colour);
            }
        }
    }
}
