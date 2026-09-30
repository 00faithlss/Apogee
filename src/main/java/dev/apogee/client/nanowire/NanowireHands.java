package dev.apogee.client.nanowire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.mantis.MantisHandPose;
import dev.apogee.mantis.MantisRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * First-person view with the nanowire out: both of the player's own arms where vanilla holds a
 * hand, a little higher and closer in so the wire held between the fists crosses the lower view,
 * with the housings and wires on them.
 *
 * <p>After a Cyberpunk 2077 monowire clip, frame by frame: at rest both fists hold the wire taut
 * across the view; a lash winds the arm out to its side and sweeps it across the view close to
 * the camera in about 0.13 s, then brings it back; the two-wire lash pushes both arms forward
 * while the wires spin.
 */
public final class NanowireHands {
    /** How far below its place the pair of arms starts and ends while the wire deploys or retracts. */
    private static final float RISE = 0.65F;
    /** The guard: each fist this far in from vanilla's hand and up. */
    private static final float GUARD_IN = 0.10F;
    private static final float GUARD_UP = 0.16F;

    private NanowireHands() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, LocalPlayer player, int light, float partialTick) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        poseStack.pushPose();
        MantisHandPose.undoViewBob(poseStack, player, partialTick);
        // Vanilla's lag as the view turns: the hands trail a little behind the camera.
        float xBob = Mth.lerp(partialTick, player.xBobO, player.xBob);
        float yBob = Mth.lerp(partialTick, player.yBobO, player.yBob);
        poseStack.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - xBob) * 0.1F));
        poseStack.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - yBob) * 0.1F));
        NanowireWire.Frame frame = NanowireWire.Frame.of(poseStack.last(), false);
        float deploy = MantisClient.deployProgress(player, partialTick);
        float heavy = MantisClient.thrustProgress(player, partialTick);
        for (boolean right : new boolean[]{true, false}) {
            float side = right ? 1.0F : -1.0F;
            poseStack.pushPose();
            // Breathing and walking sway as the hands that take over when the wire is away move
            // (Punchy's, measured), and the hand those take over from.
            MantisHandPose.apply(poseStack, player, partialTick, right, deploy);
            if (right != (player.getMainArm() == HumanoidArm.RIGHT)) {
                // The other arm rises into view as the wire deploys and sinks as it retracts, so
                // with the wire away only the main hand is left, as in vanilla.
                poseStack.translate(0.0F, -RISE * (1.0F - deploy), 0.0F);
            }
            poseStack.translate(-side * GUARD_IN * deploy, GUARD_UP * deploy, 0.0F);
            float own = MantisClient.slashProgress(player, right ? MantisRules.ARM_RIGHT : MantisRules.ARM_LEFT, partialTick);
            float other = MantisClient.slashProgress(player, right ? MantisRules.ARM_LEFT : MantisRules.ARM_RIGHT, partialTick);
            if (own >= 0.0F) {
                lash(poseStack, own, side);
            } else if (other >= 0.0F) {
                // The other fist lets go of the wire and gives a little.
                float k = Mth.sin(other * Mth.PI) * 0.6F;
                poseStack.translate(side * 0.04F * k, -0.05F * k, 0.05F * k);
            }
            if (heavy >= 0.0F) {
                heavy(poseStack, heavy, side);
            }
            arm(poseStack, buffers, light, player, renderer, right);
            poseStack.popPose();
        }
        NanowireRig.drawWires(buffers, player, frame, partialTick);
        poseStack.popPose();
    }

    /**
     * One arm's lash: a short wind-up out to its own side, then a sweep across the view close to
     * the camera, easing back to the guard.
     */
    private static void lash(PoseStack poseStack, float u, float side) {
        float wind = u < 0.10F ? Mth.sin(u / 0.10F * Mth.HALF_PI) : 1.0F - NanowireMoves.ease((u - 0.10F) / 0.12F);
        float sweep = NanowireMoves.ease((u - 0.10F) / 0.22F) * (1.0F - NanowireMoves.ease((u - 0.36F) / 0.46F));
        poseStack.translate(side * (0.10F * wind - 0.62F * sweep), 0.06F * wind + 0.18F * sweep, 0.04F * wind - 0.22F * sweep);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 30.0F * sweep));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * (12.0F * wind - 16.0F * sweep)));
    }

    /**
     * Both arms in the two-wire lash: the right one reaches out towards the centre of the view and
     * circles as the wires spin, the left one draws back a little, as in the reference clip.
     */
    private static void heavy(PoseStack poseStack, float u, float side) {
        float reach = NanowireMoves.ease(u / 0.12F) * (1.0F - NanowireMoves.ease((u - 0.62F) / 0.30F));
        float spin = u * Mth.TWO_PI * 2.0F;
        if (side > 0.0F) {
            poseStack.translate(-0.30F * reach + 0.05F * Mth.sin(spin) * reach, (0.22F + 0.04F * Mth.cos(spin)) * reach, -0.45F * reach);
            poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F * reach));
            poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * reach));
        } else {
            poseStack.translate(-0.05F * reach, -0.04F * reach, 0.08F * reach);
        }
    }

    /** Vanilla's first-person arm placement (with no swing or equip offset), then the arm itself. */
    private static void arm(PoseStack poseStack, MultiBufferSource buffers, int light, LocalPlayer player, PlayerRenderer renderer,
                            boolean right) {
        float side = right ? 1.0F : -1.0F;
        poseStack.translate(side * 0.64000005F, -0.6F, -0.71999997F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        poseStack.translate(side * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        poseStack.translate(side * 5.6F, 0.0F, 0.0F);
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
    }
}
