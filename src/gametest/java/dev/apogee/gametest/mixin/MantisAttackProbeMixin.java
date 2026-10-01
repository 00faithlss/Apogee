package dev.apogee.gametest.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.client.mantis.MantisAttackPose;
import dev.apogee.client.mantis.MantisClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MantisAttackPose.class, remap = false)
public abstract class MantisAttackProbeMixin {
    @Unique private static Vector4f apogee$before;
    @Unique private static long apogee$logged = Long.MIN_VALUE;

    @Inject(method = "apply", at = @At("HEAD"))
    private static void apogee$before(PoseStack poses, AbstractClientPlayer player, boolean right, float partial, CallbackInfo ci) {
        apogee$before = null;
        if (!Boolean.getBoolean("apogee.mantisAttackSmoke")) return;
        float own = MantisClient.slashProgress(player, right ? 0 : 1, partial);
        if (own > 0.4F && own < 0.6F && apogee$logged != MantisClient.clientTicks()) {
            apogee$before = apogee$wrist(poses, player, right);
        }
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private static void apogee$after(PoseStack poses, AbstractClientPlayer player, boolean right, float partial, CallbackInfo ci) {
        if (apogee$before == null) return;
        Vector4f after = apogee$wrist(poses, player, right);
        Vector4f beforeScreen = RenderSystem.getProjectionMatrix().transform(new Vector4f(apogee$before));
        Vector4f afterScreen = RenderSystem.getProjectionMatrix().transform(new Vector4f(after));
        if (!(Math.abs(afterScreen.x / afterScreen.w) < Math.abs(beforeScreen.x / beforeScreen.w))) {
            throw new IllegalStateException("The attacking wrist must enter towards the crosshair");
        }
        LoggerFactory.getLogger("MantisAttackProbe").info("{} cut enters: |screenX| {} -> {}", right ? "right" : "left",
                Math.abs(beforeScreen.x / beforeScreen.w), Math.abs(afterScreen.x / afterScreen.w));
        apogee$logged = MantisClient.clientTicks();
        apogee$before = null;
    }
    @Unique
    private static Vector4f apogee$wrist(PoseStack poses, AbstractClientPlayer player, boolean right) {
        float centre = (right ? -1.0F : 1.0F) * (player.getSkin().model() == PlayerSkin.Model.SLIM ? 0.5F : 1.0F) / 16.0F;
        return new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poses.last().pose())
                .transform(new Vector4f(centre, 10.0F / 16.0F, 0.0F, 1.0F));
    }
}
