package dev.apogee.client.ripperdoc;

import dev.apogee.ripperdoc.RipperdocSeat;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Final player pose aligned with the couch's hinge and backrest. */
public final class RipperdocPatientPose {
    private RipperdocPatientPose() {
    }

    public static boolean resting(LivingEntity player) {
        return player.getVehicle() instanceof RipperdocSeat || RipperdocScreen.renderingPatient;
    }

    public static void apply(PlayerModel<?> model, LivingEntity player, float headPitch) {
        if (RipperdocScreen.renderingPatient || !(player.getVehicle() instanceof RipperdocSeat seat)) {
            return;
        }
        for (ModelPart part : new ModelPart[]{model.body, model.head, model.rightArm, model.leftArm,
                model.rightLeg, model.leftLeg}) {
            part.loadPose(part.getInitialPose());
        }
        model.head.yRot = Mth.clamp(Mth.wrapDegrees(player.getYHeadRot() - seat.getYRot()), -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        model.head.xRot = Mth.clamp(headPitch, -30.0F, 45.0F) * Mth.DEG_TO_RAD;
        model.rightLeg.xRot = model.leftLeg.xRot = -RipperdocSeat.BACKREST * Mth.DEG_TO_RAD;
        model.rightLeg.zRot = 0.03F;
        model.leftLeg.zRot = -0.03F;
        model.rightArm.zRot = 0.08F;
        model.leftArm.zRot = -0.08F;
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }
}
