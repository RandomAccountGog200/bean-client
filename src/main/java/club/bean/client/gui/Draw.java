package club.bean.client.gui;

import club.bean.client.gui.Shapes.Span;
import club.bean.client.theme.Colours;
import club.bean.client.theme.Theme;
import net.minecraft.client.Minecraft;
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
 * <p>Those spans are computed in <em>device</em> pixels, not GUI pixels. This
 * matters more than it sounds: Minecraft's GUI coordinate space is a whole
 * screen pixel at GUI scale 1, but a 3x3 block of them at scale 3, so anti-
 * aliasing in GUI space produces smooth-but-chunky curves that get blockier the
 * larger the user's GUI scale. {@link #shape} instead scales the transform down
 * by the GUI scale and rasterises at the real resolution, so a rounded corner is
 * as smooth at scale 4 as at scale 1. See {@link #shape} for the mechanics.
 *
 * <p>{@link #setOpacity} is a global multiplier applied to every colour, which
 * is how the whole window fades in and out without per-call plumbing.
 */
public final class Draw {
    /** Coffee beans lean. This is the tilt, in degrees, of every bean drawn. */
    public static final double BEAN_ANGLE = -24.0;

    /** Sub-scanlines per pixel row. Four is enough to hide the stepping. */
    private static final int SUB_MAX = 4;
    /** Below this height, two samples look the same and cost half the rectangles. */
    private static final int SMALL_SHAPE = 16;

    /**
     * Highest resolution multiplier used for rasterising.
     *
     * <p>Drawing at device resolution costs one set of scanlines per device
     * pixel row, and a curve emits draw calls per row that cannot be merged -
     * consecutive rows sit at different horizontal insets, so there is no run to
     * collapse. That makes the cost of a curved edge scale linearly with this
     * number, with no way to optimise it away.
     *
     * <p>Two is the sweet spot. Going from 1x to 2x halves the size of a
     * rasterised pixel and removes most of the visible stepping; 3x and 4x are
     * progressively harder to see while costing proportionally more. Capping
     * here bounds the worst case at twice the original draw count instead of
     * four times it, on exactly the high GUI scales where the window is
     * physically largest.
     */
    private static final int MAX_RENDER_SCALE = 2;

    private static final double[][] SAMPLES = new double[SUB_MAX][];

    /**
     * Per-pixel coverage for one scanline zone.
     *
     * <p>Grown on demand rather than fixed: it used to be a flat 1024 doubles
     * and {@code scanZone} silently drew nothing for anything wider, which is
     * why long tracers vanished on wide screens. Rasterising at device
     * resolution makes every shape several times wider again, so the old cap
     * would have dropped most of the GUI.
     */
    private static double[] coverage = new double[2048];

    private static float opacity = 1f;

    /**
     * When set, everything rasterises in GUI space again.
     *
     * <p>Pushed in by the Fast GUI module rather than read from the registry,
     * so nothing under {@code gui/} has to know a module exists.
     */
    private static boolean fast;

    /**
     * Where rectangles go when the renderer is being driven outside Minecraft.
     *
     * <p>The rasteriser is pure arithmetic whose only output is a stream of
     * axis-aligned rectangles, so pointing that stream at a bitmap instead of
     * the game renders the exact same pixels the client would draw. That is
     * what {@code tools/preview} uses to check the GUI for pixelation and to
     * count draw calls without launching the game - which had otherwise meant
     * shipping render changes unseen.
     */
    public interface Sink {
        void fill(int x0, int y0, int x1, int y1, int argb);
    }

    private static Sink sink;
    private static int capturedScale;
    private static int capturedHeight;
    /** Scissor rectangle while capturing, in device pixels; null for none. */
    private static int[] capturedScissor;

    /** Redirects drawing into {@code target}, at the given render scale. */
    public static void beginCapture(Sink target, int scale, int height) {
        sink = target;
        capturedScale = Math.max(1, scale);
        capturedHeight = height;
    }

    public static void endCapture() {
        sink = null;
        capturedScale = 0;
        capturedHeight = 0;
        capturedScissor = null;
    }

    /** The scale a capture will actually rasterise at, after the cap. */
    public static int captureScale(int requested) {
        return Math.min(MAX_RENDER_SCALE, Math.max(1, requested));
    }

    /**
     * The one place a rectangle reaches the screen.
     *
     * <p>While capturing, coordinates stay in device space rather than being
     * mapped back through the pose - which is what makes the captured bitmap a
     * true picture of the real pixels.
     */
    /**
     * Emit for the aliased path.
     *
     * <p>{@link #shapeCheap} works in GUI coordinates and is deliberately never
     * wrapped in a pose transform - the wallpaper is drawn at GUI resolution on
     * purpose, since at its opacity the stepping is invisible and it is the
     * difference between a couple of hundred rectangles a frame and several
     * thousand. In game that is correct as-is. A capture has no transform to
     * compensate with, so the coordinates are scaled here instead, which keeps
     * the coarse GUI-pixel granularity the real thing has.
     */
    private static void emitCheap(GuiGraphicsExtractor gfx, int x0, int y0, int x1, int y1, int argb) {
        if (sink == null) {
            emit(gfx, x0, y0, x1, y1, argb);
            return;
        }
        int s = guiScale();
        emit(gfx, x0 * s, y0 * s, x1 * s, y1 * s, argb);
    }

    private static void emit(GuiGraphicsExtractor gfx, int x0, int y0, int x1, int y1, int argb) {
        if (sink != null) {
            if (capturedScissor != null) {
                // Clip exactly as the game's scissor would, or a capture shows
                // wallpaper spilling outside the panel that never renders.
                x0 = Math.max(x0, capturedScissor[0]);
                y0 = Math.max(y0, capturedScissor[1]);
                x1 = Math.min(x1, capturedScissor[2]);
                y1 = Math.min(y1, capturedScissor[3]);
                if (x1 <= x0 || y1 <= y0) {
                    return;
                }
            }
            sink.fill(x0, y0, x1, y1, argb);
            return;
        }
        gfx.fill(x0, y0, x1, y1, argb);
    }

    private Draw() {
    }

    /** True to rasterise in GUI space: blockier, roughly half the draw calls. */
    public static void setFast(boolean value) {
        fast = value;
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

    /**
     * Fills a shape, anti-aliased, at the display's real resolution.
     *
     * <p>The rasteriser underneath works in whole pixels of whatever coordinate
     * space it is handed. Handing it GUI space means its "pixels" are 2-4 screen
     * pixels wide on any normal GUI scale, and every curve in the client picks up
     * visible stair-stepping that no amount of sub-sampling can fix - the samples
     * are all inside one fat pixel.
     *
     * <p>So: scale the transform down by the GUI scale, multiply the geometry up
     * by the same factor, and rasterise in device pixels. The shape lands in
     * exactly the same place on screen, but its edges are now computed per real
     * pixel. Nothing at the call sites changes, and at GUI scale 1 this is a
     * no-op that skips the wrapping entirely.
     */
    public static void shape(GuiGraphicsExtractor gfx, double yTop, double yBottom, Span span, int colour) {
        // Nothing above or below the screen is worth rasterising. Cheap here,
        // and it matters most for ESP, which projects boxes for entities that
        // are frequently off the top or bottom of the viewport.
        if (yBottom < 0 || yTop > (sink != null ? capturedHeight : gfx.guiHeight())) {
            return;
        }
        int scale = guiScale();
        if (scale <= 1) {
            rasterise(gfx, yTop, yBottom, span, colour, 1);
            return;
        }
        if (sink == null) {
            gfx.pose().pushMatrix();
            gfx.pose().scale(1f / scale, 1f / scale);
        }
        // Every Shapes span returns a freshly allocated array (the only shared
        // one is the zero-length EMPTY), so scaling it in place is safe and
        // saves an allocation per sub-scanline.
        Span scaled = y -> {
            double[] extents = span.at(y / scale);
            for (int i = 0; i < extents.length; i++) {
                extents[i] *= scale;
            }
            return extents;
        };
        rasterise(gfx, yTop * scale, yBottom * scale, scaled, colour, scale);
        if (sink == null) {
            gfx.pose().popMatrix();
        }
    }

    /** The rasterising multiplier: the GUI scale, capped at {@link #MAX_RENDER_SCALE}. */
    private static int guiScale() {
        if (sink != null) {
            return fast ? 1 : Math.min(MAX_RENDER_SCALE, capturedScale);
        }
        Minecraft mc = Minecraft.getInstance();
        if (fast || mc == null || mc.getWindow() == null) {
            return 1;
        }
        return Math.max(1, Math.min(MAX_RENDER_SCALE, mc.getWindow().getGuiScale()));
    }

    private static void rasterise(GuiGraphicsExtractor gfx, double yTop, double yBottom, Span span,
                                  int colour, int scale) {
        int c = col(colour);
        int alpha = (c >>> 24) & 0xFF;
        if (alpha == 0) {
            return;
        }
        int rgb = c & 0xFFFFFF;

        int first = (int) Math.floor(yTop);
        int last = (int) Math.ceil(yBottom);
        // Icons and toggle knobs are small enough that extra samples only
        // produce extra part-covered pixels, each of which is another rectangle.
        // Vertical supersampling exists to compensate for fat GUI pixels: at
        // GUI scale 1 a row is one screen pixel and four samples are what hide
        // the stepping. Rasterising at device resolution already makes the rows
        // small, so sampling each of them four times over is paying twice for
        // the same thing - and at scale 3 that is nine times the work of the
        // original, which is what made the GUI crawl.
        //
        // Dividing the budget by the scale keeps the total sample count roughly
        // fixed no matter what the user's GUI scale is, while the extra rows
        // still buy the sharpness. Horizontal coverage is unaffected, so curved
        // edges keep their anti-aliasing either way.
        int budget = Math.max(1, Math.round((float) SUB_MAX / scale));
        int sub = (last - first) <= SMALL_SHAPE * scale ? Math.max(1, budget / 2) : budget;

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

            for (int s = 0; s < sub; s++) {
                double[] iv = span.at(y + (s + 0.5) / sub);
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
            if (uniform && count == sub) {
                double l = SAMPLES[0][0];
                double r = SAMPLES[0][1];
                boolean identical = l == Math.floor(l) && r == Math.floor(r);
                for (int s = 1; s < sub && identical; s++) {
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
            emitRow(gfx, y, lo, hi, rgb, alpha, sub);
        }

        flush(gfx, pending, pendX0, pendX1, pendY0, last, rgb, alpha);
    }

    private static boolean flush(GuiGraphicsExtractor gfx, boolean pending, int x0, int x1,
                                 int y0, int y1, int rgb, int alpha) {
        if (pending && x1 > x0 && y1 > y0) {
            emit(gfx, x0, y0, x1, y1, (alpha << 24) | rgb);
        }
        return false;
    }

    /**
     * Emits one row.
     *
     * <p>Only the pixels an edge passes through need per-pixel coverage; the
     * interior is solid by definition. When every sub-scanline gives a single
     * span — which is everything except a ring — this walks the two narrow edge
     * zones and fills the middle in one go. Scanning the full width instead, as
     * an earlier version did, cost hundreds of wasted iterations on every
     * rounded corner and was most of why opening the menu stuttered.
     */
    private static void emitRow(GuiGraphicsExtractor gfx, int y, int lo, int hi, int rgb, int alpha,
                                int sub) {
        if (hi - lo <= 0) {
            return;
        }

        // Every sub-scanline has to agree on how many spans there are before we
        // can pair them up. They disagree only where a hole opens or closes -
        // the caps of a ring - which is a couple of rows per shape.
        int spans = SAMPLES[0] == null ? 0 : SAMPLES[0].length / 2;
        boolean pairable = spans > 0;
        for (int s = 0; s < sub && pairable; s++) {
            pairable = SAMPLES[s] != null && SAMPLES[s].length / 2 == spans;
        }

        if (pairable) {
            for (int k = 0; k < spans; k++) {
                double leftMin = Double.MAX_VALUE;
                double leftMax = -Double.MAX_VALUE;
                double rightMin = Double.MAX_VALUE;
                double rightMax = -Double.MAX_VALUE;
                boolean usable = true;

                for (int s = 0; s < sub; s++) {
                    double l = SAMPLES[s][k * 2];
                    double r = SAMPLES[s][k * 2 + 1];
                    if (r <= l) {
                        usable = false;
                        break;
                    }
                    leftMin = Math.min(leftMin, l);
                    leftMax = Math.max(leftMax, l);
                    rightMin = Math.min(rightMin, r);
                    rightMax = Math.max(rightMax, r);
                }
                if (!usable) {
                    scanZone(gfx, y, lo, hi, rgb, alpha, k, sub);
                    continue;
                }

                int leftFrom = (int) Math.floor(leftMin);
                int leftTo = (int) Math.ceil(leftMax);
                int rightFrom = (int) Math.floor(rightMin);
                int rightTo = (int) Math.ceil(rightMax);

                if (leftTo <= rightFrom) {
                    scanZone(gfx, y, leftFrom, leftTo, rgb, alpha, k, sub);
                    if (rightFrom > leftTo) {
                        emit(gfx, leftTo, y, rightFrom, y + 1, (alpha << 24) | rgb);
                    }
                    scanZone(gfx, y, rightFrom, rightTo, rgb, alpha, k, sub);
                } else {
                    // Thinner than the sampling blur, so the zones overlap.
                    scanZone(gfx, y, leftFrom, rightTo, rgb, alpha, k, sub);
                }
            }
            return;
        }
        scanZone(gfx, y, lo, hi, rgb, alpha, -1, sub);
    }

    /**
     * Per-pixel coverage over a column range, emitted as merged runs.
     *
     * @param only index of the single span to accumulate, or -1 for all of them
     */
    private static void scanZone(GuiGraphicsExtractor gfx, int y, int from, int to, int rgb, int alpha,
                                 int only, int sub) {
        int width = to - from;
        if (width <= 0) {
            return;
        }
        if (width > coverage.length) {
            // Grow rather than bail. Returning here is what used to make a long
            // tracer disappear instead of drawing.
            coverage = new double[Integer.highestOneBit(width) * 2];
        }
        java.util.Arrays.fill(coverage, 0, width, 0.0);

        double share = 1.0 / sub;
        for (int s = 0; s < sub; s++) {
            double[] iv = SAMPLES[s];
            if (iv == null) {
                continue;
            }
            int first = only < 0 ? 0 : only * 2;
            int stop = only < 0 ? iv.length : Math.min(iv.length, only * 2 + 2);
            for (int k = first; k + 1 < stop; k += 2) {
                double l = iv[k];
                double r = iv[k + 1];
                if (r <= l) {
                    continue;
                }
                int start = Math.max(from, (int) Math.floor(l));
                int end = Math.min(to, (int) Math.ceil(r));
                for (int x = start; x < end; x++) {
                    // How much of pixel [x, x+1) this sub-scanline covers.
                    double overlap = Math.min(r, x + 1) - Math.max(l, x);
                    if (overlap > 0) {
                        coverage[x - from] += overlap * share;
                    }
                }
            }
        }

        int runStart = -1;
        int runAlpha = -1;
        for (int i = 0; i <= width; i++) {
            int a = i == width ? -1 : (int) Math.round(Math.min(1, coverage[i]) * alpha);
            if (a != runAlpha) {
                if (runAlpha > 0 && runStart >= 0) {
                    emit(gfx, from + runStart, y, from + i, y + 1, (runAlpha << 24) | rgb);
                }
                runStart = i;
                runAlpha = a;
            }
        }
    }

    /**
     * Aliased sibling of {@link #shape} for decoration that is too faint for
     * the difference to be visible — the wallpaper motif, mainly. Spans are
     * rounded to whole pixels and identical rows merged, so a shape costs about
     * one rectangle per row instead of three.
     */
    public static void shapeCheap(GuiGraphicsExtractor gfx, double yTop, double yBottom, Span span,
                                  int exactColour) {
        if ((exactColour >>> 24) == 0) {
            return;
        }
        int first = (int) Math.floor(yTop);
        int last = (int) Math.ceil(yBottom);

        boolean pending = false;
        int pendX0 = 0;
        int pendX1 = 0;
        int pendY0 = 0;

        for (int y = first; y < last; y++) {
            double[] iv = span.at(y + 0.5);
            int x0 = 0;
            int x1 = 0;
            if (iv != null && iv.length >= 2) {
                x0 = (int) Math.round(iv[0]);
                x1 = (int) Math.round(iv[iv.length - 1]);
            }
            if (x1 <= x0) {
                if (pending) {
                    emitCheap(gfx, pendX0, pendY0, pendX1, y, exactColour);
                    pending = false;
                }
                continue;
            }
            if (pending && x0 == pendX0 && x1 == pendX1) {
                continue;
            }
            if (pending) {
                emitCheap(gfx, pendX0, pendY0, pendX1, y, exactColour);
            }
            pending = true;
            pendX0 = x0;
            pendX1 = x1;
            pendY0 = y;
        }
        if (pending) {
            emitCheap(gfx, pendX0, pendY0, pendX1, last, exactColour);
        }
    }

    // ---- rectangles -------------------------------------------------------

    public static void rect(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int colour) {
        if (w <= 0 || h <= 0) {
            return;
        }
        emit(gfx, x, y, x + w, y + h, col(colour));
    }

    /** 1px square border just inside the given bounds. */
    public static void border(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int colour) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int c = col(colour);
        emit(gfx, x, y, x + w, y + 1, c);
        emit(gfx, x, y + h - 1, x + w, y + h, c);
        emit(gfx, x, y + 1, x + 1, y + h - 1, c);
        emit(gfx, x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    public static void roundRect(GuiGraphicsExtractor gfx, double x, double y, double w, double h,
                                 double radius, int colour) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (radius < 0.5) {
            emit(gfx, (int) Math.round(x), (int) Math.round(y),
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

    /**
     * Hairline rounded border with nothing inside it.
     *
     * <p>Only the corners are curved, so only the corners go through the
     * anti-aliased filler. Running the whole outline through it would cost two
     * rectangles for every row of the window - about 1400 for the main frame
     * alone - to draw four straight lines.
     */
    public static void roundBorder(GuiGraphicsExtractor gfx, double x, double y, double w, double h,
                                   double radius, double thickness, int colour) {
        if (w <= 0 || h <= 0) {
            return;
        }
        double r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        int c = col(colour);
        int t = (int) Math.max(1, Math.round(thickness));

        int left = (int) Math.round(x);
        int top = (int) Math.round(y);
        int right = (int) Math.round(x + w);
        int bottom = (int) Math.round(y + h);
        int inset = (int) Math.ceil(r);

        emit(gfx, left + inset, top, right - inset, top + t, c);
        emit(gfx, left + inset, bottom - t, right - inset, bottom, c);
        emit(gfx, left, top + inset, left + t, bottom - inset, c);
        emit(gfx, right - t, top + inset, right, bottom - inset, c);

        if (r < 0.5) {
            return;
        }
        Span outer = Shapes.roundRect(x, y, w, h, r);
        Span inner = Shapes.roundRect(x + t, y + t, w - t * 2, h - t * 2, Math.max(0, r - t));
        Span band = yy -> {
            double[] o = outer.at(yy);
            if (o.length == 0) {
                return Shapes.EMPTY;
            }
            double[] i = inner.at(yy);
            if (i.length == 0) {
                return o;
            }
            return new double[] { o[0], i[0], i[1], o[1] };
        };
        shape(gfx, y, y + inset, band, colour);
        shape(gfx, y + h - inset, y + h, band, colour);
    }

    /**
     * A one-pixel rectangle outline, drawn as four plain fills.
     *
     * <p>Deliberately not routed through {@link #shape}. An axis-aligned
     * rectangle has no curved edge for anti-aliasing to soften, so the scanline
     * filler spends its whole budget computing coverage values that all come out
     * as 0 or 1 - and it emits a fill per pixel row while doing it. ESP draws one
     * of these per visible entity per frame, which made it by far the most
     * expensive thing on screen. Four fills is the same picture, sharper.
     */
    public static void outline(GuiGraphicsExtractor gfx, double x, double y, double w, double h,
                               int colour) {
        int c = col(colour);
        if (((c >>> 24) & 0xFF) == 0 || w <= 0 || h <= 0) {
            return;
        }
        int x0 = (int) Math.round(x);
        int y0 = (int) Math.round(y);
        int x1 = (int) Math.round(x + w);
        int y1 = (int) Math.round(y + h);
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        emit(gfx, x0, y0, x1, y0 + 1, c);
        emit(gfx, x0, y1 - 1, x1, y1, c);
        emit(gfx, x0, y0 + 1, x0 + 1, y1 - 1, c);
        emit(gfx, x1 - 1, y0 + 1, x1, y1 - 1, c);
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
        double radius = Math.max(0.6, axes[1] * 0.24);

        int steps = Math.max(5, (int) (reach * 0.85));
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
    /**
     * @param covered rectangles ({@code {x, y, w, h}}) that opaque panels will
     *                paint over this pattern. Motifs falling entirely inside
     *                one are skipped - without this, most of the wallpaper is
     *                drawn and then immediately hidden by the rail and panel.
     */
    public static void backgroundPattern(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                         Theme theme, int[]... covered) {
        if (theme.pattern == Theme.Pattern.NONE || theme.patternOpacity <= 0.001f) {
            return;
        }
        int tint = Colours.withAlpha(theme.text, Math.round(255 * theme.patternOpacity * opacity));
        if (Colours.alpha(tint) == 0) {
            return;
        }

        if (sink == null) {
            gfx.enableScissor(x, y, x + w, y + h);
        } else {
            int s = guiScale();
            capturedScissor = new int[] { x * s, y * s, (x + w) * s, (y + h) * s };
        }
        float previous = opacity;
        opacity = 1f;
        switch (theme.pattern) {
            case BEAN -> {
                // Wallpaper is redrawn every frame, so it uses the aliased path
                // and a sparse grid. At this opacity the jagged edges are not
                // visible, and it is the difference between ~5800 rectangles a
                // frame and a couple of hundred.
                int spacingX = 104;
                int spacingY = 84;
                int beanW = 32;
                int beanH = 22;
                for (int row = 0; row * spacingY < h + spacingY; row++) {
                    int offset = (row % 2 == 0) ? 0 : spacingX / 2;
                    for (int cx = x + 24 + offset; cx < x + w + spacingX; cx += spacingX) {
                        int cy = y + 26 + row * spacingY;
                        if (hidden(cx - beanW / 2, cy - beanH / 2, beanW, beanH, covered)) {
                            continue;
                        }
                        shapeCheap(gfx, cy - beanH / 2.0 - 1, cy + beanH / 2.0 + 1,
                                Shapes.bean(cx, cy, beanW, beanH, BEAN_ANGLE), tint);
                    }
                }
            }
            case DOTS -> {
                for (int py = y + 8; py < y + h; py += 18) {
                    for (int px = x + 8; px < x + w; px += 18) {
                        emit(gfx, px, py, px + 2, py + 2, tint);
                    }
                }
            }
            case GRID -> {
                for (int px = x; px < x + w; px += 24) {
                    emit(gfx, px, y, px + 1, y + h, tint);
                }
                for (int py = y; py < y + h; py += 24) {
                    emit(gfx, x, py, x + w, py + 1, tint);
                }
            }
            default -> {
            }
        }
        opacity = previous;
        if (sink == null) {
            gfx.disableScissor();
        } else {
            capturedScissor = null;
        }
    }

    /** True when the box lies entirely inside one of the covering rectangles. */
    private static boolean hidden(int bx, int by, int bw, int bh, int[][] covered) {
        for (int[] r : covered) {
            if (bx >= r[0] && by >= r[1] && bx + bw <= r[0] + r[2] && by + bh <= r[1] + r[3]) {
                return true;
            }
        }
        return false;
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
                    emit(gfx, x + runStart * scale, y + row * scale,
                            x + column * scale, y + (row + 1) * scale, c);
                    runStart = -1;
                }
            }
        }
    }
}
