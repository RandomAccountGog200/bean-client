package club.bean.client.gui;

import club.bean.client.theme.Colours;
import club.bean.client.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Every pixel the client draws goes through here.
 *
 * <p>Minecraft only gives us axis-aligned rectangles, so rounded corners, the
 * bean logo and the category icons are all built out of horizontal spans. Each
 * helper merges its runs before filling - a rounded rectangle costs
 * {@code 2 * radius + 1} quads no matter how tall it is, and the bean is one
 * quad per scanline.
 *
 * <p>{@link #opacity} is a global multiplier applied to every colour, which is
 * how the whole window fades in and out without any per-call plumbing.
 */
public final class Draw {
    /** Coffee beans lean. This is the tilt, in degrees, of every bean drawn. */
    private static final double BEAN_ANGLE = -24.0;
    private static final double COS = Math.cos(Math.toRadians(BEAN_ANGLE));
    private static final double SIN = Math.sin(Math.toRadians(BEAN_ANGLE));
    private static final double COS_2T = Math.cos(2 * Math.toRadians(BEAN_ANGLE));

    private static float opacity = 1f;

    private Draw() {
    }

    public static void setOpacity(float value) {
        opacity = Anim.clamp01(value);
    }

    public static float opacity() {
        return opacity;
    }

    /** Applies the global fade to a colour. All drawing helpers call this for you. */
    public static int col(int argb) {
        return opacity >= 1f ? argb : Colours.fade(argb, opacity);
    }

    // ---- rectangles -------------------------------------------------------

    public static void rect(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int colour) {
        if (w <= 0 || h <= 0) {
            return;
        }
        gfx.fill(x, y, x + w, y + h, col(colour));
    }

    /** 1px border just inside the given bounds. */
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

    /**
     * Rounded rectangle. The corner inset per row comes off a circle, so the
     * arc is properly round rather than a chamfer.
     */
    public static void roundRect(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius, int colour) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            rect(gfx, x, y, w, h, colour);
            return;
        }
        int c = col(colour);

        for (int dy = 0; dy < r; dy++) {
            int inset = cornerInset(dy, r);
            // Mirror each corner row to the bottom - the shape is symmetric,
            // so one inset calculation covers two spans.
            gfx.fill(x + inset, y + dy, x + w - inset, y + dy + 1, c);
            gfx.fill(x + inset, y + h - dy - 1, x + w - inset, y + h - dy, c);
        }
        gfx.fill(x, y + r, x + w, y + h - r, c);
    }

    /** Filled rounded rect with a 1px rounded border in a second colour. */
    public static void roundRectOutlined(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                         int radius, int fill, int outline) {
        roundRect(gfx, x, y, w, h, radius, outline);
        roundRect(gfx, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
    }

    private static int cornerInset(int dy, int r) {
        double offset = r - dy - 0.5;
        double half = Math.sqrt(Math.max(0.0, (double) r * r - offset * offset));
        return (int) Math.round(r - half);
    }

    // ---- the bean --------------------------------------------------------

    /**
     * A coffee bean: a tilted ellipse with an S-curved crease down its long
     * axis. Fits inside {@code w x h}.
     *
     * @param crease pass the same colour as {@code body} for a plain silhouette
     */
    public static void bean(GuiGraphicsExtractor gfx, int cx, int cy, int w, int h, int body, int crease) {
        double halfW = w / 2.0;
        double halfH = h / 2.0;
        if (halfW < 1 || halfH < 1) {
            return;
        }

        // Solve for the local-frame semi-axes that make the *rotated* bean fit
        // exactly inside the requested box.
        double sum = halfW * halfW + halfH * halfH;
        double diff = halfW * halfW - halfH * halfH;
        double aSq = (sum + diff / COS_2T) / 2.0;
        double bSq = sum - aSq;
        if (aSq <= 0.5 || bSq <= 0.5) {
            return;
        }
        double a = Math.sqrt(aSq);
        double b = Math.sqrt(bSq);

        fillRotatedEllipse(gfx, cx, cy, a, b, halfH, body);

        if (crease != body) {
            drawCrease(gfx, cx, cy, a, b, crease, Math.max(1, Math.round(h / 7f)));
        }
    }

    public static void beanSilhouette(GuiGraphicsExtractor gfx, int cx, int cy, int w, int h, int colour) {
        bean(gfx, cx, cy, w, h, colour, colour);
    }

    /**
     * Scanline-fills the ellipse {@code (u/a)^2 + (v/b)^2 = 1} rotated by
     * {@link #BEAN_ANGLE}. Substituting the rotation into the ellipse equation
     * gives a quadratic in x for each row; its two roots are the span ends.
     */
    private static void fillRotatedEllipse(GuiGraphicsExtractor gfx, int cx, int cy,
                                           double a, double b, double halfH, int colour) {
        int c = col(colour);
        double invA = 1.0 / (a * a);
        double invB = 1.0 / (b * b);
        double qa = COS * COS * invA + SIN * SIN * invB;

        int top = (int) Math.floor(-halfH);
        int bottom = (int) Math.ceil(halfH);
        for (int dy = top; dy <= bottom; dy++) {
            double y = dy + 0.5;
            double qb = 2 * y * SIN * COS * (invA - invB);
            double qc = y * y * (SIN * SIN * invA + COS * COS * invB) - 1.0;

            double disc = qb * qb - 4 * qa * qc;
            if (disc <= 0) {
                continue;
            }
            double root = Math.sqrt(disc);
            int x0 = (int) Math.round((-qb - root) / (2 * qa));
            int x1 = (int) Math.round((-qb + root) / (2 * qa));
            if (x1 > x0) {
                gfx.fill(cx + x0, cy + dy, cx + x1, cy + dy + 1, c);
            }
        }
    }

    /**
     * The groove. In the bean's own frame the crease is a single sine period
     * along the long axis, which reads as the S-shape real beans have; each
     * sample is then rotated back into screen space.
     */
    private static void drawCrease(GuiGraphicsExtractor gfx, int cx, int cy,
                                   double a, double b, int colour, int thickness) {
        int c = col(colour);
        double reach = a * 0.80;
        double amplitude = b * 0.26;
        double step = 0.4;

        int last = Integer.MIN_VALUE;
        for (double u = -reach; u <= reach; u += step) {
            double v = Math.sin(u / a * Math.PI) * amplitude;
            int x = (int) Math.round(u * COS - v * SIN);
            int y = (int) Math.round(u * SIN + v * COS);
            // The samples are dense enough to overlap; skipping repeats keeps
            // the quad count down without leaving gaps.
            int packed = (x << 16) ^ (y & 0xFFFF);
            if (packed == last) {
                continue;
            }
            last = packed;
            gfx.fill(cx + x, cy + y, cx + x + thickness, cy + y + thickness, c);
        }
    }

    // ---- glyphs -----------------------------------------------------------

    /**
     * Draws a pixel mask ({@code '#'} is on) at {@code scale} pixels per cell,
     * merging each row into as few quads as it has runs.
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

    public static int glyphSize(String[] mask, int scale) {
        return mask.length * scale;
    }

    public static final String[] GEAR = {
            "...###...",
            ".#.###.#.",
            ".#######.",
            "###...###",
            "##.....##",
            "###...###",
            ".#######.",
            ".#.###.#.",
            "...###..."
    };

    public static final String[] SEARCH = {
            ".#####...",
            "#.....#..",
            "#.....#..",
            "#.....#..",
            "#.....#..",
            ".#####...",
            "....##...",
            ".....##..",
            "......##."
    };

    public static final String[] CHEVRON_DOWN = {
            ".........",
            ".........",
            ".........",
            "#.......#",
            "##.....##",
            ".##...##.",
            "..##.##..",
            "...###...",
            "....#...."
    };

    public static final String[] CHECK = {
            ".........",
            ".......##",
            "......##.",
            ".....##..",
            "#...##...",
            "##.##....",
            ".###.....",
            "..#......",
            "........."
    };

    public static final String[] RESET = {
            "..#####..",
            ".##...##.",
            "##.....##",
            "##.......",
            "##.......",
            "##.....##",
            ".##...##.",
            "..#####..",
            "###......"
    };

    // ---- composites -------------------------------------------------------

    /**
     * The pill toggle. {@code progress} is the eased 0-1 on-state so the knob
     * slides and the track cross-fades together.
     */
    public static void toggleSwitch(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                    float progress, Theme theme) {
        int radius = h / 2;
        int off = Colours.lighten(theme.panelAlt, 0.06f);
        int track = Colours.mix(off, theme.accent, progress);
        roundRect(gfx, x, y, w, h, radius, track);

        int knobSize = h - 4;
        int travel = w - knobSize - 4;
        int knobX = x + 2 + Math.round(travel * progress);
        int knobColour = Colours.mix(theme.textDim, Colours.contrastOn(theme.accent), progress);
        roundRect(gfx, knobX, y + 2, knobSize, knobSize, knobSize / 2, knobColour);
    }

    /** Horizontal slider track with a filled portion and a round handle. */
    public static void slider(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                              float fraction, Theme theme) {
        int trackY = y + h / 2 - 2;
        roundRect(gfx, x, trackY, w, 4, 2, Colours.lighten(theme.panelAlt, 0.08f));
        int filled = Math.round(w * Anim.clamp01(fraction));
        if (filled > 0) {
            roundRect(gfx, x, trackY, Math.max(4, filled), 4, 2, theme.accent);
        }
        int handle = h - 2;
        int handleX = x + filled - handle / 2;
        handleX = Anim.clamp(handleX, x - handle / 2, x + w - handle / 2);
        roundRect(gfx, handleX, y + 1, handle, handle, handle / 2, theme.text);
    }

    /**
     * The faint motif behind the panels. Beans are laid out on a brick grid and
     * clipped to the panel, so the pattern reads as continuous wallpaper rather
     * than as a row of icons.
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
        switch (theme.pattern) {
            case BEAN -> {
                int spacingX = 74;
                int spacingY = 58;
                int beanW = 26;
                int beanH = 18;
                for (int row = 0; row * spacingY < h + spacingY; row++) {
                    int offset = (row % 2 == 0) ? 0 : spacingX / 2;
                    for (int cx = x + 20 + offset; cx < x + w + spacingX; cx += spacingX) {
                        int cy = y + 22 + row * spacingY;
                        // Pass the tint straight through - it already carries
                        // the pattern alpha and the global fade.
                        beanRaw(gfx, cx, cy, beanW, beanH, tint);
                    }
                }
            }
            case DOTS -> {
                for (int py = y + 6; py < y + h; py += 14) {
                    for (int px = x + 6; px < x + w; px += 14) {
                        gfx.fill(px, py, px + 2, py + 2, tint);
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
        gfx.disableScissor();
    }

    /** Bean silhouette that skips {@link #col} - for callers that pre-multiplied alpha. */
    private static void beanRaw(GuiGraphicsExtractor gfx, int cx, int cy, int w, int h, int exactColour) {
        float previous = opacity;
        opacity = 1f;
        beanSilhouette(gfx, cx, cy, w, h, exactColour);
        opacity = previous;
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

    /** Truncates with an ellipsis so long names never bleed past their column. */
    public static String clip(Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) {
            return s;
        }
        String trimmed = font.plainSubstrByWidth(s, Math.max(0, maxWidth - font.width("...")));
        return trimmed + "...";
    }
}
