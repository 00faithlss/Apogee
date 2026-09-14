package dev.apogee.client.tooltip;

import dev.apogee.client.ApogeeClientState;
import dev.apogee.config.SandevistanRules;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Tooltip text for the Sandevistan, filled with the host's synced rules. */
public final class SandevistanTooltipContent {
    public record Stat(Component label, Component value, boolean separatedBefore) {
    }

    private SandevistanTooltipContent() {
    }

    public static Component title() {
        return Component.translatable("item.apogee.sandevistan");
    }

    public static Component badge() {
        return Component.translatable("tooltip.apogee.sandevistan.badge");
    }

    public static Component descriptionHeader() {
        return Component.translatable("tooltip.apogee.sandevistan.description_header");
    }

    public static List<Component> descriptionLines() {
        return List.of(
                Component.translatable("tooltip.apogee.sandevistan.description.0"),
                Component.translatable("tooltip.apogee.sandevistan.description.1"));
    }

    public static Component statsHeader() {
        return Component.translatable("tooltip.apogee.sandevistan.stats_header");
    }

    public static List<Stat> stats() {
        SandevistanRules rules = ApogeeClientState.rules();
        return List.of(
                stat("slowdown", Component.translatable("tooltip.apogee.value.percent", rules.slowdownPercent()), false),
                stat("radius", Component.translatable("tooltip.apogee.value.blocks", number(rules.fieldRadius())), false),
                stat("duration", seconds(rules.durationTicks()), false),
                stat("movement", Component.translatable("tooltip.apogee.value.bonus_percent", rules.movementBonusPercent()), false),
                stat("damage", Component.translatable("tooltip.apogee.value.bonus_percent", rules.damageBonusPercent()), false),
                stat("per_kill", Component.translatable("tooltip.apogee.value.bonus_seconds", number(rules.killExtensionTicks() / 20.0)), false),
                stat("max_duration", seconds(rules.maxDurationTicks()), false),
                stat("cooldown", seconds(rules.cooldownTicks()), true));
    }

    public static Component cooldownNote() {
        return Component.translatable("tooltip.apogee.sandevistan.cooldown_note");
    }

    public static Component brand() {
        return Component.translatable("tooltip.apogee.brand");
    }

    private static Stat stat(String key, Component value, boolean separated) {
        return new Stat(Component.translatable("tooltip.apogee.sandevistan.stat." + key), value, separated);
    }

    private static Component seconds(int ticks) {
        return Component.translatable("tooltip.apogee.value.seconds", number(ticks / 20.0));
    }

    private static String number(double value) {
        return new BigDecimal(Double.toString(value)).stripTrailingZeros().toPlainString();
    }
}
