package club.bean.client.feature;

import club.bean.client.module.Module;
import club.bean.client.module.ModuleRegistry;
import club.bean.client.module.Setting;
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

    /** @return false to swallow the message */
    public static boolean allow(Component message) {
        Module module = ModuleRegistry.get("chat_filter");
        if (module == null || !module.isEnabled() || message == null) {
            return true;
        }
        String text = message.getString();
        if (text.isBlank()) {
            return true;
        }

        if (option(module, "Hide duplicates", true)) {
            String key = text.trim().toLowerCase(Locale.ROOT);
            if (RECENT.contains(key)) {
                return false;
            }
            RECENT.addLast(key);
            while (RECENT.size() > MEMORY) {
                RECENT.removeFirst();
            }
        }

        return !(option(module, "Hide links", false) && hasLink(text));
    }

    private static boolean hasLink(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("discord.gg/") || lower.contains("http://") || lower.contains("https://");
    }

    private static boolean option(Module module, String name, boolean fallback) {
        for (Setting setting : module.settings()) {
            if (setting.name().equalsIgnoreCase(name) && setting.type() == Setting.Type.TOGGLE) {
                return setting.boolValue();
            }
        }
        return fallback;
    }
}
