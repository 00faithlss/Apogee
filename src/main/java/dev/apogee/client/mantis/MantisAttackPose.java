package dev.apogee.client.mantis;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.mantis.MantisRules;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** One wrist-centred cut shared by vanilla hands and Punchy's native hand poses. */
public final class MantisAttackPose {
    private record Motion(float x, float y, float z, float pitch, float yaw, float roll) {
        private static final Motion REST = new Motion(0, 0, 0, 0, 0, 0);
    }

    private MantisAttackPose() {
    }

    private static float pulse(float time, float peak) {
        if (time < 0.0F || time >= 1.0F) {
            return 0.0F;
        }
        float phase = time < peak ? time / peak : (1.0F - time) / (1.0F - peak);
        return phase * phase * (3.0F - 2.0F * phase);
    }

    private static Motion motion(boolean right, float own, float other, float thrust) {
        float side = right ? 1.0F : -1.0F;
        if (thrust >= 0.0F) {
            float reach = pulse(thrust, 0.42F);
            return new Motion(-side * 0.48F * reach, 0.18F * reach, -0.85F * reach,
                    -14.0F * reach, side * 18.0F * reach, side * 14.0F * reach);
        }
        if (own >= 0.0F) {
            float wind = pulse(own / 0.42F, 0.48F);
            float cut = pulse((own - 0.18F) / 0.82F, 0.38F);
            return new Motion(side * (0.14F * wind - 0.95F * cut), 0.18F * wind + 0.12F * cut,
                    0.08F * wind - 0.60F * cut, 14.0F * wind - 20.0F * cut,
                    side * (-12.0F * wind + 38.0F * cut), side * (-16.0F * wind + 48.0F * cut));
        }
        if (other >= 0.0F) {
            float follow = pulse(other, 0.50F);
            return new Motion(-side * 0.26F * follow, 0.06F * follow, 0.04F * follow,
                    5.0F * follow, -side * 7.0F * follow, -side * 12.0F * follow);
        }
        return Motion.REST;
    }

    public static void apply(PoseStack poses, AbstractClientPlayer player, boolean right, float partialTick) {
        Motion motion = motion(right,
                MantisClient.slashProgress(player, right ? MantisRules.ARM_RIGHT : MantisRules.ARM_LEFT, partialTick),
                MantisClient.slashProgress(player, right ? MantisRules.ARM_LEFT : MantisRules.ARM_RIGHT, partialTick),
                MantisClient.thrustProgress(player, partialTick));
        if (motion.equals(Motion.REST)) {
            return;
        }
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        float centre = (right ? -1.0F : 1.0F) * (slim ? 0.5F : 1.0F) / 16.0F;
        // First-person model coordinates also pass through Minecraft's model-view matrix.
        // Its camera turn reverses X/Z; cutting in the raw arm coordinates sweeps outwards.
        Matrix4f nativePose = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poses.last().pose());
        Vector3f wrist = nativePose.transformPosition(new Vector3f(centre, 10.0F / 16.0F, 0.0F));
        Matrix4f cut = new Matrix4f().translation(motion.x(), motion.y(), motion.z())
                .translate(wrist).rotateXYZ(motion.pitch() * Mth.DEG_TO_RAD,
                        motion.yaw() * Mth.DEG_TO_RAD, motion.roll() * Mth.DEG_TO_RAD)
                .translate(-wrist.x(), -wrist.y(), -wrist.z());
        poses.mulPose(new Matrix4f(nativePose).invert().mul(cut).mul(nativePose));
    }
}
