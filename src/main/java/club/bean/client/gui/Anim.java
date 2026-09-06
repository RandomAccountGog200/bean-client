package club.bean.client.gui;

/**
 * Frame-rate independent easing.
 *
 * <p>Everything animated in this GUI is a value chasing a target, stepped once
 * per frame by {@link #approach}. Using wall-clock deltas rather than tick
 * counts keeps the open/close animation the same speed whether the game is
 * running at 20 FPS or 300, and means it still animates while the world is
 * paused on a server screen.
 */
public final class Anim {
    private Anim() {
    }

    /**
     * Moves {@code value} toward {@code target} with exponential damping.
     *
     * @param speed roughly "how many e-foldings per second" - 12 is snappy,
     *              5 is lazy
     */
    public static float approach(float value, float target, float deltaSeconds, float speed) {
        if (deltaSeconds <= 0f) {
            return value;
        }
        // 1 - e^(-speed * dt) is the fraction of the remaining gap to close
        // this frame. Unlike a flat lerp factor it does not change behaviour
        // with frame rate.
        float t = 1f - (float) Math.exp(-speed * Math.min(deltaSeconds, 0.25f));
        float next = value + (target - value) * t;
        return Math.abs(target - next) < 0.001f ? target : next;
    }

    /** Decelerating ease, for things entering the screen. */
    public static float easeOutCubic(float t) {
        float x = clamp01(t);
        float inv = 1f - x;
        return 1f - inv * inv * inv;
    }

    /** Slight overshoot on the way in, so the window feels like it lands. */
    public static float easeOutBack(float t) {
        float x = clamp01(t);
        float c1 = 1.20f;
        float c3 = c1 + 1f;
        float inv = x - 1f;
        return 1f + c3 * inv * inv * inv + c1 * inv * inv;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp01(t);
    }

    public static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    public static int clamp(int v, int min, int max) {
        return v < min ? min : Math.min(v, max);
    }

    public static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }
}
