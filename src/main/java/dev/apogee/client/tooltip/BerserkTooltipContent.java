package dev.apogee.client.tooltip;

import dev.apogee.berserk.BerserkVariant;
import dev.apogee.client.ApogeeClient;
import dev.apogee.client.ApogeeClientState;
import dev.apogee.config.BerserkRules;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Tooltip text for the Berserk, filled with the host's synced rules for each variant. */
public final class BerserkTooltipContent {
    public record Stat(Component label, Component value) {
    }

    private BerserkTooltipContent() {
    }

    public static Component badge(BerserkVariant variant) {
        return variant == BerserkVariant.MILITECH
                ? Component.translatable("tooltip.apogee.berserk.iconic")
                : Component.translatable("tooltip.apogee.berserk.badge");
    }

    public static List<Component> descriptionLines(BerserkVariant variant) {
        Component activate = ApogeeClient.ACTIVATE_SANDEVISTAN.getTranslatedKeyMessage();
        Component sneak = Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage();
        return List.of(
                Component.translatable("tooltip.apogee.berserk.description.0"),
                Component.translatable("tooltip.apogee.berserk.description." + variant.id()),
                Component.translatable("tooltip.apogee.berserk.description.1", activate, sneak));
    }

    public static Component statsHeader() {
        return Component.translatable("tooltip.apogee.berserk.stats_header");
    }

    public static List<Stat> stats(BerserkVariant variant) {
        BerserkRules rules = ApogeeClientState.berserkRules();
        BerserkRules.Stats stats = rules.stats(variant);
        List<Stat> out = new ArrayList<>();
        out.add(stat("duration", seconds(stats.durationTicks())));
        if (stats.invulnerable()) {
            out.add(stat("invulnerable", Component.translatable("tooltip.apogee.berserk.value.invulnerable")));
        } else if (stats.damageReductionPercent() > 0) {
            out.add(stat("reduction", Component.translatable("tooltip.apogee.value.minus_percent", stats.damageReductionPercent())));
        }
        if (!stats.invulnerable() && stats.healthFloorPercent() > 0) {
            out.add(stat("floor", Component.translatable("tooltip.apogee.value.percent", stats.healthFloorPercent())));
        }
        if (stats.attackSpeedPercent() > 0) {
            out.add(stat("attack_speed", Component.translatable("tooltip.apogee.value.bonus_percent", stats.attackSpeedPercent())));
        }
        if (stats.movementPercent() > 0) {
            out.add(stat("movement", Component.translatable("tooltip.apogee.value.bonus_percent", stats.movementPercent())));
        }
        if (stats.critChancePercent() > 0) {
            out.add(stat("crit", Component.translatable("tooltip.apogee.berserk.value.crit", stats.critChancePercent(), stats.critDamagePercent())));
        }
        if (stats.lowHealthDamagePercent() > 0) {
            out.add(stat("low_health", Component.translatable("tooltip.apogee.berserk.value.low_health", stats.lowHealthDamagePercent())));
        }
        if (stats.fallDamageReductionPercent() > 0) {
            out.add(stat("fall", Component.translatable("tooltip.apogee.value.minus_percent", stats.fallDamageReductionPercent())));
        }
        out.add(stat("hunger", Component.translatable("tooltip.apogee.berserk.value.hunger")));
        if (rules.slamRadius() > 0.0) {
            out.add(stat("slam", Component.translatable("tooltip.apogee.berserk.value.slam", number(rules.slamDamage()), number(rules.slamRadius()))));
        }
        out.add(stat("heal", Component.translatable("tooltip.apogee.berserk.value.heal", rules.healPerKillPercent())));
        out.add(stat("cooldown", seconds(stats.cooldownTicks())));
        return out;
    }

    public static Component cooldownNote() {
        return Component.translatable("tooltip.apogee.berserk.cooldown_note");
    }

    private static Stat stat(String key, Component value) {
        return new Stat(Component.translatable("tooltip.apogee.berserk.stat." + key), value);
    }

    private static Component seconds(int ticks) {
        return Component.translatable("tooltip.apogee.value.seconds", number(ticks / 20.0));
    }

    private static String number(double value) {
        return new BigDecimal(Double.toString(value)).stripTrailingZeros().toPlainString();
    }
}
