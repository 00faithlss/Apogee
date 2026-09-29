package dev.apogee.item;

import dev.emi.trinkets.api.TrinketItem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Reusable arm implant with no durability; equipped in the Trinkets slot cyberware/arms. */
public class MantisBladesItem extends TrinketItem {
    public MantisBladesItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        for (int i = 0; i < 4; i++) {
            lines.add(Component.translatable("tooltip.apogee.mantis_blades.line." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
