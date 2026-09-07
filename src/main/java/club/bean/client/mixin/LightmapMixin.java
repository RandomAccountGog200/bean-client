package club.bean.client.mixin;

import club.bean.client.feature.Fullbright;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Raises the ambient floor of the lightmap when Fullbright is on.
 *
 * Runs after vanilla has built the lightmap and only widens the ambient term -
 * no geometry, visibility or culling decision is touched, so it cannot reveal a
 * block the client was not already given.
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void beanclient$fullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (!Fullbright.isActive()) {
            return;
        }
        float level = Fullbright.ambientLevel();
        if (level <= 0.0f) {
            return;
        }
        Vector3fc ambient = state.ambientColor;
        state.ambientColor = new Vector3f(
                Math.max(ambient.x(), level),
                Math.max(ambient.y(), level),
                Math.max(ambient.z(), level));
        state.brightness = Math.max(state.brightness, 1.0f);
    }
}
