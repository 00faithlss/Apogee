package dev.apogee.tendons;

import dev.apogee.network.ApogeeNetworking.AirDashRequest;
import dev.apogee.network.ApogeeNetworking.DoubleJumpRequest;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    /** Blocks of fall the extra jump absorbs (the 3 free blocks of vanilla come on top). */
    public static final float FALL_MARGIN = 4.0F;

    /** Horizontal speed of the air dash, in any direction. */
    public static final double DASH_SPEED = 1.05;
    /** The dash never leaves you falling: it lifts to at least this upward speed. */
    public static final double DASH_MIN_VERTICAL = 0.2;

    private static final Set<UUID> USED = new HashSet<>();
    private static final Set<UUID> DASHED = new HashSet<>();
    /** Blocks of fall each airborne player can still absorb, from double jumps this flight. */
    private static final Map<UUID, Float> MARGIN = new HashMap<>();

    private ReinforcedTendons() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(DoubleJumpRequest.TYPE, (payload, context) -> perform(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(AirDashRequest.TYPE, (payload, context) -> performDash(context.player()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (USED.isEmpty() && DASHED.isEmpty() && MARGIN.isEmpty()) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
                    USED.remove(player.getUUID());
                    DASHED.remove(player.getUUID());
                    MARGIN.remove(player.getUUID());
                } else {
                    absorbFall(player);
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            USED.clear();
            DASHED.clear();
            MARGIN.clear();
        });
    }

    /** Blocks of fall the player can still absorb this flight. */
    public static float fallMargin(ServerPlayer player) {
        return MARGIN.getOrDefault(player.getUUID(), 0.0F);
    }

    /**
     * Spends the margin on the fall built up so far, so the landing only counts what is left.
     * Vanilla clamps a negative fall distance back to zero, so the margin is kept apart and
     * drained here every tick while in the air.
     */
    public static void absorbFall(ServerPlayer player) {
        float margin = fallMargin(player);
        if (margin <= 0.0F || player.fallDistance <= 0.0F) {
            return;
        }
        float absorbed = Math.min(margin, player.fallDistance);
        player.fallDistance -= absorbed;
        MARGIN.put(player.getUUID(), margin - absorbed);
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
        MARGIN.merge(player.getUUID(), FALL_MARGIN, Float::sum);
        // The jumping client plays its own sound; everyone else hears it from here.
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.25F, 1.8F);
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 8, 0.25, 0.05, 0.25, 0.03);
        player.serverLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.1, player.getZ(), 6, 0.2, 0.1, 0.2, 0.25);
    }

    /** Validates and applies an air dash request; public so the gametests can send one. */
    public static void performDash(ServerPlayer player) {
        if (!isEquipped(player) || !player.isAlive() || player.isSpectator() || player.onGround() || player.isPassenger()
                || player.isFallFlying() || player.getAbilities().flying || !DASHED.add(player.getUUID())) {
            return;
        }
        // The double jump's margin stays; only the fall already built up is dropped.
        player.fallDistance = 0.0F;
        // The dashing client plays its own sound; everyone else hears it from here.
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 0.35F, 0.6F);
        player.serverLevel().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getY() + 0.6, player.getZ(), 8, 0.2, 0.3, 0.2, 0.01);
    }
}
