package dev.apogee.client.armor;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apogee.armor.MaxtacVisor;
import dev.apogee.registry.ApogeeItems;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class MaxtacVisorClient {
    private static final Set<UUID> ACTIVE = new HashSet<>();
    public static final KeyMapping TOGGLE_AUXILIARY = new KeyMapping("key.apogee.maxtac_visor",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.apogee");

    private MaxtacVisorClient() {}

    public static boolean auxiliaryActive(@Nullable Entity entity) {
        return entity instanceof LivingEntity wearer && MaxtacVisor.isHelmet(wearer.getItemBySlot(EquipmentSlot.HEAD))
                && ACTIVE.contains(wearer.getUUID());
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(TOGGLE_AUXILIARY);
        ClientPlayNetworking.registerGlobalReceiver(MaxtacVisor.Status.TYPE, (payload, context) -> context.client().execute(() -> {
            if (payload.active()) {
                ACTIVE.add(payload.playerId());
            } else {
                ACTIVE.remove(payload.playerId());
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ACTIVE.clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_AUXILIARY.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    var helmet = client.player.getItemBySlot(EquipmentSlot.HEAD);
                    if (MaxtacVisor.isHelmet(helmet) && ClientPlayNetworking.canSend(MaxtacVisor.Toggle.TYPE)) {
                        ClientPlayNetworking.send(MaxtacVisor.Toggle.INSTANCE);
                    } else if (helmet.is(ApogeeItems.TRAUMA_TEAM_HELMET)
                            && ClientPlayNetworking.canSend(dev.apogee.armor.TraumaHelmet.Toggle.TYPE)) {
                        ClientPlayNetworking.send(dev.apogee.armor.TraumaHelmet.Toggle.INSTANCE);
                    }
                }
            }
        });
    }
}
