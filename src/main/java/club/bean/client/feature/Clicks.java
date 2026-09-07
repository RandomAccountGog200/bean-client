package club.bean.client.feature;

/**
 * Counts clicks you have already made, for display only.
 *
 * <p>This is a measurement, not an input source — nothing here ever synthesises
 * a click.
 *
 * <p>Each button keeps a fixed ring of timestamps rather than a growing queue,
 * so sampling every frame allocates nothing and the rate is just "how many of
 * the last N stamps fall inside the window". The ring is sized well past what a
 * hand can produce; if someone did exceed it the count saturates rather than
 * misreporting.
 */
public final class Clicks {
    private static final long WINDOW_MS = 1000L;
    private static final int CAPACITY = 64;

    private static final Button LEFT = new Button();
    private static final Button RIGHT = new Button();

    private Clicks() {
    }

    /** Called once per frame with the current physical button states. */
    public static void sample(boolean leftDown, boolean rightDown) {
        long now = System.currentTimeMillis();
        LEFT.sample(leftDown, now);
        RIGHT.sample(rightDown, now);
    }

    public static int left() {
        return LEFT.rate(System.currentTimeMillis());
    }

    public static int right() {
        return RIGHT.rate(System.currentTimeMillis());
    }

    private static final class Button {
        private final long[] stamps = new long[CAPACITY];
        private int next;
        private boolean wasDown;

        void sample(boolean down, long now) {
            if (down && !wasDown) {
                stamps[next] = now;
                next = (next + 1) % CAPACITY;
            }
            wasDown = down;
        }

        int rate(long now) {
            int count = 0;
            for (long stamp : stamps) {
                if (stamp != 0 && now - stamp <= WINDOW_MS) {
                    count++;
                }
            }
            return count;
        }
    }
}
