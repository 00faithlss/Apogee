package dev.apogee.tendons;

import dev.apogee.network.ApogeeNetworking.DoubleJumpRequest;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import dev.apogee.item.ReinforcedTendonsItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Reinforced Tendons: one extra jump per time in the air. The client applies the impulse, as
 * with any movement; the server checks that the implant is worn and that the jump has not been
 * used since the last landing, then softens the fall and plays the effects for everyone else.
 */
public final class ReinforcedTendons {
    /** Upward speed of the extra jump (a normal jump is 0.42). */
    public static final double BOOST_Y = 0.68;
    /** Blocks of fall the extra jump absorbs. */
    public static final float FALL_MARGIN = 4.0F;

    private static final Set<UUID> USED = new HashSet<>();

    private ReinforcedTendons() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(DoubleJumpRequest.TYPE, (payload, context) -> perform(context.player()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (USED.isEmpty()) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
                    USED.remove(player.getUUID());
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> USED.clear());
    }

    public static boolean isEquipped(LivingEntity entity) {
        return TrinketsApi.getTrinketComponent(entity)
                .map(component -> component.isEquipped(stack -> stack.getItem() instanceof ReinforcedTendonsItem))
                .orElse(false);
    }

    /** Validates and applies a double-jump request; public so the gametests can send one. */
    public static void perform(ServerPlayer player) {
        if (!isEquipped(player) || !player.isAlive() || player.isSpectator() || player.onGround() || player.isPassenger()
                || player.isFallFlying() || player.getAbilities().flying || !USED.add(player.getUUID())) {
            return;
        }
        // A negative distance is a margin: the landing damage only counts what is left above 3 blocks.
        player.fallDistance = -FALL_MARGIN;
        // The jumping client plays its own sound; everyone else hears it from here.
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.25F, 1.8F);
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 8, 0.25, 0.05, 0.25, 0.03);
        player.serverLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.1, player.getZ(), 6, 0.2, 0.1, 0.2, 0.25);
    }
}
