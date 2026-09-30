package dev.apogee.mixin.client;

import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.mantis.MantisRig;
import dev.apogee.client.nanowire.NanowireStance;
import dev.apogee.mantis.MantisVariant;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the vanilla arms of Mantis Blade wearers and poses them for combat while the blades are out. */
@Mixin(PlayerModel.class)
public abstract class MantisPlayerModelMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void apogee$mantisPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, CallbackInfo ci) {
        MantisVariant variant = MantisClient.variant(entity);
        if (variant == null) {
            return;
        }
        if (!variant.isBlades()) {
            NanowireStance.apply((PlayerModel<?>) (Object) this, entity, ageInTicks - entity.tickCount);
            return;
        }
        PlayerModel<T> model = (PlayerModel<T>) (Object) this;
        // The implant model rebuilds the whole arm (see MantisArmsLayer).
        model.rightArm.visible = false;
        model.leftArm.visible = false;
        model.rightSleeve.visible = false;
        model.leftSleeve.visible = false;
        if (MantisClient.firstPersonRendering) {
            return;
        }
        float partial = ageInTicks - entity.tickCount;
        float deploy = MantisClient.deployProgress(entity, partial);
        if (deploy <= 0.0F) {
            return;
        }
        float thrust = MantisClient.thrustProgress(entity, partial);

        // Combat stance: forearms raised, arms held out from the torso so the implant's lower
        // covers clear the body. The cut itself is the arm_root track of the attack clips.
        float rightX = Mth.lerp(deploy, model.rightArm.xRot, -0.55F);
        float leftX = Mth.lerp(deploy, model.leftArm.xRot, -0.55F);
        float rightY = Mth.lerp(deploy, model.rightArm.yRot, 0.10F);
        float leftY = Mth.lerp(deploy, model.leftArm.yRot, -0.10F);
        float rightZ = Mth.lerp(deploy, model.rightArm.zRot, MantisRig.STANCE_ABDUCTION);
        float leftZ = Mth.lerp(deploy, model.leftArm.zRot, -MantisRig.STANCE_ABDUCTION);
        if (thrust >= 0.0F) {
            float lunge = Mth.sin(thrust * Mth.PI);
            rightX = Mth.lerp(lunge, rightX, -1.5F);
            leftX = Mth.lerp(lunge, leftX, -1.5F);
            rightY = Mth.lerp(lunge, rightY, -0.35F);
            leftY = Mth.lerp(lunge, leftY, 0.35F);
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
