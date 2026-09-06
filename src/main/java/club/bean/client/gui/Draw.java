package club.bean.client.gui;

import club.bean.client.gui.Shapes.Span;
import club.bean.client.theme.Colours;
import club.bean.client.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Every pixel the client draws goes through here.
 *
 * <p>Minecraft only hands out axis-aligned rectangles, so curves have to be
 * built out of horizontal spans. Doing that with whole pixels is what makes a
 * hand-rolled GUI look like a staircase, so instead {@link #shape} samples each
 * shape several times per pixel row, works out how much of each edge pixel is
 * really covered, and fades those pixels by that fraction. Interior pixels stay
 * fully opaque and consecutive identical rows are merged into one rectangle, so
 * a plain rounded panel still costs about as many draws as it used to.
 *
 * <p>{@link #setOpacity} is a global multiplier applied to every colour, which
 * is how the whole window fades in and out without per-call plumbing.
 */
public final class Draw {
    /** Coffee beans lean. This is the tilt, in degrees, of every bean drawn. */
    public static final double BEAN_ANGLE = -24.0;

    /** Sub-scanlines per pixel row. Four is enough to hide the stepping. */
    private static final int SUB = 4;

    private static final double[][] SAMPLES = new double[SUB][];
    private static final double[] COVERAGE = new double[1024];

    private static float opacity = 1f;

    private Draw() {
    }

    public static void setOpacity(float value) {
        opacity = Anim.clamp01(value);
    }

    public static float opacity() {
        return opacity;
    }

    /** Applies the global fade to a colour. Every helper here calls it for you. */
    public static int col(int argb) {
        return opacity >= 1f ? argb : Colours.fade(argb, opacity);
    }

    // ---- the anti-aliased scanline filler ---------------------------------

    public static void shape(GuiGraphicsExtractor gfx, double yTop, double yBottom, Span span, int colour) {
        int c = col(colour);
        int alpha = (c >>> 24) & 0xFF;
        if (alpha == 0) {
            return;
        }
        int rgb = c & 0xFFFFFF;

        int first = (int) Math.floor(yTop);
        int last = (int) Math.ceil(yBottom);

        // A run of identical full-coverage rows is emitted as one rectangle.
        boolean pending = false;
        int pendX0 = 0;
        int pendX1 = 0;
        int pendY0 = 0;

        for (int y = first; y < last; y++) {
            int count = 0;
            int lo = Integer.MAX_VALUE;
            int hi = Integer.MIN_VALUE;
            boolean uniform = true;

            for (int s = 0; s < SUB; s++) {
                double[] iv = span.at(y + (s + 0.5) / SUB);
                SAMPLES[s] = iv;
                if (iv == null || iv.length == 0) {
                    uniform = false;
                    continue;
                }
                if (iv.length != 2) {
                    uniform = false;
                }
                count++;
                lo = Math.min(lo, (int) Math.floor(iv[0]));
                hi = Math.max(hi, (int) Math.ceil(iv[iv.length - 1]));
            }

            if (count == 0) {
                pending = flush(gfx, pending, pendX0, pendX1, pendY0, y, rgb, alpha);
                continue;
            }

            // A row whose four samples agree on integer edges is a plain
            // rectangle - the flat middle of a rounded panel, mostly.
            if (uniform && count == SUB) {
                double l = SAMPLES[0][0];
                double r = SAMPLES[0][1];
                boolean identical = l == Math.floor(l) && r == Math.floor(r);
                for (int s = 1; s < SUB && identical; s++) {
                    identical = SAMPLES[s][0] == l && SAMPLES[s][1] == r;
                }
                if (identical) {
                    int x0 = (int) l;
                    int x1 = (int) r;
                    if (pending && x0 == pendX0 && x1 == pendX1) {
                        continue;
                    }
                    pending = flush(gfx, pending, pendX0, pendX1, pendY0, y, rgb, alpha);
                    pending = true;
                    pendX0 = x0;
                    pendX1 = x1;
                    pendY0 = y;
                    continue;
                }
            }

            pending = flush(gfx, pending, pendX0, pendX1, pendY0, y, rgb, alpha);
            emitRow(gfx, y, lo, hi, rgb, alpha);
        }

        flush(gfx, pending, pendX0, pendX1, pendY0, last, rgb, alpha);
    }

    private static boolean flush(GuiGraphicsExtractor gfx, boolean pending, int x0, int x1,
                                 int y0, int y1, int rgb, int alpha) {
        if (pending && x1 > x0 && y1 > y0) {
            gfx.fill(x0, y0, x1, y1, (alpha << 24) | rgb);
        }
        return false;
    }

    /** Accumulates per-pixel coverage for one row, then emits it as merged runs. */
    private static void emitRow(GuiGraphicsExtractor gfx, int y, int lo, int hi, int rgb, int alpha) {
        int width = hi - lo;
        if (width <= 0 || width > COVERAGE.length) {
            return;
        }
        java.util.Arrays.fill(COVERAGE, 0, width, 0.0);

        double share = 1.0 / SUB;
        for (int s = 0; s < SUB; s++) {
            double[] iv = SAMPLES[s];
            if (iv == null) {
                continue;
            }
            for (int k = 0; k + 1 < iv.length; k += 2) {
                double l = iv[k];
                double r = iv[k + 1];
                if (r <= l) {
                    continue;
                }
                int from = Math.max(lo, (int) Math.floor(l));
                int to = Math.min(hi, (int) Math.ceil(r));
                for (int x = from; x < to; x++) {
                    // How much of pixel [x, x+1) this sub-scanline covers.
                    double overlap = Math.min(r, x + 1) - Math.max(l, x);
                    if (overlap > 0) {
                        COVERAGE[x - lo] += overlap * share;
                    }
                }
            }
        }

        int runStart = -1;
        int runAlpha = -1;
        for (int i = 0; i <= width; i++) {
            int a = i == width ? -1 : (int) Math.round(Math.min(1, COVERAGE[i]) * alpha);
            if (a != runAlpha) {
                if (runAlpha > 0 && runStart >= 0) {
                    gfx.fill(lo + runStart, y, lo + i, y + 1, (runAlpha << 24) | rgb);
                }
                runStart = i;
                runAlpha = a;
            }
        }
    }

    // ---- rectangles -------------------------------------------------------

    public static void rect(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int colour) {
        if (w <= 0 || h <= 0) {
            return;
        }
        gfx.fill(x, y, x + w, y + h, col(colour));
    }

    /** 1px square border just inside the given bounds. */
    public static void border(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int colour) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int c = col(colour);
        gfx.fill(x, y, x + w, y + 1, c);
        gfx.fill(x, y + h - 1, x + w, y + h, c);
        gfx.fill(x, y + 1, x + 1, y + h - 1, c);
        gfx.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    public static void roundRect(GuiGraphicsExtractor gfx, double x, double y, double w, double h,
                                 double radius, int colour) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (radius < 0.5) {
            gfx.fill((int) Math.round(x), (int) Math.round(y),
                    (int) Math.round(x + w), (int) Math.round(y + h), col(colour));
            return;
        }
        shape(gfx, y, y + h, Shapes.roundRect(x, y, w, h, radius), colour);
    }

    /** Filled rounded rect with a 1px rounded border in a second colour. */
    public static void roundRectOutlined(GuiGraphicsExtractor gfx, double x, double y, double w, double h,
                                         double radius, int fill, int outline) {
        roundRect(gfx, x, y, w, h, radius, outline);
        roundRect(gfx, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
    }

    /** Hairline rounded border with nothing inside it. */
    public static void roundBorder(GuiGraphicsExtractor gfx, double x, double y, double w, double h,
                                   double radius, double thickness, int colour) {
        shape(gfx, y, y + h, yy -> {
            double[] outer = Shapes.roundRect(x, y, w, h, radius).at(yy);
            double[] inner = Shapes.roundRect(x + thickness, y + thickness,
                    w - thickness * 2, h - thickness * 2, Math.max(0, radius - thickness)).at(yy);
            if (outer.length == 0) {
                return Shapes.EMPTY;
            }
            if (inner.length == 0) {
                return outer;
            }
            return new double[] { outer[0], inner[0], inner[1], outer[1] };
        }, colour);
    }

    public static void circle(GuiGraphicsExtractor gfx, double cx, double cy, double r, int colour) {
        shape(gfx, cy - r - 1, cy + r + 1, Shapes.circle(cx, cy, r), colour);
    }

    public static void ring(GuiGraphicsExtractor gfx, double cx, double cy, double outer, double inner,
                            int colour) {
        shape(gfx, cy - outer - 1, cy + outer + 1, Shapes.ring(cx, cy, outer, inner), colour);
    }

    public static void polygon(GuiGraphicsExtractor gfx, double[] xs, double[] ys, int colour) {
        double top = Double.MAX_VALUE;
        double bottom = -Double.MAX_VALUE;
        for (double y : ys) {
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
        }
        shape(gfx, top, bottom, Shapes.polygon(xs, ys), colour);
    }

    /** Rotated capsule-ish bar, used for icon strokes. */
    public static void bar(GuiGraphicsExtractor gfx, double cx, double cy, double length,
                           double thickness, double degrees, int colour) {
        double reach = (length + thickness) / 2 + 1;
        shape(gfx, cy - reach, cy + reach, Shapes.bar(cx, cy, length, thickness, degrees), colour);
    }

    // ---- the bean --------------------------------------------------------

    /**
     * A coffee bean: a tilted ellipse with an S-curved crease down its long
     * axis. Fits inside {@code w x h}.
     *
     * @param crease pass the same colour as {@code body} for a plain silhouette
     */
    public static void bean(GuiGraphicsExtractor gfx, double cx, double cy, double w, double h,
                            int body, int crease) {
        if (w < 3 || h < 2) {
            return;
        }
        double reach = Shapes.beanHalfHeight(h);
        shape(gfx, cy - reach, cy + reach, Shapes.bean(cx, cy, w, h, BEAN_ANGLE), body);

        if (crease != body) {
            drawCrease(gfx, cx, cy, w, h, crease);
        }
    }

    public static void beanSilhouette(GuiGraphicsExtractor gfx, double cx, double cy, double w, double h,
                                      int colour) {
        bean(gfx, cx, cy, w, h, colour, colour);
    }

    /**
     * The groove. In the bean's own frame it is a single sine period along the
     * long axis - the S-shape real beans have - stroked with overlapping round
     * dots so the curve stays smooth at any size.
     */
    private static void drawCrease(GuiGraphicsExtractor gfx, double cx, double cy, double w, double h,
                                   int colour) {
        double angle = Math.toRadians(BEAN_ANGLE);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        // Sized against the bean's own axes, not its bounding box.
        double[] axes = Shapes.beanAxes(w, h, BEAN_ANGLE);
        double reach = axes[0] * 0.78;
        double amplitude = axes[1] * 0.24;
        double radius = Math.max(0.55, axes[1] * 0.21);

        int steps = Math.max(6, (int) (reach * 1.8));
        for (int i = 0; i <= steps; i++) {
            double u = -reach + (reach * 2) * i / steps;
            double v = Math.sin(u / reach * Math.PI) * amplitude;
            circle(gfx, cx + u * cos - v * sin, cy + u * sin + v * cos, radius, colour);
        }
    }

    // ---- widgets ----------------------------------------------------------

    /**
     * The pill toggle. {@code progress} is the eased 0-1 on-state; the knob
     * uses a slight overshoot so it lands with a bit of spring.
     */
    public static void toggleSwitch(GuiGraphicsExtractor gfx, double x, double y, double w, double h,
                                    float progress, Theme theme) {
        double radius = h / 2;
        int off = Colours.lighten(theme.panelAlt, 0.10f);
        int track = Colours.mix(off, theme.accent, progress);
        roundRect(gfx, x, y, w, h, radius, track);

        double knob = h - 3;
        double travel = w - knob - 3;
        float eased = Anim.clamp01(Anim.easeOutBack(progress));
        double knobX = x + 1.5 + travel * eased;
        int knobColour = Colours.mix(theme.textDim, Colours.contrastOn(theme.accent), progress);
        circle(gfx, knobX + knob / 2, y + h / 2, knob / 2, knobColour);
    }

    /** Horizontal slider track with a filled portion and a round handle. */
    public static void slider(GuiGraphicsExtractor gfx, double x, double y, double w, double h,
                              float fraction, Theme theme) {
        double trackY = y + h / 2 - 1.5;
        roundRect(gfx, x, trackY, w, 3, 1.5, Colours.lighten(theme.panelAlt, 0.12f));
        double filled = w * Anim.clamp01(fraction);
        if (filled > 1) {
            roundRect(gfx, x, trackY, filled, 3, 1.5, theme.accent);
        }
        double handle = Math.min(h - 2, 9);
        double handleX = Anim.clamp(x + filled, x, x + w);
        circle(gfx, handleX, y + h / 2, handle / 2 + 1, Colours.darken(theme.panel, 0.25f));
        circle(gfx, handleX, y + h / 2, handle / 2, theme.text);
    }

    /**
     * The faint motif behind the panels. Beans sit on a brick grid and are
     * clipped to the panel, so it reads as continuous wallpaper rather than a
     * row of icons.
     */
    public static void backgroundPattern(GuiGraphicsExtractor gfx, int x, int y, int w, int h, Theme theme) {
        if (theme.pattern == Theme.Pattern.NONE || theme.patternOpacity <= 0.001f) {
            return;
        }
        int tint = Colours.withAlpha(theme.text, Math.round(255 * theme.patternOpacity * opacity));
        if (Colours.alpha(tint) == 0) {
            return;
        }

        gfx.enableScissor(x, y, x + w, y + h);
        float previous = opacity;
        opacity = 1f;
        switch (theme.pattern) {
            case BEAN -> {
                int spacingX = 74;
                int spacingY = 58;
                for (int row = 0; row * spacingY < h + spacingY; row++) {
                    int offset = (row % 2 == 0) ? 0 : spacingX / 2;
                    for (int cx = x + 20 + offset; cx < x + w + spacingX; cx += spacingX) {
                        beanSilhouette(gfx, cx, y + 22 + row * spacingY, 26, 18, tint);
                    }
                }
            }
            case DOTS -> {
                for (int py = y + 8; py < y + h; py += 16) {
                    for (int px = x + 8; px < x + w; px += 16) {
                        circle(gfx, px, py, 1.4, tint);
                    }
                }
            }
            case GRID -> {
                for (int px = x; px < x + w; px += 24) {
                    gfx.fill(px, y, px + 1, y + h, tint);
                }
                for (int py = y; py < y + h; py += 24) {
                    gfx.fill(x, py, x + w, py + 1, tint);
                }
            }
            default -> {
            }
        }
        opacity = previous;
        gfx.disableScissor();
    }

    // ---- text -------------------------------------------------------------

    public static void text(GuiGraphicsExtractor gfx, Font font, String s, int x, int y, int colour) {
        gfx.text(font, s, x, y, col(colour), false);
    }

    public static void textShadow(GuiGraphicsExtractor gfx, Font font, String s, int x, int y, int colour) {
        gfx.text(font, s, x, y, col(colour), true);
    }

    public static void textRight(GuiGraphicsExtractor gfx, Font font, String s, int right, int y, int colour) {
        gfx.text(font, s, right - font.width(s), y, col(colour), false);
    }

    public static void textCentred(GuiGraphicsExtractor gfx, Font font, String s, int cx, int y, int colour) {
        gfx.text(font, s, cx - font.width(s) / 2, y, col(colour), false);
    }

    /** Truncates with an ellipsis so long names never bleed past their column. */
    public static String clip(Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) {
            return s;
        }
        String trimmed = font.plainSubstrByWidth(s, Math.max(0, maxWidth - font.width("...")));
        return trimmed + "...";
    }

    /**
     * Legacy 9x9 pixel-mask glyph, kept so a category added by hand can supply
     * a simple icon without drawing vectors. {@code '#'} is on.
     */
    public static void glyph(GuiGraphicsExtractor gfx, String[] mask, int x, int y, int scale, int colour) {
        int c = col(colour);
        for (int row = 0; row < mask.length; row++) {
            String line = mask[row];
            int runStart = -1;
            for (int column = 0; column <= line.length(); column++) {
                boolean on = column < line.length() && line.charAt(column) == '#';
                if (on && runStart < 0) {
                    runStart = column;
                } else if (!on && runStart >= 0) {
                    gfx.fill(x + runStart * scale, y + row * scale,
                            x + column * scale, y + (row + 1) * scale, c);
                    runStart = -1;
                }
            }
        }
    }
}
