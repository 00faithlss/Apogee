package dev.apogee.item;

import dev.apogee.registry.ApogeeSounds;
import dev.emi.trinkets.api.TrinketItem;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Reusable implant with no durability; equipped in the Trinkets slot cyberware/os. */
public class SandevistanItem extends TrinketItem {
    /** Shift-click crafting reports the same craft more than once; keep the voice line to one play. */
    private static final int VOICE_LINE_GAP_TICKS = 40;
    private static final Map<Player, Long> LAST_VOICE_LINE = new WeakHashMap<>();

    public SandevistanItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (level.isClientSide || level.getServer() == null) {
            return;
        }
        long now = level.getGameTime();
        Long last = LAST_VOICE_LINE.get(player);
        if (last != null && now - last < VOICE_LINE_GAP_TICKS) {
            return;
        }
        LAST_VOICE_LINE.put(player, now);
        // Adam Smasher's verdict on the Sandevistan, heard by every player on the server wherever they are.
        for (ServerPlayer listener : level.getServer().getPlayerList().getPlayers()) {
            listener.playNotifySound(ApogeeSounds.RUDIMENTARY_IMPLANT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
