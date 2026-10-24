package dev.apogee.armor;

import dev.apogee.Apogee;
import dev.apogee.registry.ApogeeItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Buffos de Trauma Team: Regeneración II con el conjunto completo, +10 % de velocidad con los pantalones
 * y 10 bloques de caída segura con las botas.
 */
public final class TraumaBuffs {
    private static final ResourceLocation SPEED_ID = Apogee.id("trauma_team_leggings_speed");
    private static final ResourceLocation FALL_ID = Apogee.id("trauma_team_boots_fall");
    private static final double SPEED_BONUS = 0.10;
    // La caída segura de base son 3 bloques; con +7 el daño empieza a los 10.
    private static final double SAFE_FALL_BONUS = 7.0;
    private static final int REGENERATION_TICKS = 40;
    private static final int REGENERATION_AMPLIFIER = 1;

    private TraumaBuffs() {}

    public static boolean isFullSet(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ApogeeItems.TRAUMA_TEAM_HELMET)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ApogeeItems.TRAUMA_TEAM_CHESTPLATE)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ApogeeItems.TRAUMA_TEAM_LEGGINGS)
                && player.getItemBySlot(EquipmentSlot.FEET).is(ApogeeItems.TRAUMA_TEAM_BOOTS);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                MaxtacBuffs.syncModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, SPEED_BONUS,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                        player.getItemBySlot(EquipmentSlot.LEGS).is(ApogeeItems.TRAUMA_TEAM_LEGGINGS));
                MaxtacBuffs.syncModifier(player, Attributes.SAFE_FALL_DISTANCE, FALL_ID, SAFE_FALL_BONUS,
                        AttributeModifier.Operation.ADD_VALUE,
                        player.getItemBySlot(EquipmentSlot.FEET).is(ApogeeItems.TRAUMA_TEAM_BOOTS));
                if (player.isAlive() && isFullSet(player)) {
                    MobEffectInstance current = player.getEffect(MobEffects.REGENERATION);
                    if (current == null || current.getAmplifier() < REGENERATION_AMPLIFIER || current.getDuration() <= 10) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS,
                                REGENERATION_AMPLIFIER, true, false, true));
                    }
                }
            }
        });
    }
}
