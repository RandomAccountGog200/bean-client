package club.bean.client.hud;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * Turns a world position into a point on the HUD.
 *
 * <p>This is what lets the Render modules exist at all in a client with no
 * mixins. Minecraft 26.2 replaced the old world-render hooks with a
 * submit-node pipeline, and Fabric API no longer ships a "draw in the world"
 * event - so rather than fight the render path, ESP works out where each entity
 * would appear on screen and draws a flat box there, through exactly the same
 * {@link club.bean.client.gui.Draw} filler the GUI window uses.
 *
 * <p>The projection is rebuilt once per frame and then asked about many points,
 * which is why it is an object rather than a static method: the expensive parts
 * - the camera basis and the field-of-view half-extents - are shared by every
 * query.
 *
 * <h2>The maths</h2>
 *
 * <p>The camera hands out three orthonormal vectors, so putting a world point
 * into camera space is three dot products rather than a matrix multiply. From
 * there it is the standard perspective divide.
 *
 * <p>The one number that has to be right is the field of view.
 * {@code Camera.getFov()} is the <em>vertical</em> FOV in degrees - vanilla
 * multiplies it by pi/180 on the way into {@code Matrix4f.setPerspective} - and
 * the aspect ratio is the framebuffer's, not the GUI's. Take either from the
 * wrong place and every box is subtly the wrong size, in a way that only shows
 * up at the edges of the screen.
 */
public final class Projection {
    /** Points closer than this to the camera plane are treated as behind it. */
    private static final double NEAR = 0.05;

    /**
     * How far outside the screen a projected point is allowed to land, as a
     * multiple of the screen size.
     *
     * <p>This is not cosmetic. The perspective divide blows up as depth
     * approaches {@link #NEAR}: an entity standing on top of the camera has
     * corners at depth 0.06 that project tens of thousands of pixels away. The
     * anti-aliased filler in {@code Draw} walks one scanline per pixel row, so
     * an unclamped box like that asks it to iterate a hundred thousand rows and
     * the game stalls for seconds - which reads as a random freeze rather than
     * as a rendering bug. Clamping costs nothing and bounds the work.
     */
    private static final double OVERSCAN = 2.0;

    private final Vec3 origin;
    private final Vector3fc forward;
    private final Vector3fc up;
    private final Vector3fc left;
    private final double halfWidth;
    private final double halfHeight;
    private final int guiWidth;
    private final int guiHeight;

    private Projection(Camera camera, double halfWidth, double halfHeight,
                       int guiWidth, int guiHeight) {
        this.origin = camera.position();
        this.forward = camera.forwardVector();
        this.up = camera.upVector();
        this.left = camera.leftVector();
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
    }

    /** @return a projection for this frame, or null if the camera is not ready */
    public static Projection of(Minecraft mc, GuiGraphicsExtractor gfx) {
        Camera camera = mc.gameRenderer == null ? null : mc.gameRenderer.mainCamera();
        if (camera == null || !camera.isInitialized()) {
            return null;
        }
        Window window = mc.getWindow();
        if (window == null || window.getHeight() <= 0 || window.getWidth() <= 0) {
            return null;
        }
        double halfHeight = Math.tan(Math.toRadians(camera.getFov()) / 2.0);
        double aspect = (double) window.getWidth() / window.getHeight();
        return new Projection(camera, halfHeight * aspect, halfHeight,
                gfx.guiWidth(), gfx.guiHeight());
    }

    /**
     * Projects one world point.
     *
     * @return {@code {x, y}} in GUI coordinates, or null when the point is
     *         behind the camera - which callers must handle, because a point
     *         behind you still has a mathematically valid but nonsensical
     *         projection that lands mirrored on the far side of the screen
     */
    public float[] project(Vec3 world) {
        double dx = world.x - origin.x;
        double dy = world.y - origin.y;
        double dz = world.z - origin.z;

        double depth = dx * forward.x() + dy * forward.y() + dz * forward.z();
        if (depth < NEAR) {
            return null;
        }
        double vertical = dx * up.x() + dy * up.y() + dz * up.z();
        // The camera gives a left vector; screen x grows to the right.
        double horizontal = -(dx * left.x() + dy * left.y() + dz * left.z());

        double ndcX = horizontal / depth / halfWidth;
        double ndcY = vertical / depth / halfHeight;
        return new float[] {
                clamp((ndcX + 1) * 0.5 * guiWidth, guiWidth),
                clamp((1 - ndcY) * 0.5 * guiHeight, guiHeight)
        };
    }

    /** Keeps a coordinate within {@link #OVERSCAN} screens of the viewport. */
    private static float clamp(double value, int extent) {
        double slack = extent * OVERSCAN;
        if (Double.isNaN(value)) {
            return 0;
        }
        return (float) Math.max(-slack, Math.min(extent + slack, value));
    }

    /**
     * The screen-space rectangle enclosing a world-space box.
     *
     * <p>Projects all eight corners and takes their bounds, which is what makes
     * the box tighten as you look at an entity side-on instead of staying a
     * fixed size. Corners behind the camera are dropped rather than clamped -
     * for a box you are standing inside the result is partial, but the
     * alternative is a rectangle that wraps around the screen.
     *
     * @return {@code {minX, minY, maxX, maxY}}, or null if nothing projected
     */
    public float[] projectBox(AABB box) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        boolean any = false;

        for (int corner = 0; corner < 8; corner++) {
            // Each bit of the counter picks the low or high side of one axis.
            double x = (corner & 1) == 0 ? box.minX : box.maxX;
            double y = (corner & 2) == 0 ? box.minY : box.maxY;
            double z = (corner & 4) == 0 ? box.minZ : box.maxZ;

            float[] point = project(new Vec3(x, y, z));
            if (point == null) {
                continue;
            }
            any = true;
            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
        }
        return any ? new float[] { minX, minY, maxX, maxY } : null;
    }

    public int guiWidth() {
        return guiWidth;
    }

    public int guiHeight() {
        return guiHeight;
    }
}
