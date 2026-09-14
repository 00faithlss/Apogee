package dev.apogee.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.apogee.Apogee;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * config/apogee-server.json, read by whichever side hosts the world (integrated server for
 * Essential/LAN hosts, or a dedicated server). Values are expressed in seconds for humans.
 */
public final class ApogeeServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "apogee-server.json";
    private static SandevistanRules rules = SandevistanRules.DEFAULTS;

    private ApogeeServerConfig() {
    }

    public static SandevistanRules rules() {
        return rules;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        FileFormat format = new FileFormat();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                FileFormat read = GSON.fromJson(reader, FileFormat.class);
                if (read != null) {
                    format = read;
                }
            } catch (IOException | RuntimeException exception) {
                Apogee.LOGGER.warn("Could not read {}, using defaults", FILE_NAME, exception);
            }
        }
        rules = format.toRules().sanitized();
        FileFormat normalized = FileFormat.from(rules);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(normalized, writer);
            }
        } catch (IOException exception) {
            Apogee.LOGGER.warn("Could not write {}", FILE_NAME, exception);
        }
    }

    @SuppressWarnings("unused")
    private static final class FileFormat {
        int slowdownPercent = SandevistanRules.DEFAULTS.slowdownPercent();
        double fieldRadiusBlocks = SandevistanRules.DEFAULTS.fieldRadius();
        double durationSeconds = SandevistanRules.DEFAULTS.durationTicks() / 20.0;
        int movementBonusPercent = SandevistanRules.DEFAULTS.movementBonusPercent();
        int damageBonusPercent = SandevistanRules.DEFAULTS.damageBonusPercent();
        double killExtensionSeconds = SandevistanRules.DEFAULTS.killExtensionTicks() / 20.0;
        double maxDurationSeconds = SandevistanRules.DEFAULTS.maxDurationTicks() / 20.0;
        double cooldownSeconds = SandevistanRules.DEFAULTS.cooldownTicks() / 20.0;
        double combatMemorySeconds = SandevistanRules.DEFAULTS.combatMemoryTicks() / 20.0;
        double movementRampSeconds = SandevistanRules.DEFAULTS.movementRampTicks() / 20.0;
        int slowedKnockbackPercent = SandevistanRules.DEFAULTS.slowedKnockbackPercent();

        SandevistanRules toRules() {
            return new SandevistanRules(
                    slowdownPercent,
                    fieldRadiusBlocks,
                    ticks(durationSeconds),
                    movementBonusPercent,
                    damageBonusPercent,
                    ticks(killExtensionSeconds),
                    ticks(maxDurationSeconds),
                    ticks(cooldownSeconds),
                    ticks(combatMemorySeconds),
                    ticks(movementRampSeconds),
                    slowedKnockbackPercent
            );
        }

        static FileFormat from(SandevistanRules rules) {
            FileFormat format = new FileFormat();
            format.slowdownPercent = rules.slowdownPercent();
            format.fieldRadiusBlocks = rules.fieldRadius();
            format.durationSeconds = rules.durationTicks() / 20.0;
            format.movementBonusPercent = rules.movementBonusPercent();
            format.damageBonusPercent = rules.damageBonusPercent();
            format.killExtensionSeconds = rules.killExtensionTicks() / 20.0;
            format.maxDurationSeconds = rules.maxDurationTicks() / 20.0;
            format.cooldownSeconds = rules.cooldownTicks() / 20.0;
            format.combatMemorySeconds = rules.combatMemoryTicks() / 20.0;
            format.movementRampSeconds = rules.movementRampTicks() / 20.0;
            format.slowedKnockbackPercent = rules.slowedKnockbackPercent();
            return format;
        }

        private static int ticks(double seconds) {
            return (int) Math.round(seconds * 20.0);
        }
    }
}
