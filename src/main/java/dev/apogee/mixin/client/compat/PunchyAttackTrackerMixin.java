package dev.apogee.mixin.client.compat;

import dev.apogee.client.mantis.MantisClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "punchy.client.state.AttackActionTracker", remap = false)
public abstract class PunchyAttackTrackerMixin {
    @Inject(method = "recordAttack*", at = @At("HEAD"), cancellable = true)
    private static void apogee$implantAttack(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && MantisClient.isOut(player)) {
            ci.cancel();
        }
    }
}
