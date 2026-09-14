package dev.apogee.client.compat.immersivearmors;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apogee.compat.immersivearmors.SteamDash;
import dev.apogee.network.ApogeeNetworking.SteamDashRequest;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Client half of the Steampunk Leggings dash. Pressing the key while airborne launches you
 * where you look, once per jump. Holding the key from the ground (for sprinting) does not
 * count: it has to be pressed again in the air.
 */
public final class SteamDashClient {
    /**
     * Optional dedicated key, unbound by default. Minecraft routes each physical key to a single
     * mapping, so binding Left Ctrl here would steal it from sprint; by default the dash reads the
     * sprint key's physical state instead.
     */
    private static final KeyMapping DASH = new KeyMapping(
            "key.apogee.steam_dash", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.apogee");

    private static boolean charged;
    private static boolean heldSinceTakeoff;
    private static boolean sprintKeyWasDown;

    private SteamDashClient() {
    }

    public static void register() {
        if (!SteamDash.isAvailable()) {
            return;
        }
        KeyBindingHelper.registerKeyBinding(DASH);
        ClientTickEvents.END_CLIENT_TICK.register(SteamDashClient::tick);
    }

    private static void tick(Minecraft client) {
        boolean pressed = false;
        while (DASH.consumeClick()) {
            pressed = true;
        }
        boolean sprintKeyDown = isPhysicallyDown(client, client.options.keySprint);
        pressed |= sprintKeyDown && !sprintKeyWasDown || forcedPress;
        forcedPress = false;
        sprintKeyWasDown = sprintKeyDown;
        boolean anyDown = sprintKeyDown || DASH.isDown();

        LocalPlayer player = client.player;
        if (player == null || client.screen != null) {
            return;
        }
        if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            charged = true;
            heldSinceTakeoff = anyDown;
            return;
        }
        if (!anyDown) {
            heldSinceTakeoff = false;
        }
        if (!pressed || heldSinceTakeoff || !charged) {
            return;
        }
        if (!SteamDash.wearsSteampunkLeggings(player) || player.getAbilities().flying || player.isFallFlying()
                || player.isPassenger() || player.isSpectator()) {
            return;
        }
        charged = false;
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(-Mth.sin(yaw) * SteamDash.HORIZONTAL_SPEED,
                Math.max(velocity.y, SteamDash.MIN_VERTICAL_SPEED),
                Mth.cos(yaw) * SteamDash.HORIZONTAL_SPEED);
        player.resetFallDistance();
        player.playSound(SoundEvents.REDSTONE_TORCH_BURNOUT, 0.35F, 0.6F);
        if (ClientPlayNetworking.canSend(SteamDashRequest.TYPE)) {
            ClientPlayNetworking.send(SteamDashRequest.INSTANCE);
        }
    }

    /** Raw key state, independent of toggle-sprint and of which mapping owns the key. */
    private static boolean isPhysicallyDown(Minecraft client, KeyMapping mapping) {
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(mapping);
        long window = client.getWindow().getWindow();
        return switch (key.getType()) {
            case KEYSYM -> key.getValue() != InputConstants.UNKNOWN.getValue() && InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    /** Test hook: behaves as if the sprint key was just pressed this tick. */
    public static void simulateSprintPressForTests() {
        sprintKeyWasDown = false;
        forcedPress = true;
    }

    private static boolean forcedPress;
}
