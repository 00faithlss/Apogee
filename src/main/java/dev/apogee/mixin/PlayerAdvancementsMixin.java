package dev.apogee.mixin;

import dev.apogee.item.SandevistanItem;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    /** Runs once, on the tick an advancement is completed (where vanilla hands out its rewards). */
    @Inject(method = "award", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void apogee$onCompleted(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (advancement.id().equals(SandevistanItem.OBTAINED_ADVANCEMENT)) {
            SandevistanItem.playVoiceLine(player);
        }
    }
}
