package dev.apogee.mixin;

import dev.apogee.ripperdoc.CyberwareSlots;
import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.TrinketInventory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While Trinkets builds the inventory menu's slots, the trinket component answers without the
 * cyberware group, so implants get no slot there and are only fitted at a ripperdoc table. Any
 * other caller, the ripperdoc table included, still sees the whole component. The flag is set by
 * {@link TrinketsInventoryMenuMixin}.
 */
@Mixin(value = LivingEntityTrinketComponent.class, remap = false)
public abstract class TrinketsComponentMixin {
    @Inject(method = "getGroups", at = @At("RETURN"), cancellable = true, remap = false)
    private void apogee$hideCyberwareGroup(CallbackInfoReturnable<Map<String, SlotGroup>> cir) {
        if (CyberwareSlots.hiding() && cir.getReturnValue().containsKey(CyberwareSlots.GROUP)) {
            Map<String, SlotGroup> shown = new LinkedHashMap<>(cir.getReturnValue());
            shown.remove(CyberwareSlots.GROUP);
            cir.setReturnValue(shown);
        }
    }

    @Inject(method = "getInventory", at = @At("RETURN"), cancellable = true, remap = false)
    private void apogee$hideCyberwareInventory(CallbackInfoReturnable<Map<String, Map<String, TrinketInventory>>> cir) {
        if (CyberwareSlots.hiding() && cir.getReturnValue().containsKey(CyberwareSlots.GROUP)) {
            Map<String, Map<String, TrinketInventory>> shown = new LinkedHashMap<>(cir.getReturnValue());
            shown.remove(CyberwareSlots.GROUP);
            cir.setReturnValue(shown);
        }
    }
}
