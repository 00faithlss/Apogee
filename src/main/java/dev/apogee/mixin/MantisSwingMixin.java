package dev.apogee.mixin;

import dev.apogee.mantis.MantisManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MantisSwingMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
    private void apogee$alternateMantisArm(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            MantisManager.onSwing(player);
        }
    }
}
