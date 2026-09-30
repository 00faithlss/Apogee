package dev.apogee.client.tooltip;

import dev.apogee.client.ApogeeClient;
import dev.apogee.mantis.MantisRules;
import dev.apogee.mantis.MantisVariant;
import dev.apogee.mantis.NanowireRules;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Tooltip text for the arm cyberware (the Mantis Blades and the nanowire), read from
 * {@link MantisRules} and {@link NanowireRules} so the tooltip always shows what it actually does. Laid out like a Cyberpunk 2077 weapon card: damage per hit and
 * attacks per second up top, then the controls and the variant's ability (in the manner of
 * Simply Swords' unique weapons), then the detailed numbers.
 */
public final class MantisTooltipContent {
    /** A named block of text: a control ("Left click") or the variant's ability. */
    public record Section(Component header, List<Component> lines) {
    }

    public record Stat(Component label, Component value, boolean separatedBefore) {
    }

    private MantisTooltipContent() {
    }

    public static Component badge() {
        return Component.translatable("tooltip.apogee.mantis_blades.badge");
    }

    public static Component damagePerHit(MantisVariant variant) {
        return Component.literal(number(variant == MantisVariant.NANOWIRE ? NanowireRules.ATTACK_DAMAGE : MantisRules.ATTACK_DAMAGE));
    }

    public static Component damagePerHitLabel() {
        return Component.translatable("tooltip.apogee.mantis_blades.damage_per_hit");
    }

    public static Component attacksPerSecond(MantisVariant variant) {
        return Component.literal(number(variant == MantisVariant.NANOWIRE ? NanowireRules.ATTACK_SPEED : MantisRules.ATTACK_SPEED));
    }

    public static Component attacksPerSecondLabel() {
        return Component.translatable("tooltip.apogee.mantis_blades.attacks_per_second");
    }

    /** Physical, thermal or electrical, as Cyberpunk names its damage types. */
    public static Component damageType(MantisVariant variant) {
        return Component.translatable("tooltip.apogee.mantis_blades.damage_type." + key(variant));
    }

    public static Component damageTypeLabel() {
        return Component.translatable("tooltip.apogee.mantis_blades.damage_type");
    }

    public static List<Component> descriptionLines(MantisVariant variant) {
        return List.of(Component.translatable("tooltip.apogee.mantis_blades.description." + key(variant)));
    }

    public static List<Section> sections(MantisVariant variant) {
        List<Section> sections = new ArrayList<>();
        boolean wire = variant == MantisVariant.NANOWIRE;
        sections.add(new Section(ApogeeClient.TOGGLE_MANTIS.getTranslatedKeyMessage().copy(),
                List.of(Component.translatable(wire ? "tooltip.apogee.nanowire.toggle" : "tooltip.apogee.mantis_blades.toggle"))));
        sections.add(new Section(Component.translatable("tooltip.apogee.mantis_blades.left_click"),
                List.of(Component.translatable(wire ? "tooltip.apogee.nanowire.lash" : "tooltip.apogee.mantis_blades.slash"))));
        sections.add(new Section(Component.translatable("tooltip.apogee.mantis_blades.right_click"),
                List.of(Component.translatable(wire ? "tooltip.apogee.nanowire.heavy" : "tooltip.apogee.mantis_blades.lunge"))));
        if (variant == MantisVariant.THERMAL || variant == MantisVariant.ELECTRIC) {
            sections.add(new Section(Component.translatable("tooltip.apogee.mantis_blades.ability." + key(variant)),
                    List.of(Component.translatable("tooltip.apogee.mantis_blades.ability." + key(variant) + ".text"))));
        }
        return sections;
    }

    public static Component statsHeader() {
        return Component.translatable("tooltip.apogee.mantis_blades.stats_header");
    }

    public static List<Stat> stats(MantisVariant variant) {
        List<Stat> stats = new ArrayList<>();
        // Damage per hit and attacks per second are already on the card in large type.
        if (variant == MantisVariant.NANOWIRE) {
            stats.add(stat("critical", Component.literal(number(NanowireRules.ATTACK_DAMAGE * 1.5)), false));
            stats.add(stat("reach", Component.translatable("tooltip.apogee.value.blocks", number(3.0 + NanowireRules.REACH_BONUS)), false));
            stats.add(stat("heavy", Component.literal(number(NanowireRules.HEAVY_DAMAGE)), true));
            stats.add(stat("heavy_arc", Component.translatable("tooltip.apogee.value.degrees", number(NanowireRules.HEAVY_HALF_ANGLE * 2.0)), false));
            stats.add(stat("heavy_cooldown", seconds(NanowireRules.HEAVY_COOLDOWN_TICKS), false));
            return stats;
        }
        stats.add(stat("critical", Component.literal(number(MantisRules.ATTACK_DAMAGE * 1.5)), false));
        stats.add(stat("lunge", Component.literal(number(MantisRules.THRUST_DAMAGE)), true));
        stats.add(stat("lunge_reach", Component.translatable("tooltip.apogee.value.blocks", number(MantisRules.THRUST_REACH)), false));
        stats.add(stat("lunge_cooldown", seconds(MantisRules.THRUST_COOLDOWN_TICKS), false));
        if (variant == MantisVariant.THERMAL) {
            stats.add(stat("burn", Component.translatable("tooltip.apogee.value.seconds", number(MantisRules.THERMAL_BURN_SECONDS)), true));
        } else if (variant == MantisVariant.ELECTRIC) {
            stats.add(stat("stun", seconds(MantisRules.ELECTRIC_STUN_TICKS), true));
            stats.add(stat("water", Component.translatable("tooltip.apogee.value.bonus", number(MantisRules.ELECTRIC_WATER_BONUS)), false));
        }
        return stats;
    }

    /** Held-item and enchantment rules, shown small under the numbers. */
    public static Component note() {
        return Component.translatable("tooltip.apogee.mantis_blades.note");
    }

    public static Component brand() {
        return Component.translatable("tooltip.apogee.brand");
    }

    /** Plain lines for when Simply Tooltips is not installed. */
    public static List<Component> plainLines(MantisVariant variant) {
        List<Component> lines = new ArrayList<>(descriptionLines(variant));
        lines.add(Component.translatable("tooltip.apogee.mantis_blades.plain_stats", damagePerHit(variant), attacksPerSecond(variant), damageType(variant)));
        for (Section section : sections(variant)) {
            lines.add(Component.translatable("tooltip.apogee.mantis_blades.plain_section", section.header(), section.lines().get(0)));
        }
        return lines;
    }

    private static Stat stat(String key, Component value, boolean separated) {
        return new Stat(Component.translatable("tooltip.apogee.mantis_blades.stat." + key), value, separated);
    }

    private static String key(MantisVariant variant) {
        return variant.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static Component seconds(int ticks) {
        return Component.translatable("tooltip.apogee.value.seconds", number(ticks / 20.0));
    }

    /** At most two decimals: the nanowire's 1.33 attacks per second, not 1.3333333333333333. */
    private static String number(double value) {
        return new BigDecimal(Double.toString(value)).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
