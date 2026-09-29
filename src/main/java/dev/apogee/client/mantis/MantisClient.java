package dev.apogee.client.mantis;

import dev.apogee.mantis.MantisRules;
import dev.apogee.network.ApogeeNetworking.MantisSwing;
import dev.apogee.network.ApogeeNetworking.MantisSync;
import dev.apogee.network.ApogeeNetworking.MantisThrust;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Client view of who has the Mantis Blades out: unfold progress, the arm that slashed last,
 * the lunge animation, and the input lock (hotbar, inventory, use) for the local player.
 */
public final class MantisClient {
    private static final class Anim {
        boolean deployed;
        float progress;
        float previous;
        int swingArm = MantisRules.ARM_LEFT;
        int nextArm = MantisRules.ARM_RIGHT;
        long thrustStart = Long.MIN_VALUE;
    }

    private static final Int2ObjectOpenHashMap<Anim> ANIMS = new Int2ObjectOpenHashMap<>();
    private static long ticks;
    private static int lockedSlot = -1;
    private static long localThrustReady;

    /** True while the first-person hands are drawn, so the third-person pose is not applied to them. */
    public static boolean firstPersonRendering;

    private MantisClient() {
    }

    public static void reset() {
        ANIMS.clear();
        lockedSlot = -1;
        localThrustReady = 0L;
        firstPersonRendering = false;
    }

    private static Anim anim(int id) {
        return ANIMS.computeIfAbsent(id, k -> new Anim());
    }

    // ------------------------------------------------------------------ network

    public static void accept(MantisSync payload) {
        Minecraft mc = Minecraft.getInstance();
        Anim anim = anim(payload.entityId());
        anim.deployed = payload.deployed();
        anim.nextArm = MantisRules.ARM_RIGHT;
        anim.swingArm = MantisRules.ARM_LEFT;
        if (mc.player != null && payload.entityId() == mc.player.getId()) {
            lockedSlot = payload.deployed() ? mc.player.getInventory().selected : -1;
        }
    }

    public static void accept(MantisSwing payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && payload.entityId() == mc.player.getId()) {
            return; // the local player predicts its own swings
        }
        Anim anim = anim(payload.entityId());
        if (payload.arm() == MantisRules.ARM_BOTH) {
            anim.thrustStart = ticks;
        } else {
            anim.swingArm = payload.arm();
        }
    }

    /** Called when the local player starts a swing; mirrors the server's alternation. */
    public static void onLocalSwing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Anim anim = ANIMS.get(mc.player.getId());
        if (anim == null || !anim.deployed) {
            return;
        }
        anim.swingArm = anim.nextArm;
        anim.nextArm = anim.nextArm == MantisRules.ARM_RIGHT ? MantisRules.ARM_LEFT : MantisRules.ARM_RIGHT;
    }

    // ------------------------------------------------------------------ queries

    public static boolean isDeployed(Entity entity) {
        Anim anim = ANIMS.get(entity.getId());
        return anim != null && anim.deployed;
    }

    /** Eased unfold progress, 0 folded to 1 fully open. */
    public static float deployProgress(Entity entity, float partialTick) {
        Anim anim = ANIMS.get(entity.getId());
        if (anim == null) {
            return 0.0F;
        }
        float raw = Mth.lerp(partialTick, anim.previous, anim.progress);
        return raw * raw * (3.0F - 2.0F * raw);
    }

    public static int swingArm(Entity entity) {
        Anim anim = ANIMS.get(entity.getId());
        return anim == null ? MantisRules.ARM_RIGHT : anim.swingArm;
    }

    /** Lunge progress 0..1, or -1 when no lunge is playing. */
    public static float thrustProgress(Entity entity, float partialTick) {
        Anim anim = ANIMS.get(entity.getId());
        if (anim == null || anim.thrustStart == Long.MIN_VALUE) {
            return -1.0F;
        }
        float t = (ticks - anim.thrustStart + partialTick) / MantisRules.THRUST_ANIMATION_TICKS;
        return t >= 0.0F && t <= 1.0F ? t : -1.0F;
    }

    // ------------------------------------------------------------------ tick

    public static void tick(Minecraft mc) {
        ticks++;
        float step = 1.0F / MantisRules.DEPLOY_TICKS;
        var iterator = ANIMS.int2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Anim anim = iterator.next().getValue();
            anim.previous = anim.progress;
            anim.progress = Mth.clamp(anim.progress + (anim.deployed ? step : -step), 0.0F, 1.0F);
            if (!anim.deployed && anim.progress == 0.0F && anim.previous == 0.0F) {
                iterator.remove();
            }
        }
        LocalPlayer player = mc.player;
        if (player == null || !isDeployed(player)) {
            return;
        }
        // Runs before the vanilla key handling of this tick, so consumed clicks never reach it.
        Options options = mc.options;
        drain(options.keyInventory);
        drain(options.keySwapOffhand);
        drain(options.keyDrop);
        drain(options.keyPickItem);
        for (KeyMapping key : options.keyHotbarSlots) {
            drain(key);
        }
        while (options.keyUse.consumeClick()) {
            if (mc.screen == null && ticks >= localThrustReady && ClientPlayNetworking.canSend(MantisThrust.TYPE)) {
                localThrustReady = ticks + MantisRules.THRUST_COOLDOWN_TICKS;
                anim(player.getId()).thrustStart = ticks;
                ClientPlayNetworking.send(MantisThrust.INSTANCE);
            }
        }
        if (lockedSlot >= 0) {
            player.getInventory().selected = lockedSlot;
        }
    }

    private static void drain(KeyMapping key) {
        while (key.consumeClick()) {
            // discard
        }
    }

    /** True while the local player's hotbar must not change. */
    public static boolean hotbarLocked() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && isDeployed(mc.player);
    }
}
