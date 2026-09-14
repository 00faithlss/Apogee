package dev.apogee.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.apogee.Apogee;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** config/apogee-client.json: visual and audio preferences local to each player's client. */
public final class ApogeeClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "apogee-client.json";
    private static ApogeeClientConfig instance = new ApogeeClientConfig();

    public boolean afterimages = true;
    public int afterimageSilhouettes = 28;
    public double afterimageRangeBlocks = 48.0;
    public double afterimageFadeSeconds = 5.2;
    public double afterimageSpacingBlocks = 0.45;
    public float afterimageOpacity = 0.55F;
    public double afterimageFirstPersonMinDistance = 1.75;
    public float soundVolume = 1.0F;
    public boolean screenTint = true;
    public float screenTintOpacity = 0.28F;
    public boolean radialTransition = true;
    public boolean fovEffect = true;
    public float fovPulseStrength = 0.12F;
    public boolean hud = true;

    public static ApogeeClientConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        ApogeeClientConfig config = new ApogeeClientConfig();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                ApogeeClientConfig read = GSON.fromJson(reader, ApogeeClientConfig.class);
                if (read != null) {
                    config = read;
                }
            } catch (IOException | RuntimeException exception) {
                Apogee.LOGGER.warn("Could not read {}, using defaults", FILE_NAME, exception);
            }
        }
        config.sanitize();
        instance = config;
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            Apogee.LOGGER.warn("Could not write {}", FILE_NAME, exception);
        }
    }

    private void sanitize() {
        afterimageSilhouettes = Math.max(0, Math.min(128, afterimageSilhouettes));
        afterimageRangeBlocks = Math.max(0.0, Math.min(256.0, afterimageRangeBlocks));
        afterimageFadeSeconds = Math.max(0.1, Math.min(30.0, afterimageFadeSeconds));
        afterimageSpacingBlocks = Math.max(0.0, Math.min(8.0, afterimageSpacingBlocks));
        afterimageOpacity = clamp01(afterimageOpacity);
        afterimageFirstPersonMinDistance = Math.max(0.0, Math.min(16.0, afterimageFirstPersonMinDistance));
        soundVolume = Math.max(0.0F, Math.min(2.0F, soundVolume));
        screenTintOpacity = clamp01(screenTintOpacity);
        fovPulseStrength = Math.max(0.0F, Math.min(0.5F, fovPulseStrength));
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
