package club.bean.client.gui;

/**
 * Shape definitions for the anti-aliased scanline filler in {@link Draw}.
 *
 * <p>A shape is described by where its edges are at a given height, as
 * {@code {left0, right0, left1, right1, ...}}. Returning the extents as real
 * numbers rather than pixels is what lets {@code Draw} work out how much of
 * each edge pixel is actually covered, which is the whole trick behind the
 * curves not looking like staircases.
 */
public final class Shapes {
    /** No shape at this height. */
    static final double[] EMPTY = new double[0];

    @FunctionalInterface
    public interface Span {
        /**
         * @param y a height, sampled several times per pixel row
         * @return interleaved left/right pairs, or {@link #EMPTY} where the
         *         shape does not reach this height
         */
        double[] at(double y);
    }

    private Shapes() {
    }

    /** Rectangle with all four corners rounded to {@code radius}. */
    public static Span roundRect(double x, double y, double w, double h, double radius) {
        double r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        return yy -> {
            if (yy < y || yy > y + h) {
                return EMPTY;
            }
            double inset = 0;
            double fromTop = yy - y;
            double fromBottom = y + h - yy;
            double d = Math.min(fromTop, fromBottom);
            if (d < r) {
                // Horizontal distance from the corner circle's centre.
                inset = r - Math.sqrt(Math.max(0, r * r - (r - d) * (r - d)));
            }
            if (inset * 2 >= w) {
                return EMPTY;
            }
            return new double[] { x + inset, x + w - inset };
        };
    }

    public static Span circle(double cx, double cy, double radius) {
        return ellipse(cx, cy, radius, radius);
    }

    public static Span ellipse(double cx, double cy, double rx, double ry) {
        return yy -> {
            double dy = (yy - cy) / ry;
            if (dy <= -1 || dy >= 1) {
                return EMPTY;
            }
            double half = rx * Math.sqrt(1 - dy * dy);
            return new double[] { cx - half, cx + half };
        };
    }

    /** Annulus. Produces two spans through the middle and one across the caps. */
    public static Span ring(double cx, double cy, double outer, double inner) {
        return ellipseRing(cx, cy, outer, outer, inner, inner);
    }

    public static Span ellipseRing(double cx, double cy, double rxOut, double ryOut,
                                   double rxIn, double ryIn) {
        return yy -> {
            double dyO = (yy - cy) / ryOut;
            if (dyO <= -1 || dyO >= 1) {
                return EMPTY;
            }
            double outHalf = rxOut * Math.sqrt(1 - dyO * dyO);

            double dyI = ryIn <= 0 ? 2 : (yy - cy) / ryIn;
            if (dyI <= -1 || dyI >= 1) {
                return new double[] { cx - outHalf, cx + outHalf };
            }
            double inHalf = rxIn * Math.sqrt(1 - dyI * dyI);
            if (inHalf >= outHalf) {
                return EMPTY;
            }
            return new double[] { cx - outHalf, cx - inHalf, cx + inHalf, cx + outHalf };
        };
    }

    /**
     * Convex polygon. Each height crosses exactly two edges, so the span is
     * simply the leftmost and rightmost crossing.
     */
    public static Span polygon(double[] xs, double[] ys) {
        return yy -> {
            double lo = Double.MAX_VALUE;
            double hi = -Double.MAX_VALUE;
            for (int i = 0; i < xs.length; i++) {
                int j = (i + 1) % xs.length;
                double y0 = ys[i];
                double y1 = ys[j];
                if (y0 == y1 || yy < Math.min(y0, y1) || yy >= Math.max(y0, y1)) {
                    continue;
                }
                double x = xs[i] + (xs[j] - xs[i]) * (yy - y0) / (y1 - y0);
                lo = Math.min(lo, x);
                hi = Math.max(hi, x);
            }
            return hi > lo ? new double[] { lo, hi } : EMPTY;
        };
    }

