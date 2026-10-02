package dev.apogee.client.tendons;

import dev.apogee.network.ApogeeNetworking.DoubleJumpRequest;
import dev.apogee.tendons.ReinforcedTendons;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the Reinforced Tendons. Pressing jump in the air launches you once per time in
 * the air. Holding jump from the ground does not count: it has to be pressed again in the air.
 */
public final class TendonsClient {
    private static boolean charged;
    private static boolean jumpWasDown;
    private static boolean heldSinceTakeoff;
    /** Test hook: behaves as if jump was just pressed this tick. */
    private static boolean forcedPress;

    private TendonsClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TendonsClient::tick);
    }

    public static void simulateJumpPressForTests() {
        forcedPress = true;
    }

    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        boolean down = client.options.keyJump.isDown();
        boolean pressed = down && !jumpWasDown || forcedPress;
        forcedPress = false;
        jumpWasDown = down;
        if (player == null || client.screen != null) {
            return;
        }
        if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            charged = true;
            heldSinceTakeoff = down;
            return;
        }
        if (!down) {
            heldSinceTakeoff = false;
        }
        if (!pressed || heldSinceTakeoff || !charged) {
            return;
        }
        if (!ReinforcedTendons.isEquipped(player) || player.getAbilities().flying || player.isFallFlying()
                || player.isPassenger() || player.isSpectator()) {
            return;
        }
        charged = false;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.0) + ReinforcedTendons.BOOST_Y, velocity.z);
        player.fallDistance = -ReinforcedTendons.FALL_MARGIN;
        player.playSound(SoundEvents.PISTON_EXTEND, 0.25F, 1.8F);
        if (ClientPlayNetworking.canSend(DoubleJumpRequest.TYPE)) {
            ClientPlayNetworking.send(DoubleJumpRequest.INSTANCE);
        }
    }
}
