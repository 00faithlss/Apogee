package dev.apogee.client.tendons;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apogee.registry.ApogeeSounds;
import dev.apogee.network.ApogeeNetworking.AirDashRequest;
import dev.apogee.network.ApogeeNetworking.DoubleJumpRequest;
import dev.apogee.tendons.ReinforcedTendons;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Client half of the Reinforced Tendons. In the air, pressing jump launches you upward and
 * pressing the sprint key dashes in the direction you are pushing (forward, back or sideways),
 * or where you look when no direction is held. Each works once per time in the air, independently.
 * Holding the key from the ground does not count: it has to be pressed again in the air.
 */
public final class TendonsClient {
    /**
     * Optional dedicated dash key, unbound by default. Minecraft routes each physical key to a
     * single mapping, so binding Left Ctrl here would steal it from sprint; by default the dash
     * reads the sprint key's physical state instead.
     */
    private static final KeyMapping DASH = new KeyMapping(
            "key.apogee.air_dash", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.apogee");

    private static boolean jumpCharged;
    private static boolean jumpWasDown;
    private static boolean jumpHeldSinceTakeoff;
    private static boolean dashCharged;
    private static boolean dashHeldSinceTakeoff;
    private static boolean sprintKeyWasDown;
    private static boolean forcedJump;
    private static boolean forcedDash;
    /** Test hook: the direction the next forced dash is pushed in, as {forward, left}; null reads the real input. */
    private static float[] forcedInput;

    private TendonsClient() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(DASH);
        ClientTickEvents.END_CLIENT_TICK.register(TendonsClient::tick);
    }

    /** Test hook: behaves as if jump was just pressed this tick. */
    public static void simulateJumpPressForTests() {
        forcedJump = true;
    }

    /** Test hook: behaves as if the dash key was just pressed this tick, pushing {@code forward}/{@code left} (each -1..1). */
    public static void simulateDashPressForTests(float forward, float left) {
        sprintKeyWasDown = false;
        forcedDash = true;
        forcedInput = new float[]{forward, left};
    }

    private static void tick(Minecraft client) {
        boolean pressedDash = false;
        while (DASH.consumeClick()) {
            pressedDash = true;
        }
        boolean sprintKeyDown = isPhysicallyDown(client, client.options.keySprint);
        pressedDash |= sprintKeyDown && !sprintKeyWasDown || forcedDash;
        boolean dashDown = sprintKeyDown || DASH.isDown();
        sprintKeyWasDown = sprintKeyDown;
        boolean jumpDown = client.options.keyJump.isDown();
        boolean pressedJump = jumpDown && !jumpWasDown || forcedJump;
        jumpWasDown = jumpDown;

        LocalPlayer player = client.player;
        if (player == null || client.screen != null) {
            forcedJump = false;
            forcedDash = false;
            return;
        }
        if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            jumpCharged = true;
            dashCharged = true;
            jumpHeldSinceTakeoff = jumpDown;
            dashHeldSinceTakeoff = dashDown;
            forcedJump = false;
            forcedDash = false;
            return;
        }
        if (!jumpDown) {
            jumpHeldSinceTakeoff = false;
        }
        if (!dashDown) {
            dashHeldSinceTakeoff = false;
        }
        boolean usable = ReinforcedTendons.isEquipped(player) && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isPassenger() && !player.isSpectator();
        if (usable && pressedJump && jumpCharged && !jumpHeldSinceTakeoff) {
            jumpCharged = false;
            jump(player);
        }
        if (usable && pressedDash && dashCharged && !dashHeldSinceTakeoff) {
            dashCharged = false;
            dash(player);
        }
        forcedJump = false;
        forcedDash = false;
        forcedInput = null;
    }

    private static void jump(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        // A second ordinary jump: the vertical speed is set, not added, so it never stacks on a rise or a fall.
        player.setDeltaMovement(velocity.x, ReinforcedTendons.jumpPower(player), velocity.z);
        player.playSound(ApogeeSounds.TENDONS_DOUBLE_JUMP, 0.7F, 1.0F);
        if (ClientPlayNetworking.canSend(DoubleJumpRequest.TYPE)) {
            ClientPlayNetworking.send(DoubleJumpRequest.INSTANCE);
        }
    }

    private static void dash(LocalPlayer player) {
        float forward = forcedInput != null ? forcedInput[0] : player.input.forwardImpulse;
        float left = forcedInput != null ? forcedInput[1] : player.input.leftImpulse;
        if (forward == 0.0F && left == 0.0F) {
            forward = 1.0F;
        }
        float length = Mth.sqrt(forward * forward + left * left);
        forward /= length;
        left /= length;
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);
        // Forward is (-sin, cos) and left is (cos, sin) in world x/z for a player facing yaw.
        double x = (-sin * forward + cos * left) * ReinforcedTendons.DASH_SPEED;
        double z = (cos * forward + sin * left) * ReinforcedTendons.DASH_SPEED;
        player.setDeltaMovement(x, Math.max(player.getDeltaMovement().y, ReinforcedTendons.DASH_MIN_VERTICAL), z);
        player.playSound(ApogeeSounds.TENDONS_AIR_DASH, 0.7F, 1.0F);
        if (ClientPlayNetworking.canSend(AirDashRequest.TYPE)) {
            ClientPlayNetworking.send(AirDashRequest.INSTANCE);
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
}
