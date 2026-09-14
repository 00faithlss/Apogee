package dev.apogee.mixin;

import dev.apogee.sandevistan.SandevistanManager;
import dev.apogee.time.TimeScaledEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    /**
     * Slowed entities skip whole ticks. Players are ticked through their connection instead
     * (see PlayerTickMixin) so menus and networking stay responsive.
     */
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void apogee$scaleEntityTick(Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer) {
            return;
        }
        TimeScaledEntity scaled = TimeScaledEntity.of(entity);
        if (scaled.apogee$isSlowed() && !scaled.apogee$advanceTime(SandevistanManager.rules().timeScale())) {
            // Hit invulnerability still counts down in real time so hits on slowed targets land normally.
            if (entity instanceof LivingEntity living && living.invulnerableTime > 0) {
                living.invulnerableTime--;
            }
            ci.cancel();
        }
    }

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void apogee$tagProjectile(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        SandevistanManager.onEntitySpawn(entity);
    }
}
