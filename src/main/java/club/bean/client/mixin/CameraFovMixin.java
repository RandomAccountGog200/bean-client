package club.bean.client.mixin;

import club.bean.client.feature.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scales the camera FOV for hold-to-zoom. Client-side rendering only - the
 * server is never told what FOV you are using.
 */
@Mixin(Camera.class)
public class CameraFovMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void beanclient$applyZoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        float factor = Zoom.factor();
        if (Math.abs(factor - 1.0f) > 0.0005f) {
            cir.setReturnValue(cir.getReturnValueF() * factor);
        }
    }
}
