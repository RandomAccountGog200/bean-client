package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Hides chat you have already seen.
 *
 * <p>Purely a display filter on messages the server already sent you. It does
 * not mute anyone for anyone else, and nothing is sent back.
 */
public final class ChatFilter {
    private static final int MEMORY = 24;
    private static final Deque<String> RECENT = new ArrayDeque<>();

    private ChatFilter() {
    }

    /** Drops the seen-message ring, so anything hidden shows up again. */
    public static void forget() {
        RECENT.clear();
    }

    /** @return false to swallow the message */
    public static boolean allow(Component message) {
        if (message == null || !Settings.enabled("chat_filter")) {
            return true;
        }
        String text = message.getString();
        if (text.isBlank()) {
            return true;
        }

        if (Settings.flag("chat_filter", "Hide duplicates", true)) {
            String key = text.trim().toLowerCase(Locale.ROOT);
            if (RECENT.contains(key)) {
                return false;
            }
            RECENT.addLast(key);
            while (RECENT.size() > MEMORY) {
                RECENT.removeFirst();
            }
        }

        return !(Settings.flag("chat_filter", "Hide links", false) && hasLink(text));
    }

    private static boolean hasLink(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("discord.gg/") || lower.contains("http://") || lower.contains("https://");
    }
}
