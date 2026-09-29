package dev.apogee.mixin.client;

import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisRules;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third-person arm pose while the blades are out: guard stance, alternating slashes, two-arm lunge. */
@Mixin(PlayerModel.class)
public abstract class MantisPlayerModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void apogee$mantisPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (MantisClient.firstPersonRendering) {
            return;
        }
        float partial = ageInTicks - entity.tickCount;
        float deploy = MantisClient.deployProgress(entity, partial);
        if (deploy <= 0.0F) {
            return;
        }
        PlayerModel<T> model = (PlayerModel<T>) (Object) this;
        float slash = entity.getAttackAnim(partial);
        int slashArm = MantisClient.swingArm(entity);
        float thrust = MantisClient.thrustProgress(entity, partial);

        float rightX = Mth.lerp(deploy, model.rightArm.xRot, -0.55F);
        float leftX = Mth.lerp(deploy, model.leftArm.xRot, -0.55F);
        float rightY = Mth.lerp(deploy, model.rightArm.yRot, 0.10F);
        float leftY = Mth.lerp(deploy, model.leftArm.yRot, -0.10F);
        float rightZ = Mth.lerp(deploy, model.rightArm.zRot, 0.08F);
        float leftZ = Mth.lerp(deploy, model.leftArm.zRot, -0.08F);

        if (thrust >= 0.0F) {
            float lunge = Mth.sin(thrust * Mth.PI);
            rightX = Mth.lerp(lunge, rightX, -1.5F);
            leftX = Mth.lerp(lunge, leftX, -1.5F);
            rightY = Mth.lerp(lunge, rightY, -0.35F);
            leftY = Mth.lerp(lunge, leftY, 0.35F);
        } else if (slash > 0.0F) {
            float arc = Mth.sin(slash * Mth.PI);
            if (slashArm == MantisRules.ARM_RIGHT) {
                rightX -= 0.9F * arc;
                rightY -= 0.8F * Mth.sin(slash * Mth.PI * 2.0F) * 0.5F + 0.5F * arc;
            } else {
                leftX -= 0.9F * arc;
                leftY += 0.8F * Mth.sin(slash * Mth.PI * 2.0F) * 0.5F + 0.5F * arc;
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
