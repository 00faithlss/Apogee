package dev.apogee.item;

import dev.apogee.mantis.MantisVariant;
import dev.emi.trinkets.api.TrinketItem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Reusable arm implant with no durability; equipped in the Trinkets slot cyberware/arms. */
public class MantisBladesItem extends TrinketItem {
    private final MantisVariant variant;

    public MantisBladesItem(Item.Properties properties, MantisVariant variant) {
        super(properties);
        this.variant = variant;
    }

    public MantisVariant variant() {
        return variant;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        for (int i = 0; i < 4; i++) {
            lines.add(Component.translatable("tooltip.apogee.mantis_blades.line." + i).withStyle(ChatFormatting.GRAY));
        }
        if (variant != MantisVariant.STANDARD) {
            lines.add(Component.translatable("tooltip.apogee.mantis_blades.variant." + variant.name().toLowerCase())
                    .withStyle(variant == MantisVariant.THERMAL ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        }
    }
}
