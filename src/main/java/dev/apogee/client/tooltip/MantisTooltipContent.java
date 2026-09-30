package dev.apogee.client.tooltip;

import dev.apogee.client.ApogeeClient;
import dev.apogee.mantis.MantisRules;
import dev.apogee.mantis.MantisVariant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Tooltip text for the Mantis Blades, read from {@link MantisRules} so the tooltip always shows
 * what the blades actually do. Laid out like a Cyberpunk 2077 weapon card: damage per hit and
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

    public static Component damagePerHit() {
        return Component.literal(number(MantisRules.ATTACK_DAMAGE));
    }

    public static Component damagePerHitLabel() {
        return Component.translatable("tooltip.apogee.mantis_blades.damage_per_hit");
    }

    public static Component attacksPerSecond() {
        return Component.literal(number(MantisRules.ATTACK_SPEED));
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
        return List.of(
                Component.translatable("tooltip.apogee.mantis_blades.description." + key(variant) + ".0"),
                Component.translatable("tooltip.apogee.mantis_blades.description." + key(variant) + ".1"));
    }

    public static List<Section> sections(MantisVariant variant) {
        List<Section> sections = new ArrayList<>();
        sections.add(new Section(ApogeeClient.TOGGLE_MANTIS.getTranslatedKeyMessage().copy(),
                List.of(Component.translatable("tooltip.apogee.mantis_blades.toggle"))));
        sections.add(new Section(Component.translatable("tooltip.apogee.mantis_blades.left_click"),
                List.of(Component.translatable("tooltip.apogee.mantis_blades.slash"))));
        sections.add(new Section(Component.translatable("tooltip.apogee.mantis_blades.right_click"),
                List.of(Component.translatable("tooltip.apogee.mantis_blades.lunge"))));
        if (variant != MantisVariant.STANDARD) {
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
        lines.add(Component.translatable("tooltip.apogee.mantis_blades.plain_stats", damagePerHit(), attacksPerSecond(), damageType(variant)));
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

    private static String number(double value) {
        return new BigDecimal(Double.toString(value)).stripTrailingZeros().toPlainString();
    }
}
