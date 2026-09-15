package dev.apogee.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.apogee.client.fx.SandevistanEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /**
     * Chains with other FOV mods (e.g. Cinematic Zoom). A cancellable @Inject at RETURN would skip
     * their injectors at the same point and break their zoom.
     */
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private double apogee$sandevistanFov(double fov, Camera camera, float partialTick, boolean useFovSetting) {
        return useFovSetting ? SandevistanEffects.modifyFov(fov, partialTick) : fov;
    }
}
