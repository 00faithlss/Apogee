package dev.apogee.client.tooltip;

import dev.apogee.bloodpump.BloodPumpRules;
import dev.apogee.client.ApogeeClient;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Tooltip text for the Blood Pump, read from {@link BloodPumpRules} so it shows what it does. */
public final class BloodPumpTooltipContent {
    public record Stat(Component label, Component value, boolean separatedBefore) {
    }

    private BloodPumpTooltipContent() {
    }

    public static Component title() {
        return Component.translatable("item.apogee.blood_pump");
    }

    public static Component badge() {
        return Component.translatable("tooltip.apogee.blood_pump.badge");
    }

    public static Component descriptionHeader() {
        return Component.translatable("tooltip.apogee.blood_pump.description_header");
    }

    public static List<Component> descriptionLines() {
        return List.of(
                Component.translatable("tooltip.apogee.blood_pump.description.0"),
                Component.translatable("tooltip.apogee.blood_pump.description.1", ApogeeClient.ACTIVATE_BLOOD_PUMP.getTranslatedKeyMessage()));
    }

    public static Component statsHeader() {
        return Component.translatable("tooltip.apogee.blood_pump.stats_header");
    }

    public static List<Stat> stats() {
        return List.of(
                stat("instant", Component.translatable("tooltip.apogee.value.hearts", number(BloodPumpRules.INSTANT_HEAL / 2.0)), false),
                stat("regen", Component.translatable("tooltip.apogee.value.hearts", number(BloodPumpRules.REGEN_HEAL / 2.0)), false),
                stat("regen_time", seconds(BloodPumpRules.REGEN_TICKS), false),
                stat("cooldown", seconds(BloodPumpRules.COOLDOWN_TICKS), true));
    }

    public static Component note() {
        return Component.translatable("tooltip.apogee.blood_pump.note");
    }

    public static Component brand() {
        return Component.translatable("tooltip.apogee.brand");
    }

    /** Plain lines for when Simply Tooltips is not installed. */
    public static List<Component> plainLines() {
        return List.of(
                Component.translatable("tooltip.apogee.blood_pump.plain_stats",
                        number(BloodPumpRules.INSTANT_HEAL / 2.0), number(BloodPumpRules.REGEN_HEAL / 2.0),
                        number(BloodPumpRules.REGEN_TICKS / 20.0), number(BloodPumpRules.COOLDOWN_TICKS / 20.0)));
    }

    private static Stat stat(String key, Component value, boolean separated) {
        return new Stat(Component.translatable("tooltip.apogee.blood_pump.stat." + key), value, separated);
    }

    private static Component seconds(int ticks) {
        return Component.translatable("tooltip.apogee.value.seconds", number(ticks / 20.0));
    }

    private static String number(double value) {
        return new BigDecimal(Double.toString(value)).stripTrailingZeros().toPlainString();
    }
}
