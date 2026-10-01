package dev.apogee.mixin.client;

import dev.apogee.client.ripperdoc.RipperdocScreen;
import dev.apogee.ripperdoc.RipperdocSeat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A patient on the ripperdoc couch is posed lying down rather than riding: the legs straight and
 * bent forward by the backrest's tilt, so they lie flat along the cushion while the body leans on
 * the head end (see {@link RipperdocPatientMixin}). In the ripperdoc screen's view they stand.
 */
@Mixin(HumanoidModel.class)
public abstract class RipperdocPatientModelMixin<T extends LivingEntity> {
    @Shadow
    @Final
    public ModelPart rightLeg;

    @Shadow
    @Final
    public ModelPart leftLeg;

    @Shadow
    @Final
    public ModelPart rightArm;

    @Shadow
    @Final
    public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void apogee$notRiding(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  CallbackInfo ci) {
        if (RipperdocScreen.renderingPatient || entity.getVehicle() instanceof RipperdocSeat) {
            ((HumanoidModel<?>) (Object) this).riding = false;
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void apogee$lieDown(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                CallbackInfo ci) {
        if (RipperdocScreen.renderingPatient || !(entity.getVehicle() instanceof RipperdocSeat)) {
            return;
        }
        float bend = -RipperdocSeat.BACKREST * Mth.DEG_TO_RAD;
        rightLeg.xRot = bend;
        leftLeg.xRot = bend;
        rightLeg.yRot = leftLeg.yRot = 0.0F;
        rightLeg.zRot = 0.03F;
        leftLeg.zRot = -0.03F;
        // Arms resting along the sides.
        rightArm.xRot = leftArm.xRot = 0.0F;
        rightArm.yRot = leftArm.yRot = 0.0F;
        rightArm.zRot = 0.08F;
        leftArm.zRot = -0.08F;
    }
}
