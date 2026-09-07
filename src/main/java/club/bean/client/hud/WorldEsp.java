package club.bean.client.hud;

import club.bean.client.feature.BowAimbot;
import club.bean.client.feature.Combat;
import club.bean.client.feature.Targets;
import club.bean.client.feature.Trajectories;
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

    /**
     * Most entities drawn in one frame.
     *
     * <p>Without a cap, a mob farm or a crowded spawn puts several hundred
     * entities through projection and drawing every single frame, and the frame
     * rate goes with it. Entities are walked nearest-first, so the cap drops the
     * far ones - the ones least worth seeing.
     */
    private static final int MAX_DRAWN = 64;

    private WorldEsp() {
    }

    static void render(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme) {
        boolean esp = Settings.enabled("esp");
        boolean tracers = Settings.enabled("tracers");
        boolean tags = Settings.enabled("name_tags");
        boolean items = Settings.enabled("item_esp");
        boolean radar = Settings.enabled("player_radar");
        boolean path = Settings.enabled("trajectories");
        boolean targetHud = Settings.enabled("target_hud");

        if (!esp && !tracers && !tags && !items && !radar && !path && !targetHud) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        Projection projection = Projection.of(mc, gfx);
        if (projection == null) {
            return;
        }

        // One walk of the entity list, not one per module. Each of these used
        // to iterate everything the client is rendering independently.
        drawEntities(gfx, mc, font, theme, projection, esp, tracers, tags, items);
        if (radar) {
            drawRadar(gfx, mc, theme);
        }
        if (path) {
            drawTrajectory(gfx, mc, theme, projection);
        }
        if (targetHud) {
            drawTargetHud(gfx, mc, font, theme);
        }
    }

    // ---- entities ---------------------------------------------------------

    private static void drawEntities(GuiGraphicsExtractor gfx, Minecraft mc, Font font, Theme theme,
                                     Projection projection, boolean esp, boolean tracers,
                                     boolean tags, boolean items) {
        Targets.Filter espFilter = filter("esp", "Players");
        Targets.Filter tracerFilter = filter("tracers", "Players");
        Targets.Filter tagFilter = filter("name_tags", "All");

        double espRange = Settings.number("esp", "Range", 64);
        double tracerRange = Settings.number("tracers", "Range", 64);
        double tagRange = Settings.number("name_tags", "Range", 48);
        boolean fill = Settings.flag("esp", "Fill", false);

        double itemRange = Settings.number("item_esp", "Range", 32);

        Vec3 eye = mc.player.getEyePosition();
        LivingEntity aura = Combat.target();
        int drawn = 0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) {
                continue;
            }
            if (drawn >= MAX_DRAWN) {
                break;
            }
            if (items && entity instanceof ItemEntity item) {
                double itemDistance = Math.sqrt(entity.getBoundingBox().getCenter().distanceToSqr(eye));
                if (itemDistance <= itemRange && drawItem(gfx, font, theme, projection, item)) {
                    drawn++;
                }
                continue;
            }
            if (!(entity instanceof LivingEntity living)) {
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
            drawn++;

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
        // Draw.outline, not Draw.roundBorder: the box has no curved edge, so the
        // anti-aliased filler would burn a fill per pixel row producing the same
        // picture. This was the single most expensive thing ESP did.
        Draw.outline(gfx, x, y, w, h, colour);
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

    /** @return true if the item was actually on screen and drawn */
    private static boolean drawItem(GuiGraphicsExtractor gfx, Font font, Theme theme,
                                    Projection projection, ItemEntity item) {
        float[] point = projection.project(item.getBoundingBox().getCenter());
        if (point == null) {
            return false;
        }
        int count = item.getItem().getCount();
        String label = item.getItem().getHoverName().getString() + (count > 1 ? " x" + count : "");
        int width = font.width(label);

        Draw.roundRect(gfx, point[0] - width / 2.0 - 3, point[1] - 6, width + 6,
                font.lineHeight + 3, 2, Colours.withAlpha(theme.background, 180));
        Draw.text(gfx, font, label, (int) (point[0] - width / 2.0), (int) point[1] - 5, theme.accent);
        return true;
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

    // ---- trajectory --------------------------------------------------------

    /**
     * The predicted flight path, drawn as a chain of short segments.
     *
     * <p>Each pair of simulated points becomes one rotated bar. Points behind
     * the camera project to nothing and simply break the chain, which is the
     * right behaviour - a line drawn to a point behind you would sweep across
     * the screen.
     */
    private static void drawTrajectory(GuiGraphicsExtractor gfx, Minecraft mc, Theme theme,
                                       Projection projection) {
        Trajectories.Path path = Trajectories.predict(mc);
        if (path == null || path.points().size() < 2) {
            return;
        }
        int colour = switch (path.ending()) {
            case ENTITY -> HURT;
            case BLOCK -> theme.accent;
            default -> Colours.withAlpha(theme.textDim, 160);
        };

        float[] previous = null;
        for (Vec3 point : path.points()) {
            float[] screen = projection.project(point);
            if (screen == null) {
                previous = null;
                continue;
            }
            if (previous != null) {
                double dx = screen[0] - previous[0];
                double dy = screen[1] - previous[1];
                double length = Math.sqrt(dx * dx + dy * dy);
                if (length >= 0.5) {
                    Draw.bar(gfx, (previous[0] + screen[0]) / 2, (previous[1] + screen[1]) / 2,
                            length, 1.4, Math.toDegrees(Math.atan2(dy, dx)), colour);
                }
            }
            previous = screen;
        }
        // Mark where it lands.
        float[] end = projection.project(path.points().get(path.points().size() - 1));
        if (end != null) {
            Draw.ring(gfx, end[0], end[1], 4, 2.4, colour);
        }
    }

    // ---- target panel --------------------------------------------------------

    /**
     * A readout for whatever the combat modules are currently pointed at.
     *
     * <p>Deliberately reads their chosen target rather than picking one of its
     * own, so it shows what will actually be hit rather than a second opinion.
     */
    private static void drawTargetHud(GuiGraphicsExtractor gfx, Minecraft mc, Font font,
                                      Theme theme) {
        LivingEntity target = Combat.target();
        if (target == null) {
            target = BowAimbot.target();
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        String name = target.getName().getString();
        float health = target.getHealth();
        float max = Math.max(1f, target.getMaxHealth());
        String stats = String.format("%.0f / %.0f", Math.ceil(health), max);

        int width = Math.max(96, Math.max(font.width(name), font.width(stats)) + 20);
        int height = 40;
        int x = (gfx.guiWidth() - width) / 2;
        int y = gfx.guiHeight() / 2 + 30;

        Draw.roundRect(gfx, x, y, width, height, Math.max(2, theme.cornerRadius - 2),
                Colours.withAlpha(theme.background, 190));
        Draw.text(gfx, font, name, x + 8, y + 6, theme.text);
        Draw.textRight(gfx, font, stats, x + width - 8, y + 6, theme.textDim);

        // Health bar, tinted the same green-to-red as the ESP boxes.
        double fraction = Math.max(0, Math.min(1, health / max));
        Draw.roundRect(gfx, x + 8, y + 22, width - 16, 6, 3,
                Colours.withAlpha(theme.panelAlt, 220));
        if (fraction > 0) {
            Draw.roundRect(gfx, x + 8, y + 22, (width - 16) * fraction, 6, 3,
                    Colours.mix(HURT, HEALTHY, (float) fraction));
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
