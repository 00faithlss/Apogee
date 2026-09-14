package dev.apogee.mixin;

import dev.apogee.sandevistan.SandevistanManager;
import dev.apogee.time.TimeScaledEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTickMixin {
    /**
     * ServerPlayer#doTick calls super.tick() for the living/player simulation (attack cooldown,
     * item use, effects). Slowed server players skip it at the field's time scale; the rest of
     * doTick (menus, health sync) keeps running every tick. Client players are scaled in
     * ClientLevelMixin instead.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void apogee$scalePlayerTick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer)) {
            return;
        }
        TimeScaledEntity scaled = TimeScaledEntity.of(self);
        if (scaled.apogee$isSlowed() && !scaled.apogee$advanceTime(SandevistanManager.rules().timeScale())) {
            if (self.invulnerableTime > 0) {
                self.invulnerableTime--;
            }
            ci.cancel();
        }
    }
}
