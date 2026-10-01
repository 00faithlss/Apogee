package dev.apogee.mixin;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.LinkedHashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Leaves the cyberware group out of the slots Trinkets builds into the inventory screens (survival
 * and creative), so implants are only fitted at a ripperdoc table. Only those screens ask for the
 * player's slots this way; the equipped items themselves stay in the player's trinket component.
 * Trinkets ships intermediary names, while development runs remap it to Mojang names, so both
 * descriptors are listed.
 */
@Mixin(value = TrinketsApi.class, remap = false)
public abstract class TrinketsPlayerSlotsMixin {
    private static final String CYBERWARE_GROUP = "cyberware";

    @Inject(
            method = {
                    "getPlayerSlots(Lnet/minecraft/world/entity/player/Player;)Ljava/util/Map;",
                    "getPlayerSlots(Lnet/minecraft/class_1657;)Ljava/util/Map;"
            },
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void apogee$hideCyberware(CallbackInfoReturnable<Map<String, SlotGroup>> cir) {
        Map<String, SlotGroup> groups = cir.getReturnValue();
        if (groups != null && groups.containsKey(CYBERWARE_GROUP)) {
            Map<String, SlotGroup> shown = new LinkedHashMap<>(groups);
            shown.remove(CYBERWARE_GROUP);
            cir.setReturnValue(shown);
        }
    }
}
