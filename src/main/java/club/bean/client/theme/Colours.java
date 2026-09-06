package club.bean.client.theme;

import java.util.Locale;

/** ARGB colour maths shared by the theme system and the renderer. */
public final class Colours {
    private Colours() {
    }

    /** Parses {@code #RGB}, {@code #RRGGBB} or {@code #AARRGGBB} (the {@code #} is optional). */
    public static Integer parse(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        try {
            switch (s.length()) {
                case 3 -> {
                    int r = Integer.parseInt(s.substring(0, 1).repeat(2), 16);
                    int g = Integer.parseInt(s.substring(1, 2).repeat(2), 16);
                    int b = Integer.parseInt(s.substring(2, 3).repeat(2), 16);
                    return 0xFF000000 | (r << 16) | (g << 8) | b;
                }
                case 6 -> {
                    return 0xFF000000 | Integer.parseInt(s, 16);
                }
                case 8 -> {
                    return (int) Long.parseLong(s, 16);
                }
                default -> {
                    return null;
                }
            }
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String toHex(int argb) {
        return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
    }

    public static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    public static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    public static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    public static int blue(int argb) {
        return argb & 0xFF;
    }

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /** Replaces the alpha channel. {@code a} is 0-255. */
    public static int withAlpha(int argb, int a) {
        return (clamp(a) << 24) | (argb & 0xFFFFFF);
    }

    /** Multiplies the existing alpha by {@code factor} (0-1). */
    public static int fade(int argb, float factor) {
        int a = Math.round(alpha(argb) * Math.max(0f, Math.min(1f, factor)));
        return withAlpha(argb, a);
    }

    /** Linear blend; {@code t} of 0 returns {@code a}, 1 returns {@code b}. Alpha comes from {@code a}. */
    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return (alpha(a) << 24)
                | (Math.round(red(a) + (red(b) - red(a)) * t) << 16)
                | (Math.round(green(a) + (green(b) - green(a)) * t) << 8)
                | Math.round(blue(a) + (blue(b) - blue(a)) * t);
    }

    public static int lighten(int argb, float amount) {
        return mix(argb, 0xFFFFFFFF, amount);
    }

    public static int darken(int argb, float amount) {
        return mix(argb, 0xFF000000, amount);
    }

    /** Relative luminance, 0-1. Used to pick readable text over an arbitrary accent. */
    public static float luminance(int argb) {
        return (0.2126f * red(argb) + 0.7152f * green(argb) + 0.0722f * blue(argb)) / 255f;
    }

    /** Black or white, whichever reads better on {@code background}. */
    public static int contrastOn(int background) {
        return luminance(background) > 0.55f ? 0xFF14100C : 0xFFFFFFFF;
    }

    /** {@code h} is 0-1, {@code s} and {@code v} are 0-1. Returns opaque ARGB. */
    public static int fromHsv(float h, float s, float v) {
        h = ((h % 1f) + 1f) % 1f;
        s = Math.max(0f, Math.min(1f, s));
        v = Math.max(0f, Math.min(1f, v));

        int sector = (int) (h * 6f) % 6;
        float f = h * 6f - (int) (h * 6f);
        float p = v * (1f - s);
        float q = v * (1f - f * s);
        float t = v * (1f - (1f - f) * s);

        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return rgb(Math.round(r * 255f), Math.round(g * 255f), Math.round(b * 255f));
    }

    /** Returns {hue, saturation, value}, each 0-1. */
    public static float[] toHsv(int argb) {
        float r = red(argb) / 255f;
        float g = green(argb) / 255f;
        float b = blue(argb) / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h = 0f;
        if (delta > 1e-5f) {
            if (max == r) {
                h = ((g - b) / delta) / 6f;
            } else if (max == g) {
                h = (2f + (b - r) / delta) / 6f;
            } else {
                h = (4f + (r - g) / delta) / 6f;
            }
            h = ((h % 1f) + 1f) % 1f;
        }
        return new float[] { h, max <= 0f ? 0f : delta / max, max };
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }
}
