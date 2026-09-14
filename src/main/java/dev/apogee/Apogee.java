package dev.apogee;

import dev.apogee.compat.immersivearmors.SteamDash;
import dev.apogee.config.ApogeeServerConfig;
import dev.apogee.network.ApogeeNetworking;
import dev.apogee.registry.ApogeeAttachments;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.registry.ApogeeSounds;
import dev.apogee.sandevistan.SandevistanManager;
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
        ApogeeItems.register();
        ApogeeSounds.register();
        ApogeeAttachments.register();
        ApogeeNetworking.registerCommon();
        SandevistanManager.register();
        SteamDash.registerServer();
    }
}
