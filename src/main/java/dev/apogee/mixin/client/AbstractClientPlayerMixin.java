package dev.apogee.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.apogee.sandevistan.SandevistanManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    /** The Sandevistan speed bonus must not widen the FOV; most players already play at max FOV. */
    @ModifyExpressionValue(method = "getFieldOfViewModifier", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double apogee$ignoreSandevistanSpeed(double speed) {
        AttributeInstance attribute = ((AbstractClientPlayer) (Object) this).getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeModifier modifier = attribute == null ? null : attribute.getModifier(SandevistanManager.SPEED_MODIFIER);
        return modifier == null ? speed : speed / (1.0 + modifier.amount());
    }
}
