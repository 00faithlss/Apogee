package dev.apogee.client.nanowire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;

/**
 * First-person view with the nanowire out: both of the player's own arms, posed by the pack's
 * two-arm rig and framed as its first-person camera would see them, with the housings and wires
 * on them. Every clip starts and ends in the rig's rest pose, so moves chain without blending.
 */
public final class NanowireHands {
    /**
     * Where the eye sits in the rig's space, in pixels: centred between the arms, above and
     * behind their pivots, so the hands come up from the bottom corners of the view.
     */
    static float eyeX = 0.0F;
    static float eyeY = 5.0F;
    static float eyeZ = 13.0F;
    /** How far below its place the pair of arms starts and ends while the wire deploys or retracts, in blocks. */
    private static final float RISE = 0.6F;

    private NanowireHands() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, LocalPlayer player, int light, float partialTick) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        float deploy = MantisClient.deployProgress(player, partialTick);
        poseStack.pushPose();
        // Vanilla's lag as the view turns: the hands trail a little behind the camera.
        float xBob = Mth.lerp(partialTick, player.xBobO, player.xBob);
        float yBob = Mth.lerp(partialTick, player.yBobO, player.yBob);
        poseStack.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - xBob) * 0.1F));
        poseStack.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - yBob) * 0.1F));
        poseStack.translate(0.0F, -RISE * (1.0F - deploy), 0.0F);
        poseStack.translate(-eyeX / 16.0F, -eyeY / 16.0F, -eyeZ / 16.0F);
        String clip = clip(player, partialTick);
        float seconds = seconds(player, clip, partialTick);
        for (boolean right : new boolean[]{true, false}) {
            poseStack.pushPose();
            NanowireArmMotion.apply(poseStack, clip, seconds, right);
            // From the player model's space (pixels as blocks / 16, +Y down the hanging arm) to the
            // rig's: a half turn about Z brings +Y up and the right arm to +X, a quarter turn about X
            // points the arms forward, and the hand ends land on the rig's fingertips.
            poseStack.translate(0.0F, 0.0F, 7.5F / 16.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            // Keeps the third-person stance off the arm model while it is posed here.
            MantisClient.firstPersonRendering = true;
            try {
                if (right) {
                    renderer.renderRightHand(poseStack, buffers, light, player);
                } else {
                    renderer.renderLeftHand(poseStack, buffers, light, player);
                }
            } finally {
                MantisClient.firstPersonRendering = false;
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** The rig clip for the wearer's current move. */
    private static String clip(LocalPlayer player, float partialTick) {
        if (MantisClient.thrustProgress(player, partialTick) >= 0.0F) {
            return "heavy";
        }
        if (MantisClient.slashProgress(player, MantisRules.ARM_RIGHT, partialTick) >= 0.0F) {
            return "attack_right";
        }
        if (MantisClient.slashProgress(player, MantisRules.ARM_LEFT, partialTick) >= 0.0F) {
            return "attack_left";
        }
        float deploy = MantisClient.deployProgress(player, partialTick);
        if (deploy < 1.0F) {
            return MantisClient.isDeployed(player) ? "deploy" : "retract";
        }
        return "idle";
    }

    private static float seconds(LocalPlayer player, String clip, float partialTick) {
        float length = NanowireArmMotion.length(clip);
        return switch (clip) {
            case "heavy" -> MantisClient.thrustProgress(player, partialTick) * length;
            case "attack_right" -> MantisClient.slashProgress(player, MantisRules.ARM_RIGHT, partialTick) * length;
            case "attack_left" -> MantisClient.slashProgress(player, MantisRules.ARM_LEFT, partialTick) * length;
            case "deploy" -> MantisClient.deployProgress(player, partialTick) * length;
            case "retract" -> (1.0F - MantisClient.deployProgress(player, partialTick)) * length;
            default -> ((MantisClient.clientTicks() + partialTick) / 20.0F) % length;
        };
    }
}