    /** A rectangle of {@code length} x {@code thickness} rotated about its centre. */
    public static Span bar(double cx, double cy, double length, double thickness, double degrees) {
        double a = Math.toRadians(degrees);
        double cos = Math.cos(a);
        double sin = Math.sin(a);
        double hl = length / 2;
        double ht = thickness / 2;
        double[] xs = new double[4];
        double[] ys = new double[4];
        double[][] corners = { { -hl, -ht }, { hl, -ht }, { hl, ht }, { -hl, ht } };
        for (int i = 0; i < 4; i++) {
            xs[i] = cx + corners[i][0] * cos - corners[i][1] * sin;
            ys[i] = cy + corners[i][0] * sin + corners[i][1] * cos;
        }
        return polygon(xs, ys);
    }

    /** Lens shape where two circles overlap — the outline of an eye. */
    public static Span lens(double cx, double cy, double halfWidth, double halfHeight) {
        // Radius and offset that make two circles meet at the requested corners.
        double r = (halfWidth * halfWidth + halfHeight * halfHeight) / (2 * halfHeight);
        double offset = r - halfHeight;
        return yy -> {
            double topHalf = span(yy, cy + offset, r);
            double bottomHalf = span(yy, cy - offset, r);
            if (topHalf <= 0 || bottomHalf <= 0) {
                return EMPTY;
            }
            double half = Math.min(topHalf, bottomHalf);
            return new double[] { cx - half, cx + half };
        };
    }

    private static double span(double yy, double centreY, double radius) {
        double dy = yy - centreY;
        double inside = radius * radius - dy * dy;
        return inside <= 0 ? 0 : Math.sqrt(inside);
    }

    /**
     * A coffee bean: an ellipse tilted by {@code degrees}, sized so the tilted
     * result still fits the requested box.
     *
     * <p>Substituting the rotation into the ellipse equation leaves a quadratic
     * in x for each height; its two roots are the span.
     */
    public static Span bean(double cx, double cy, double w, double h, double degrees) {
        double halfW = w / 2;
        double halfH = h / 2;
        double a = Math.toRadians(degrees);
        double cos = Math.cos(a);
        double sin = Math.sin(a);

        // Solve for the semi-axes whose rotated bounding box is exactly w x h.
        double cos2 = Math.cos(2 * a);
        double sum = halfW * halfW + halfH * halfH;
        double diff = halfW * halfW - halfH * halfH;
        double aSq = (sum + diff / cos2) / 2;
        double bSq = sum - aSq;
        if (aSq <= 0.25 || bSq <= 0.25) {
            return yy -> EMPTY;
        }

        double invA = 1 / aSq;
        double invB = 1 / bSq;
        double qa = cos * cos * invA + sin * sin * invB;

        return yy -> {
            double dy = yy - cy;
            double qb = 2 * dy * sin * cos * (invA - invB);
            double qc = dy * dy * (sin * sin * invA + cos * cos * invB) - 1;
            double disc = qb * qb - 4 * qa * qc;
            if (disc <= 0) {
                return EMPTY;
            }
            double root = Math.sqrt(disc);
            return new double[] { cx + (-qb - root) / (2 * qa), cx + (-qb + root) / (2 * qa) };
        };
    }

    /** Vertical extent to scan for a bean drawn by {@link #bean}. */
    public static double beanHalfHeight(double h) {
        return h / 2 + 1;
    }

    /**
     * The bean's semi-axes in its own untilted frame, as {@code {major, minor}}.
     * The crease has to be sized against these rather than against the bounding
     * box, or it swallows the body at small sizes.
     */
    public static double[] beanAxes(double w, double h, double degrees) {
        double halfW = w / 2;
        double halfH = h / 2;
        double cos2 = Math.cos(2 * Math.toRadians(degrees));
        double sum = halfW * halfW + halfH * halfH;
        double diff = halfW * halfW - halfH * halfH;
        double aSq = (sum + diff / cos2) / 2;
        double bSq = sum - aSq;
        if (aSq <= 0.25 || bSq <= 0.25) {
            return new double[] { halfW, halfH };
        }
        return new double[] { Math.sqrt(aSq), Math.sqrt(bSq) };
    }
}
