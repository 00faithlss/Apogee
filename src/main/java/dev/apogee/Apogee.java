package dev.apogee;

import dev.apogee.bloodpump.BloodPumpManager;
import dev.apogee.compat.immersivearmors.SteamDash;
import dev.apogee.berserk.BerserkManager;
import dev.apogee.config.ApogeeServerConfig;
import dev.apogee.mantis.MantisManager;
import dev.apogee.network.ApogeeNetworking;
import dev.apogee.registry.ApogeeAttachments;
import dev.apogee.registry.ApogeeBlocks;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.registry.ApogeeEffects;
import dev.apogee.registry.ApogeeSounds;
import dev.apogee.sandevistan.SandevistanManager;
import dev.apogee.tendons.ReinforcedTendons;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Apogee implements ModInitializer {
    public static final String MOD_ID = "apogee";
    public static final Logger LOGGER = LoggerFactory.getLogger("Apogee");

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ApogeeServerConfig.load();
        ApogeeBlocks.register();
        ApogeeItems.register();
        ApogeeEffects.register();
        ApogeeSounds.register();
        ApogeeAttachments.register();
        ApogeeNetworking.registerCommon();
        SandevistanManager.register();
        BerserkManager.register();
        MantisManager.register();
        SteamDash.registerServer();
        ReinforcedTendons.register();
        BloodPumpManager.register();
    }
}
