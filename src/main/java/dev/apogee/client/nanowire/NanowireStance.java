package dev.apogee.client.nanowire;

import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisRules;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Third-person arm pose of a nanowire wearer with the wire out: both hands up in front of the
 * chest, a little in towards each other, holding the wire between them. A lash sweeps the arm
 * that strikes across the body (the right one to the left, the left one to the right); the
 * two-wire lash raises both arms and sweeps them apart.
 */
public final class NanowireStance {
    private static final float GUARD_X = -1.20F;
    private static final float GUARD_Y = 0.35F;

    private NanowireStance() {
    }

    public static void apply(PlayerModel<?> model, LivingEntity entity, float partialTick) {
        if (MantisClient.firstPersonRendering) {
            return;
        }
        float deploy = MantisClient.deployProgress(entity, partialTick);
        if (deploy <= 0.0F) {
            return;
        }
        float rightX = Mth.lerp(deploy, model.rightArm.xRot, GUARD_X);
        float leftX = Mth.lerp(deploy, model.leftArm.xRot, GUARD_X);
        float rightY = Mth.lerp(deploy, model.rightArm.yRot, -GUARD_Y);
        float leftY = Mth.lerp(deploy, model.leftArm.yRot, GUARD_Y);
        float rightZ = Mth.lerp(deploy, model.rightArm.zRot, 0.0F);
        float leftZ = Mth.lerp(deploy, model.leftArm.zRot, 0.0F);

        float right = MantisClient.slashProgress(entity, MantisRules.ARM_RIGHT, partialTick);
        if (right >= 0.0F) {
            float sweep = lash(right);
            rightX = Mth.lerp(Mth.sin(right * Mth.PI), rightX, -1.65F);
            rightY = Mth.lerp(Math.abs(sweep), rightY, sweep > 0.0F ? 0.55F : -1.10F);
        }
        float left = MantisClient.slashProgress(entity, MantisRules.ARM_LEFT, partialTick);
        if (left >= 0.0F) {
            float sweep = lash(left);
            leftX = Mth.lerp(Mth.sin(left * Mth.PI), leftX, -1.65F);
            leftY = Mth.lerp(Math.abs(sweep), leftY, sweep > 0.0F ? -0.55F : 1.10F);
        }
        float heavy = MantisClient.thrustProgress(entity, partialTick);
        if (heavy >= 0.0F) {
            float raise = Mth.sin(heavy * Mth.PI);
            rightX = Mth.lerp(raise, rightX, -2.3F);
            leftX = Mth.lerp(raise, leftX, -2.3F);
            rightZ = Mth.lerp(raise, rightZ, 0.6F);
            leftZ = Mth.lerp(raise, leftZ, -0.6F);
        }
        model.rightArm.xRot = rightX;
        model.rightArm.yRot = rightY;
        model.rightArm.zRot = rightZ;
        model.leftArm.xRot = leftX;
        model.leftArm.yRot = leftY;
        model.leftArm.zRot = leftZ;
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
    }

    /**
     * Sideways sweep of a lash over its progress: a short wind-up out to the arm's own side
     * (positive), then a fast sweep across the body (negative) that eases back to the guard.
     */
    private static float lash(float t) {
        if (t < 0.2F) {
            return Mth.sin(t / 0.2F * Mth.HALF_PI) * 0.6F;
        }
        if (t < 0.45F) {
            float k = (t - 0.2F) / 0.25F;
            return Mth.lerp(k * k * (3.0F - 2.0F * k), 0.6F, -1.0F);
        }
        float k = (t - 0.45F) / 0.55F;
        return Mth.lerp(k * k * (3.0F - 2.0F * k), -1.0F, 0.0F);
    }
}
