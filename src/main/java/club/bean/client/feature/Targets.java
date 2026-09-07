package club.bean.client.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Who counts as a target, in one place.
 *
 * <p>Killaura, ESP, Tracers and the radar all need the same question answered -
 * "which of the entities the client already knows about am I interested in?" -
 * and answering it four times would guarantee four different answers. The
 * filter strings come straight from a {@link club.bean.client.module.Setting}
 * mode cycler, so the module list and this class cannot drift apart.
 *
 * <p>Nothing here asks the server for anything. {@code entitiesForRendering()}
 * is the client's own view of what it has been told about, which is the same
 * set the vanilla renderer walks every frame.
 */
public final class Targets {
    /** Matches the labels used by the mode cyclers in {@code DefaultModules}. */
    public enum Filter {
        PLAYERS, MOBS, ALL;

        public static Filter parse(String label) {
            if (label == null) {
                return PLAYERS;
            }
            String lower = label.toLowerCase(Locale.ROOT);
            if (lower.startsWith("mob")) {
                return MOBS;
            }
            return lower.startsWith("all") || lower.startsWith("both") ? ALL : PLAYERS;
        }
    }

    private Targets() {
    }

    /**
     * Every living entity that passes {@code filter} within {@code range} of the
     * player, nearest first.
     */
    public static List<LivingEntity> around(Minecraft mc, double range, Filter filter) {
        List<LivingEntity> out = new ArrayList<>();
        LocalPlayer self = mc.player;
        if (self == null || mc.level == null) {
            return out;
        }
        Vec3 eye = self.getEyePosition();
        double rangeSqr = range * range;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !valid(mc, living, filter)) {
                continue;
            }
            if (living.getBoundingBox().getCenter().distanceToSqr(eye) <= rangeSqr) {
                out.add(living);
            }
        }
        out.sort((a, b) -> Double.compare(
                a.getBoundingBox().getCenter().distanceToSqr(eye),
                b.getBoundingBox().getCenter().distanceToSqr(eye)));
        return out;
    }

    /** The nearest match, or null. */
    public static LivingEntity nearest(Minecraft mc, double range, Filter filter) {
        List<LivingEntity> found = around(mc, range, filter);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Whether {@code entity} is something we are willing to point at.
     *
     * <p>Dead and spectating entities are excluded because attacking them is a
     * no-op the server will drop anyway, and drawing a box on them is noise.
     */
    public static boolean valid(Minecraft mc, Entity entity, Filter filter) {
        if (entity == null || entity == mc.player || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (!(entity instanceof LivingEntity living) || living.isDeadOrDying()) {
            return false;
        }
        boolean player = entity instanceof Player;
        return switch (filter) {
            case PLAYERS -> player;
            case MOBS -> !player;
            case ALL -> true;
        };
    }
}
