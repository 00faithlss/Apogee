package dev.apogee.mixin.client;

import dev.apogee.client.ripperdoc.RipperdocScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The patient shown in the ripperdoc screen stands, even while they lie on the couch. */
@Mixin(HumanoidModel.class)
public abstract class RipperdocPatientModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void apogee$standInView(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                    CallbackInfo ci) {
        if (RipperdocScreen.renderingPatient) {
            ((HumanoidModel<?>) (Object) this).riding = false;
        }
    }
}
