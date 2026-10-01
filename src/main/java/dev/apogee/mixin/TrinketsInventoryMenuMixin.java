package dev.apogee.mixin;

import dev.apogee.ripperdoc.CyberwareSlots;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the moment Trinkets builds its slots into the player's inventory menu, so the cyberware
 * group is left out of it (see {@link TrinketsComponentMixin}). Both the server and the client
 * build the menu this way, so their slot lists still match. The target method is added by Trinkets'
 * own mixin, hence the higher priority.
 */
@Mixin(value = InventoryMenu.class, priority = 1500)
public abstract class TrinketsInventoryMenuMixin {
    @Dynamic("Added to InventoryMenu by Trinkets")
    @Inject(method = "trinkets$updateTrinketSlots", at = @At("HEAD"), remap = false)
    private void apogee$hideCyberwareStart(boolean slotsChanged, CallbackInfo ci) {
        CyberwareSlots.startHiding();
    }

    @Dynamic("Added to InventoryMenu by Trinkets")
    @Inject(method = "trinkets$updateTrinketSlots", at = @At("RETURN"), remap = false)
    private void apogee$hideCyberwareEnd(boolean slotsChanged, CallbackInfo ci) {
        CyberwareSlots.stopHiding();
    }
}
