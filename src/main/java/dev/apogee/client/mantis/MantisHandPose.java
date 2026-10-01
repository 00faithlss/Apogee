package dev.apogee.client.mantis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.compat.punchy.PunchyCompat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class MantisHandPose {
    public static final float[] FOLDED = new float[6];
    public static boolean holdFolded;

    private MantisHandPose() {
    }

    public static void apply(PoseStack poses, LocalPlayer player, float partialTick, boolean right, float open) {
        float side = right ? 1.0F : -1.0F;
        float folded = 1.0F - open;
        poses.translate(side * FOLDED[0] * folded, FOLDED[1] * folded, FOLDED[2] * folded);
        poses.translate(side * 0.64F, -0.6F, -0.72F);
        poses.mulPose(Axis.YP.rotationDegrees(side * FOLDED[3] * folded));
        poses.mulPose(Axis.XP.rotationDegrees(FOLDED[4] * folded));
        poses.mulPose(Axis.ZP.rotationDegrees(side * FOLDED[5] * folded));
        poses.translate(-side * 0.64F, 0.6F, 0.72F);
    }

    public static void applyViewLag(PoseStack poses, LocalPlayer player, float partialTick) {
        if (PunchyCompat.rendering()) {
            return;
        }
        float xBob = Mth.lerp(partialTick, player.xBobO, player.xBob);
        float yBob = Mth.lerp(partialTick, player.yBobO, player.yBob);
        poses.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - xBob) * 0.1F));
        poses.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - yBob) * 0.1F));
    }
}
