package dev.apogee.client.tooltip;

import dev.apogee.tendons.ReinforcedTendons;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Tooltip text for the Reinforced Tendons. */
public final class TendonsTooltipContent {
    public record Stat(Component label, Component value, boolean separatedBefore) {
    }

    private TendonsTooltipContent() {
    }

    public static Component title() {
        return Component.translatable("item.apogee.reinforced_tendons");
    }

    public static Component badge() {
        return Component.translatable("tooltip.apogee.reinforced_tendons.badge");
    }

    public static Component descriptionHeader() {
        return Component.translatable("tooltip.apogee.reinforced_tendons.description_header");
    }

    public static List<Component> descriptionLines() {
        return List.of(
                Component.translatable("tooltip.apogee.reinforced_tendons.description.0"),
                Component.translatable("tooltip.apogee.reinforced_tendons.description.1"),
                Component.translatable("tooltip.apogee.reinforced_tendons.description.2"),
                Component.translatable("tooltip.apogee.reinforced_tendons.description.3"));
    }

    public static Component statsHeader() {
        return Component.translatable("tooltip.apogee.reinforced_tendons.stats_header");
    }

    public static List<Stat> stats() {
        return List.of(
                stat("jump", number(ReinforcedTendons.BOOST_Y), false),
                stat("fall", Component.translatable("tooltip.apogee.value.blocks", number(ReinforcedTendons.FALL_MARGIN)), false),
                stat("dash", Component.translatable("tooltip.apogee.value.blocks_per_second", number(ReinforcedTendons.DASH_SPEED * 20.0)), false),
                stat("directions", Component.translatable("tooltip.apogee.reinforced_tendons.all"), false),
                stat("recharge", Component.translatable("tooltip.apogee.reinforced_tendons.on_landing"), true));
    }

    public static Component note() {
        return Component.translatable("tooltip.apogee.reinforced_tendons.note");
    }

    public static Component brand() {
        return Component.translatable("tooltip.apogee.brand");
    }

    private static Stat stat(String key, Object value, boolean separated) {
        Component text = value instanceof Component component ? component : Component.literal(String.valueOf(value));
        return new Stat(Component.translatable("tooltip.apogee.reinforced_tendons.stat." + key), text, separated);
    }

    private static String number(double value) {
        return new BigDecimal(Double.toString(value)).stripTrailingZeros().toPlainString();
    }
}
