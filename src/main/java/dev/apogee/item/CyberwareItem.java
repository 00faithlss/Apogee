package dev.apogee.item;

import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An implant: it sits in a {@code cyberware/*} Trinkets slot, but only a ripperdoc table puts it
 * there or takes it out. Those slots are left out of the inventory screens, and the item does not
 * equip itself on use the way other trinkets do.
 */
public abstract class CyberwareItem extends TrinketItem {
    protected CyberwareItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
