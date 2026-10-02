package dev.apogee.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Leg implant that allows one extra jump in the air; equipped in the Trinkets slot cyberware/legs. */
public class ReinforcedTendonsItem extends CyberwareItem {
    public ReinforcedTendonsItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.apogee.reinforced_tendons.slot").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.apogee.reinforced_tendons.description").withStyle(ChatFormatting.AQUA));
    }
}
