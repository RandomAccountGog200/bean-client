package club.bean.client.hud;

import club.bean.client.feature.Combat;
import club.bean.client.feature.Targets;
import club.bean.client.gui.Draw;
import club.bean.client.module.Settings;
import club.bean.client.theme.Colours;
import club.bean.client.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The Render tab: ESP, Tracers, Name Tags, Item ESP and the radar.
 *
 * <p>Every one of these draws in two dimensions. There is no world-space
 * rendering here and no mixin - {@link Projection} works out where a thing
 * would appear on screen, and the result goes through the same
 * {@link Draw} filler that draws the GUI window, so the boxes are
 * anti-aliased and pick up the active theme for free.
 *
 * <p>The honest description of what these do: they draw things the vanilla
 * client already knows and had decided not to show you. An entity only appears
 * here if the server has already sent it - render distance, and whatever the
 * server chooses not to transmit, still apply, and nothing here asks for more
 * than the client was given. What it removes is the wall in front of it.
 *
 * <p>One loop covers ESP, Tracers and Name Tags because all three want the same
 * projected box, and projecting eight corners three times per entity is the one
 * cost worth avoiding here.
 */
public final class WorldEsp {
    private static final int HEALTHY = 0xFF4ADE80;
    private static final int HURT = 0xFFE05555;
    /** Killaura's current pick, so you can see what it chose. */
    private static final int TARGETED = 0xFFFFC53D;

    /** Boxes below this many pixels are a dot, and just add noise. */
    private static final double MIN_BOX = 2.0;

    private WorldEsp() {
    }

    static void render(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme) {
        boolean esp = Settings.enabled("esp");
        boolean tracers = Settings.enabled("tracers");
        boolean tags = Settings.enabled("name_tags");
        boolean items = Settings.enabled("item_esp");
        boolean radar = Settings.enabled("player_radar");

        if (!esp && !tracers && !tags && !items && !radar) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        Projection projection = Projection.of(mc, gfx);
        if (projection == null) {
            return;
        }

        if (esp || tracers || tags) {
            drawEntities(gfx, mc, font, theme, projection, esp, tracers, tags);
        }
        if (items) {
            drawItems(gfx, mc, font, theme, projection);
        }
        if (radar) {
            drawRadar(gfx, mc, theme);
        }
    }

    // ---- entities ---------------------------------------------------------

    private static void drawEntities(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme,
                                     Projection projection, boolean esp, boolean tracers, boolean tags) {
        Targets.Filter espFilter = filter("esp", "Players");
        Targets.Filter tracerFilter = filter("tracers", "Players");
        Targets.Filter tagFilter = filter("name_tags", "All");

        double espRange = Settings.number("esp", "Range", 64);
        double tracerRange = Settings.number("tracers", "Range", 64);
        double tagRange = Settings.number("name_tags", "Range", 48);
        boolean fill = Settings.flag("esp", "Fill", false);

        Vec3 eye = mc.player.getEyePosition();
        LivingEntity aura = Combat.target();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == mc.player) {
                continue;
            }
            double distance = Math.sqrt(entity.getBoundingBox().getCenter().distanceToSqr(eye));

            boolean wantEsp = esp && distance <= espRange && Targets.valid(mc, living, espFilter);
            boolean wantTracer = tracers && distance <= tracerRange && Targets.valid(mc, living, tracerFilter);
            boolean wantTag = tags && distance <= tagRange && Targets.valid(mc, living, tagFilter);
            if (!wantEsp && !wantTracer && !wantTag) {
                continue;
            }

            // Projected once, used by all three.
            float[] box = projection.projectBox(living.getBoundingBox());
            if (box == null || box[2] - box[0] < MIN_BOX || box[3] - box[1] < MIN_BOX) {
                continue;
            }
            int colour = living == aura ? TARGETED : colourFor(theme, living);

