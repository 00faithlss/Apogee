package dev.apogee.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisVariant;
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
 * While the blades are out the first-person view shows both implants and no held item; the
 * attack clips animate the cut, and a lunge brings both arms forward together.
 * Low priority so this runs before other first-person animation mods and takes over the view.
 */
@Mixin(value = ItemInHandRenderer.class, priority = 500)
public abstract class MantisItemInHandRendererMixin {
    /** Extra sideways offset of each forearm while the blades are out, in screen units. */
    private static final float ARM_SPREAD = 0.22F;

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
        float thrust = MantisClient.thrustProgress(player, partialTick);
        for (HumanoidArm arm : new HumanoidArm[]{HumanoidArm.RIGHT, HumanoidArm.LEFT}) {
            boolean right = arm == HumanoidArm.RIGHT;
            float side = right ? 1.0F : -1.0F;
            poseStack.pushPose();
            // Spread the forearms out towards the screen edges, level with where vanilla holds
            // an item in each hand; the implants' blades then rise clear of the view's centre.
            float guard = MantisClient.deployProgress(player, partialTick);
            poseStack.translate(side * ARM_SPREAD * guard, 0.0F, 0.0F);
            if (!right && MantisClient.variant(player) == MantisVariant.THERMAL) {
                // More of the left implant faces the camera, without resizing the skin hand.
                poseStack.translate(0.10F * guard, 0.12F * guard, 0.10F * guard);
                poseStack.mulPose(Axis.YP.rotationDegrees(-12.0F * guard));
                poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F * guard));
            }
            if (thrust >= 0.0F) {
                float lunge = Mth.sin(thrust * Mth.PI);
                poseStack.translate(-side * 0.30F * lunge, 0.12F * lunge, -0.60F * lunge);
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 16.0F * lunge));
            }
            // The cut is the arm_root track of the implant's attack clip, not the vanilla swing.
            renderPlayerArm(poseStack, buffers, light, 0.0F, 0.0F, arm);
            poseStack.popPose();
        }
        buffers.endBatch();
    }
}
