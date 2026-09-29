package dev.apogee.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisRules;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the blades are out the first-person view shows both bare forearms and no held item:
 * the slashing arm plays the normal swing, and a lunge brings both arms forward together.
 * Low priority so this runs before other first-person animation mods and takes over the view.
 */
@Mixin(value = ItemInHandRenderer.class, priority = 500)
public abstract class MantisItemInHandRendererMixin {
    @Shadow
    private void renderPlayerArm(PoseStack poseStack, MultiBufferSource buffer, int light, float equippedProgress,
                                 float swingProgress, HumanoidArm arm) {
        throw new AssertionError();
    }

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void apogee$mantisHands(float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                    LocalPlayer player, int light, CallbackInfo ci) {
        if (!MantisClient.isDeployed(player) || player.isScoping()) {
            return;
        }
        ci.cancel();
        float slash = player.getAttackAnim(partialTick);
        int slashArm = MantisClient.swingArm(player);
        float thrust = MantisClient.thrustProgress(player, partialTick);
        for (HumanoidArm arm : new HumanoidArm[]{HumanoidArm.RIGHT, HumanoidArm.LEFT}) {
            boolean right = arm == HumanoidArm.RIGHT;
            float side = right ? 1.0F : -1.0F;
            float swing = (right ? MantisRules.ARM_RIGHT : MantisRules.ARM_LEFT) == slashArm && thrust < 0.0F ? slash : 0.0F;
            poseStack.pushPose();
            // Guard stance: both forearms come in towards the centre so the blades cross in front.
            float guard = MantisClient.deployProgress(player, partialTick);
            poseStack.translate(-side * 0.10F * guard, 0.04F * guard, -0.08F * guard);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 10.0F * guard));
            if (thrust >= 0.0F) {
                float lunge = Mth.sin(thrust * Mth.PI);
                poseStack.translate(-side * 0.30F * lunge, 0.12F * lunge, -0.60F * lunge);
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 16.0F * lunge));
            }
            renderPlayerArm(poseStack, buffers, light, 0.0F, swing, arm);
            poseStack.popPose();
        }
        buffers.endBatch();
    }
}