            if (wantEsp) {
                outline(gfx, box, colour, fill);
            }
            if (wantTracer) {
                tracer(gfx, projection, box, colour);
            }
            if (wantTag) {
                nameTag(gfx, font, theme, box, living, distance, colour);
            }
        }
    }

    private static void outline(GuiGraphicsExtractor gfx, float[] box, int colour, boolean fill) {
        double x = box[0];
        double y = box[1];
        double w = box[2] - box[0];
        double h = box[3] - box[1];
        if (fill) {
            Draw.roundRect(gfx, x, y, w, h, 2, Colours.withAlpha(colour, 45));
        }
        Draw.roundBorder(gfx, x, y, w, h, 2, 1, colour);
    }

    /**
     * A line from the bottom middle of the screen to the entity's feet.
     *
     * <p>Anchoring at the bottom rather than the crosshair keeps the lines out
     * of the middle of the screen, where they would sit on top of whatever you
     * are actually aiming at.
     */
    private static void tracer(GuiGraphicsExtractor gfx, Projection projection, float[] box, int colour) {
        double fromX = projection.guiWidth() / 2.0;
        double fromY = projection.guiHeight();
        double toX = (box[0] + box[2]) / 2.0;
        double toY = box[3];

        double dx = toX - fromX;
        double dy = toY - fromY;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1) {
            return;
        }
        Draw.bar(gfx, (fromX + toX) / 2, (fromY + toY) / 2, length, 1.0,
                Math.toDegrees(Math.atan2(dy, dx)), Colours.withAlpha(colour, 170));
    }

    private static void nameTag(GuiGraphicsExtractor gfx, Font font, Theme theme, float[] box,
                                LivingEntity living, double distance, int colour) {
        StringBuilder label = new StringBuilder(living.getName().getString());
        if (Settings.flag("name_tags", "Show health", true)) {
            label.append("  ").append((int) Math.ceil(living.getHealth()));
        }
        if (Settings.flag("name_tags", "Show distance", true)) {
            label.append("  ").append((int) distance).append('m');
        }
        String text = label.toString();

        int width = font.width(text);
        int x = (int) ((box[0] + box[2]) / 2 - width / 2.0);
        int y = (int) (box[1] - font.lineHeight - 3);

        Draw.roundRect(gfx, x - 3, y - 2, width + 6, font.lineHeight + 3, 2,
                Colours.withAlpha(theme.background, 190));
        Draw.text(gfx, font, text, x, y, colour);
    }

    // ---- items ------------------------------------------------------------

    private static void drawItems(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme,
                                  Projection projection) {
        double range = Settings.number("item_esp", "Range", 32);
        Vec3 eye = mc.player.getEyePosition();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item)) {
                continue;
            }
            if (Math.sqrt(entity.getBoundingBox().getCenter().distanceToSqr(eye)) > range) {
                continue;
            }
            float[] point = projection.project(entity.getBoundingBox().getCenter());
            if (point == null) {
                continue;
            }
            int count = item.getItem().getCount();
            String label = item.getItem().getHoverName().getString() + (count > 1 ? " x" + count : "");
            int width = font.width(label);

            Draw.roundRect(gfx, point[0] - width / 2.0 - 3, point[1] - 6, width + 6,
                    font.lineHeight + 3, 2, Colours.withAlpha(theme.background, 180));
            Draw.text(gfx, font, label, (int) (point[0] - width / 2.0), (int) point[1] - 5, theme.accent);
        }
    }

    // ---- radar ------------------------------------------------------------

    /**
     * A top-down radar, rotated so your facing is always up.
     *
     * <p>Sits bottom-right, which it shares with the Effects HUD - run both and
     * they will overlap.
     */
    private static void drawRadar(GuiGraphicsExtractor gfx, Minecraft mc, Theme theme) {
        int size = Settings.integer("player_radar", "Size", 100);
        double range = Settings.number("player_radar", "Range", 64);
        Targets.Filter filter = filter("player_radar", "Players");

        int x = gfx.guiWidth() - size - 6;
        int y = gfx.guiHeight() - size - 6;
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;
        double reach = size / 2.0 - 4;

        Draw.roundRect(gfx, x, y, size, size, Math.max(2, theme.cornerRadius - 2),
                Colours.withAlpha(theme.background, 165));
        Draw.roundBorder(gfx, x, y, size, size, Math.max(2, theme.cornerRadius - 2), 1,
                Colours.withAlpha(theme.accent, 110));
        // Crosshair through the middle, so "in front" is readable at a glance.
        Draw.bar(gfx, cx, cy, size - 10, 0.6, 0, Colours.withAlpha(theme.textDim, 70));
        Draw.bar(gfx, cx, cy, size - 10, 0.6, 90, Colours.withAlpha(theme.textDim, 70));
        Draw.circle(gfx, cx, cy, 2.2, theme.accent);

        double yaw = Math.toRadians(mc.player.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);

        for (LivingEntity living : Targets.around(mc, range, filter)) {
            double dx = living.getX() - mc.player.getX();
            double dz = living.getZ() - mc.player.getZ();
            // Rotate world space into "forward is up". At yaw 0 the player faces
            // +z, so forward maps to +rz, which is then drawn as negative screen y.
            double rx = dx * cos + dz * sin;
            double rz = dz * cos - dx * sin;

            double px = cx + rx / range * reach;
            double py = cy - rz / range * reach;
            Draw.circle(gfx, px, py, 1.8, colourFor(theme, living));
        }
    }

    // ---- shared -----------------------------------------------------------

    private static Targets.Filter filter(String moduleId, String fallback) {
        return Targets.Filter.parse(Settings.mode(moduleId, "Targets", fallback));
    }

    /**
     * Green through red by remaining health for players, dimmed theme text for
     * everything else - so a hurt player stands out from a full one without
     * needing a health bar.
     */
    private static int colourFor(Theme theme, LivingEntity living) {
        if (!(living instanceof Player)) {
            return Colours.withAlpha(theme.text, 200);
        }
        float max = Math.max(1f, living.getMaxHealth());
        float fraction = Math.max(0f, Math.min(1f, living.getHealth() / max));
        return Colours.mix(HURT, HEALTHY, fraction);
    }
}
