package dev.apogee.compat.immersivearmors;

import dev.apogee.Apogee;
import dev.apogee.network.ApogeeNetworking.SteamDashRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/**
 * Forward air dash for Immersive Armors' Steampunk Leggings, which already give a steam double
 * jump. Movement is applied on the client like the original thruster; the server only checks
 * the request and plays the steam burst for everyone else. Inactive without Immersive Armors.
 */
public final class SteamDash {
    public static final String MOD_ID = "immersive_armors";
    public static final ResourceLocation STEAMPUNK_LEGGINGS = ResourceLocation.fromNamespaceAndPath(MOD_ID, "steampunk_leggings");
    public static final double HORIZONTAL_SPEED = 1.05;
    public static final double MIN_VERTICAL_SPEED = 0.2;
    private static final int SERVER_MIN_INTERVAL_TICKS = 8;

    private static final Map<UUID, Long> LAST_DASH = new HashMap<>();

    private SteamDash() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean wearsSteampunkLeggings(Player player) {
        return STEAMPUNK_LEGGINGS.equals(BuiltInRegistries.ITEM.getKey(player.getItemBySlot(EquipmentSlot.LEGS).getItem()));
    }

    public static void registerServer() {
        if (!isAvailable()) {
            return;
        }
        ServerPlayNetworking.registerGlobalReceiver(SteamDashRequest.TYPE, (payload, context) -> onDash(context.player()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LAST_DASH.clear());
        Apogee.LOGGER.info("Immersive Armors detected: Steampunk Leggings air dash enabled");
    }

    private static void onDash(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        Long last = LAST_DASH.get(player.getUUID());
        if (!wearsSteampunkLeggings(player) || player.onGround() || (last != null && now - last < SERVER_MIN_INTERVAL_TICKS)) {
            return;
        }
        LAST_DASH.put(player.getUUID(), now);
        player.resetFallDistance();
        // The dashing client plays its own sound; everyone else hears it from here.
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 0.35F, 0.6F);
        player.serverLevel().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getY() + 0.6, player.getZ(), 8, 0.2, 0.3, 0.2, 0.01);
    }
}
