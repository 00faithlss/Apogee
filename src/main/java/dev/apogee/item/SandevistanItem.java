package dev.apogee.item;

import dev.apogee.Apogee;
import dev.apogee.registry.ApogeeSounds;
import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;

/** Reusable implant with no durability; equipped in the Trinkets slot cyberware/os. */
public class SandevistanItem extends TrinketItem {
    /**
     * Completed the first time a player gets the implant by any means (crafting, a chest, another
     * player). Its id predates that and is kept so nobody loses it.
     */
    public static final ResourceLocation OBTAINED_ADVANCEMENT = Apogee.id("craft_sandevistan");

    public SandevistanItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * Adam Smasher's verdict on the Sandevistan, heard by every player on the server wherever they
     * are. Played when {@code player} completes {@link #OBTAINED_ADVANCEMENT}, so once per player.
     */
    public static void playVoiceLine(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer listener : server.getPlayerList().getPlayers()) {
            listener.playNotifySound(ApogeeSounds.RUDIMENTARY_IMPLANT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
