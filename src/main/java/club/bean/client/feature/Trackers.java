package club.bean.client.feature;

import club.bean.client.BeanConfig;
import club.bean.client.module.Module;
import club.bean.client.module.ModuleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The small stateful bits: your own movement speed, where you last died, and
 * how long you have played on each server.
 *
 * <p>Every value here is derived from your own player and the clock. None of it
 * is asked of the server, and none of it is sent anywhere.
 */
public final class Trackers {
    // ---- speed ------------------------------------------------------------
    private static double lastX;
    private static double lastZ;
    private static long lastSampleAt;
    private static double blocksPerSecond;

    // ---- deaths -----------------------------------------------------------
    private static boolean wasDead;
    private static BlockPos deathPos;

    // ---- playtime ---------------------------------------------------------
    private static String currentServer = "";
    private static long serverJoinedAt;

    private Trackers() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        trackSpeed(mc);
        trackDeath(mc);
        trackPlaytime(mc);
    }

    private static void trackSpeed(Minecraft mc) {
        if (mc.player == null) {
            blocksPerSecond = 0;
            lastSampleAt = 0;
            return;
        }
        long now = System.currentTimeMillis();
        double x = mc.player.getX();
        double z = mc.player.getZ();
        if (lastSampleAt == 0) {
            lastX = x;
            lastZ = z;
            lastSampleAt = now;
            return;
        }
        double elapsed = (now - lastSampleAt) / 1000.0;
        if (elapsed < 0.1) {
            return;
        }
        double dx = x - lastX;
        double dz = z - lastZ;
        double measured = Math.sqrt(dx * dx + dz * dz) / elapsed;
        // Smoothed, or the readout is unreadable noise.
        blocksPerSecond += (measured - blocksPerSecond) * 0.35;
        lastX = x;
        lastZ = z;
        lastSampleAt = now;
    }

    public static double blocksPerSecond() {
        return blocksPerSecond;
    }

    private static void trackDeath(Minecraft mc) {
        if (mc.player == null) {
            wasDead = false;
            return;
        }
        boolean dead = mc.player.isDeadOrDying();
        if (dead && !wasDead) {
            deathPos = mc.player.blockPosition();
            BeanConfig.setString("death.pos",
                    deathPos.getX() + "," + deathPos.getY() + "," + deathPos.getZ());
            BeanConfig.saveSoon();

            Module module = ModuleRegistry.get("death_coords");
            if (module != null && module.isEnabled()) {
                mc.player.sendSystemMessage(Component.literal(
                        "[Bean] died at " + deathPos.getX() + ", " + deathPos.getY()
                                + ", " + deathPos.getZ()));
            }
        }
        wasDead = dead;
    }

    /** Last recorded death position, restored from config if this session has none. */
    public static BlockPos deathPos() {
        if (deathPos == null) {
            String saved = BeanConfig.getString("death.pos", null);
            if (saved != null) {
                String[] parts = saved.split(",");
                if (parts.length == 3) {
                    try {
                        deathPos = new BlockPos(Integer.parseInt(parts[0].trim()),
                                Integer.parseInt(parts[1].trim()),
                                Integer.parseInt(parts[2].trim()));
                    } catch (NumberFormatException ignored) {
                        // Malformed entry; treat it as no recorded death.
                    }
                }
            }
        }
        return deathPos;
    }

    private static void trackPlaytime(Minecraft mc) {
        String server = serverKey(mc);
        if (!server.equals(currentServer)) {
            flushPlaytime();
            currentServer = server;
            serverJoinedAt = server.isEmpty() ? 0 : System.currentTimeMillis();
        }
    }

    /** Banks the elapsed minutes for the server we are on or leaving. */
    public static void flushPlaytime() {
        if (currentServer.isEmpty() || serverJoinedAt == 0) {
            return;
        }
        long minutes = (System.currentTimeMillis() - serverJoinedAt) / 60000L;
        if (minutes > 0) {
            String key = "playtime." + currentServer;
            BeanConfig.setNumber(key, BeanConfig.getNumber(key, 0) + minutes);
            BeanConfig.saveSoon();
            serverJoinedAt = System.currentTimeMillis();
        }
    }

    /** Total minutes on the current server, banked plus this session. */
    public static long playtimeMinutes(Minecraft mc) {
        String key = "playtime." + serverKey(mc);
        long stored = (long) BeanConfig.getNumber(key, 0);
        long live = serverJoinedAt == 0 ? 0 : (System.currentTimeMillis() - serverJoinedAt) / 60000L;
        return stored + live;
    }

    public static String serverKey(Minecraft mc) {
        if (mc.getCurrentServer() != null) {
            return mc.getCurrentServer().ip;
        }
        return mc.getSingleplayerServer() != null ? "singleplayer" : "";
    }

    public static String serverName(Minecraft mc) {
        if (mc.getCurrentServer() != null) {
            String name = mc.getCurrentServer().name;
            return name == null || name.isBlank() ? mc.getCurrentServer().ip : name;
        }
        return mc.getSingleplayerServer() != null ? "Singleplayer" : "-";
    }
}
