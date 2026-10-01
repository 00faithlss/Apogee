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

        float heavy = MantisClient.thrustProgress(entity, partialTick);
        int style = MantisClient.heavyStyle(entity);
        for (boolean right : new boolean[]{true, false}) {
            float side = right ? 1.0F : -1.0F;
            float own = MantisClient.slashProgress(entity, right ? MantisRules.ARM_RIGHT : MantisRules.ARM_LEFT, partialTick);
            float other = MantisClient.slashProgress(entity, right ? MantisRules.ARM_LEFT : MantisRules.ARM_RIGHT, partialTick);
            float x = right ? rightX : leftX;
            float y = right ? rightY : leftY;
            float z = right ? rightZ : leftZ;
            if (heavy >= 0.0F) {
                NanowireMoves.Motion motion = NanowireMoves.motion(NanowireMoves.heavyTime(side, style, heavy), true);
                float wind = motion.wind();
                float strike = motion.strike();
                float follow = motion.follow();
                // Raise over the shoulder, then pull down to the hip instead of reaching out.
                x += -1.65F * wind + 0.70F * strike + 0.10F * follow;
                if (style == NanowireMoves.HEAVY_X) {
                    y += side * (-0.15F * wind + 0.80F * strike);
                    z += side * (0.35F * wind - 0.18F * strike);
                } else if (style == NanowireMoves.HEAVY_VERTICAL) {
                    y += side * GUARD_Y * (wind + strike);
                } else {
                    y += side * (-0.12F * wind + 0.45F * strike);
                    z += side * (0.20F * wind - 0.25F * strike);
                }
            } else if (own >= 0.0F) {
                NanowireMoves.Motion motion = NanowireMoves.motion(own, false);
                // Continuous signed offsets: crossing zero never switches the target angle.
                x += -0.25F * motion.wind() - 0.45F * motion.strike() + 0.25F * motion.follow();
                y += side * (-0.45F * motion.wind() + 1.30F * motion.strike() - 0.20F * motion.follow());
                z += side * (0.18F * motion.wind() - 0.30F * motion.strike() + 0.16F * motion.follow());
            } else if (other >= 0.0F) {
                float give = NanowireMoves.release(other, false);
                x += 0.12F * give;
                y -= side * 0.10F * give;
            }
            if (right) {
                rightX = x;
                rightY = y;
                rightZ = z;
            } else {
                leftX = x;
                leftY = y;
                leftZ = z;
            }
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
}
