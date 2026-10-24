package dev.apogee.armor;

import dev.apogee.Apogee;
import dev.apogee.registry.ApogeeItems;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Buffos de MaxTac: visión nocturna con los ópticos activos, Resistencia II con el conjunto completo,
 * +10 % de velocidad con los pantalones y 10 bloques de caída segura con las botas.
 */
public final class MaxtacBuffs {
    // Sobre 200 ticks restantes el efecto no parpadea en el cliente.
    private static final int NIGHT_VISION_TICKS = 260;
    private static final int REFRESH_BELOW = 230;
    private static final int RESISTANCE_TICKS = 40;
    private static final int RESISTANCE_AMPLIFIER = 1;

    private static final ResourceLocation SPEED_ID = Apogee.id("maxtac_leggings_speed");
    private static final ResourceLocation FALL_ID = Apogee.id("maxtac_boots_fall");
    private static final double SPEED_BONUS = 0.10;
    // La caída segura de base son 3 bloques; con +7 el daño empieza a los 10.
    private static final double SAFE_FALL_BONUS = 7.0;

    private static final Set<UUID> NIGHT_VISION_GRANTED = new HashSet<>();

    private MaxtacBuffs() {}

    public static boolean isFullSet(ServerPlayer player) {
        return MaxtacVisor.isHelmet(player.getItemBySlot(EquipmentSlot.HEAD))
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ApogeeItems.MAXTAC_CHESTPLATE)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ApogeeItems.MAXTAC_LEGGINGS)
                && player.getItemBySlot(EquipmentSlot.FEET).is(ApogeeItems.MAXTAC_BOOTS);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickNightVision(player);
                syncModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, SPEED_BONUS,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                        player.getItemBySlot(EquipmentSlot.LEGS).is(ApogeeItems.MAXTAC_LEGGINGS));
                syncModifier(player, Attributes.SAFE_FALL_DISTANCE, FALL_ID, SAFE_FALL_BONUS,
                        AttributeModifier.Operation.ADD_VALUE,
                        player.getItemBySlot(EquipmentSlot.FEET).is(ApogeeItems.MAXTAC_BOOTS));
                if (player.isAlive() && isFullSet(player)) {
                    refresh(player, MobEffects.DAMAGE_RESISTANCE, RESISTANCE_TICKS, RESISTANCE_AMPLIFIER, 10);
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> NIGHT_VISION_GRANTED.clear());
    }

    static void syncModifier(ServerPlayer player, net.minecraft.core.Holder<Attribute> attribute,
                                     ResourceLocation id, double amount, AttributeModifier.Operation operation,
                                     boolean wanted) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        boolean present = instance.hasModifier(id);
        if (wanted && !present) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        } else if (!wanted && present) {
            instance.removeModifier(id);
        }
    }

    private static void tickNightVision(ServerPlayer player) {
        UUID id = player.getUUID();
        if (MaxtacVisor.auxiliaryActive(player)) {
            NIGHT_VISION_GRANTED.add(id);
            refresh(player, MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0, REFRESH_BELOW);
        } else if (NIGHT_VISION_GRANTED.remove(id)) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static void refresh(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                int duration, int amplifier, int refreshBelow) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() <= refreshBelow) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
        }
    }
}
