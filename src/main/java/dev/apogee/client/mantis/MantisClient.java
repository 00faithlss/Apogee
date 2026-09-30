package dev.apogee.client.mantis;

import dev.apogee.mantis.MantisManager;
import dev.apogee.mantis.MantisRules;
import dev.apogee.mantis.MantisVariant;
import dev.apogee.network.ApogeeNetworking.MantisSwing;
import dev.apogee.network.ApogeeNetworking.MantisSync;
import dev.apogee.network.ApogeeNetworking.MantisThrust;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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
        /** Client tick each arm's last slash started on, indexed by {@link MantisRules#ARM_RIGHT}/{@link MantisRules#ARM_LEFT}. */
        final long[] slashStart = {Long.MIN_VALUE / 2, Long.MIN_VALUE / 2};
        MantisVariant variant = MantisVariant.STANDARD;
    }

    private static final Int2ObjectOpenHashMap<Anim> ANIMS = new Int2ObjectOpenHashMap<>();
    private static long ticks;
    private static int lockedSlot = -1;
    private static long localThrustReady;
    /** Client tick the local player's blades were last deployed on. */
    private static long localDeployTick = Long.MIN_VALUE / 2;
    /** Whether that deploy had to put a held item away first. */
    private static boolean stowingItems;
    /** Whether the local implant view was up last tick. */
    private static boolean localOut;

    /**
     * Vanilla's item swap, from {@code ItemInHandRenderer}: the hand height falls or rises by 0.4 a
     * tick and a lowered hand sits 0.6 below its place. Deploying with an item in hand first lets
     * that item go down this way; folding sends the implant down before the item comes back up.
     */
    public static final float EQUIP_SPEED = 0.4F;
    public static final float EQUIP_DROP = 0.6F;
    /** Ticks vanilla takes to lower a held item out of view. */
    public static final int STOW_TICKS = 3;

    /** True while the first-person hands are drawn, so the third-person pose is not applied to them. */
    public static boolean firstPersonRendering;

    private MantisClient() {
    }

    public static void reset() {
        ANIMS.clear();
        MantisRig.clear();
        lockedSlot = -1;
        localThrustReady = 0L;
        localDeployTick = Long.MIN_VALUE / 2;
        stowingItems = false;
        localOut = false;
        firstPersonRendering = false;
    }

    private static Anim anim(int id) {
        return ANIMS.computeIfAbsent(id, k -> new Anim());
    }

    // ------------------------------------------------------------------ network

    public static void accept(MantisSync payload) {
        Minecraft mc = Minecraft.getInstance();
        Anim anim = anim(payload.entityId());
        if (mc.player != null && payload.entityId() == mc.player.getId() && payload.deployed() && !anim.deployed) {
            localDeployTick = ticks;
            stowingItems = holdsItems(mc.player);
        }
        anim.deployed = payload.deployed();
        if (payload.deployed()) {
            anim.variant = MantisVariant.byId(payload.variant());
        }
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
            MantisRig.of(payload.entityId()).thrust();
        } else {
            anim.swingArm = payload.arm();
            anim.slashStart[payload.arm()] = ticks;
            MantisRig.of(payload.entityId()).attack(payload.arm());
        }
        strike(mc, anim, payload.entityId());
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
        // Holding the button on a block swings every tick; one cut at a time, as on the server.
        if (ticks - anim.slashStart[anim.swingArm] < MantisRules.SLASH_INTERVAL_TICKS) {
            return;
        }
        anim.swingArm = anim.nextArm;
        anim.nextArm = anim.nextArm == MantisRules.ARM_RIGHT ? MantisRules.ARM_LEFT : MantisRules.ARM_RIGHT;
        anim.slashStart[anim.swingArm] = ticks;
        MantisRig.of(mc.player.getId()).attack(anim.swingArm);
        strike(mc, anim, mc.player.getId());
    }

    /** Throws sparks or embers from thermal and electric blades as they strike. */
    private static void strike(Minecraft mc, Anim anim, int entityId) {
        if (anim.variant == MantisVariant.STANDARD) {
            return;
        }
        Entity entity = mc.level == null ? null : mc.level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        var look = entity.getLookAngle();
        var random = mc.level.random;
        for (int i = 0; i < 6; i++) {
            ParticleOptions particle = anim.variant == MantisVariant.THERMAL ? MantisBladeFx.heatParticle(random) : ParticleTypes.ELECTRIC_SPARK;
            mc.level.addParticle(particle,
                    entity.getX() + look.x * (0.6 + random.nextDouble() * 1.4) + (random.nextDouble() - 0.5) * 0.8,
                    entity.getEyeY() - 0.5 + look.y * 0.8 + (random.nextDouble() - 0.5) * 0.6,
                    entity.getZ() + look.z * (0.6 + random.nextDouble() * 1.4) + (random.nextDouble() - 0.5) * 0.8,
                    (random.nextDouble() - 0.5) * 0.05, 0.02, (random.nextDouble() - 0.5) * 0.05);
        }
    }

    // ------------------------------------------------------------------ queries

    public static long clientTicks() {
        return ticks;
    }

    public static boolean isDeployed(Entity entity) {
        Anim anim = ANIMS.get(entity.getId());
        return anim != null && anim.deployed;
    }

    /** True while the blades are out or still folding away. */
    public static boolean isOut(Entity entity) {
        Anim anim = ANIMS.get(entity.getId());
        return anim != null && (anim.deployed || anim.progress > 0.0F || anim.previous > 0.0F);
    }

    private static boolean holdsItems(LocalPlayer player) {
        return !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
    }

    /**
     * True while the held items must look put away to the first-person renderers (vanilla's and
     * other mods'): whenever the implant view is up, so they lower the item before it and raise it
     * after it the same way as when swapping items.
     */
    public static boolean hideHeldItems(LocalPlayer player) {
        return isOut(player);
    }

    /** True once the implant view may take over: at once with empty hands, else once the item is down. */
    public static boolean implantViewReady(LocalPlayer player) {
        return isOut(player) && (!stowingItems || ticks - localDeployTick >= STOW_TICKS);
    }

    /**
     * How far the implant arms are raised into view, 0 (a whole equip drop below) to 1. With an
     * item that was put away they rise after it like a swapped-in item, and sink at the end of the
     * fold before the item comes back; with empty hands they stay in place.
     */
    public static float implantLift(LocalPlayer player, float partialTick) {
        Anim anim = ANIMS.get(player.getId());
        if (anim == null || !(stowingItems || holdsItems(player))) {
            return 1.0F;
        }
        float rise = stowingItems ? Mth.clamp((ticks - localDeployTick - STOW_TICKS + partialTick) * EQUIP_SPEED, 0.0F, 1.0F) : 1.0F;
        if (anim.deployed) {
            return rise;
        }
        float remaining = Mth.lerp(partialTick, anim.previous, anim.progress) * MantisRules.DEPLOY_TICKS;
        return Math.min(rise, Mth.clamp(remaining * EQUIP_SPEED, 0.0F, 1.0F));
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

    /** Variant of the Mantis Blades the entity wears (Trinkets syncs worn items), or null. */
    public static MantisVariant variant(Entity entity) {
        return entity instanceof LivingEntity living ? MantisManager.equippedVariant(living) : null;
    }

    public static int swingArm(Entity entity) {
        Anim anim = ANIMS.get(entity.getId());
        return anim == null ? MantisRules.ARM_RIGHT : anim.swingArm;
    }

    /** Progress 0..1 of the slash the given arm is making, or -1 when it is not slashing. */
    public static float slashProgress(Entity entity, int arm, float partialTick) {
        Anim anim = ANIMS.get(entity.getId());
        if (anim == null || anim.slashStart[arm] == Long.MIN_VALUE / 2) {
            return -1.0F;
        }
        float t = (ticks - anim.slashStart[arm] + partialTick) / MantisRules.SLASH_ANIMATION_TICKS;
        return t >= 0.0F && t <= 1.0F ? t : -1.0F;
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
        if (mc.level != null && ticks % 100 == 0) {
            MantisRig.prune(id -> mc.level.getEntity(id) != null);
        }
        MantisBladeFx.tick(mc);
        MantisHandPose.tick(mc.player);
        LocalPlayer player = mc.player;
        if (player != null) {
            boolean out = isOut(player);
            if (localOut && !out) {
                // The implant has sunk out of view: bring the held items up from below at once,
                // instead of first lowering the empty hand vanilla thinks was there.
                if (!player.getMainHandItem().isEmpty()) {
                    mc.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.MAIN_HAND);
                }
                if (!player.getOffhandItem().isEmpty()) {
                    mc.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.OFF_HAND);
                }
                stowingItems = false;
            }
            localOut = out;
        }
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
                Anim self = anim(player.getId());
                self.thrustStart = ticks;
                MantisRig.of(player.getId()).thrust();
                strike(mc, self, player.getId());
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
