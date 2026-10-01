package dev.apogee.mixin.client;

import dev.apogee.client.ripperdoc.RipperdocScreen;
import dev.apogee.ripperdoc.RipperdocSeat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Removes vanilla's riding bend before the couch or surgery viewport supplies its own pose. */
@Mixin(HumanoidModel.class)
public abstract class RipperdocPatientModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void apogee$notRiding(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  CallbackInfo ci) {
        if (RipperdocScreen.renderingPatient || entity.getVehicle() instanceof RipperdocSeat) {
            ((HumanoidModel<?>) (Object) this).riding = false;
        }
    }

}
