package club.bean.client.feature;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Counts clicks you have already made, for display only.
 *
 * <p>This is a measurement, not an input source - nothing here ever synthesises
 * a click. Sampling happens once per rendered frame, so it resolves rates well
 * past what a hand can produce.
 */
public final class Clicks {
    private static final long WINDOW_MS = 1000L;

    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    private static boolean leftWasDown;
    private static boolean rightWasDown;

    private Clicks() {
    }

    /** Called once per frame with the current physical button states. */
    public static void sample(boolean leftDown, boolean rightDown) {
        long now = System.currentTimeMillis();
        if (leftDown && !leftWasDown) {
            LEFT.addLast(now);
        }
        if (rightDown && !rightWasDown) {
            RIGHT.addLast(now);
        }
        leftWasDown = leftDown;
        rightWasDown = rightDown;
        trim(LEFT, now);
        trim(RIGHT, now);
    }

    private static void trim(Deque<Long> queue, long now) {
        while (!queue.isEmpty() && now - queue.peekFirst() > WINDOW_MS) {
            queue.removeFirst();
        }
    }

    public static int left() {
        return LEFT.size();
    }

    public static int right() {
        return RIGHT.size();
    }
}
