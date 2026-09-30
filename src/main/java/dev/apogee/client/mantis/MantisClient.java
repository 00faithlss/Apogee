package dev.apogee.client.mantis;

import dev.apogee.client.nanowire.NanowireRig;
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
    /** Client tick the local implant view ended on, when an item has to come back up after it. */
    private static long localUnstowTick = Long.MIN_VALUE / 2;

    /**
     * Deploying or folding with an item in hand works like an item swap, the same with vanilla's
     * hands and another mod's: the whole hand view (arm and item, however they are drawn) eases
     * down out of sight, the implant arms rise, and the reverse on folding. How far down, in hand
     * units (vanilla's own swap lowers the hand 0.6; a tall item needs a little more).
     */
    public static final float HANDS_DROP = 0.9F;
    /** Ticks the held item takes to ease down before the implant rises. */
    public static final int STOW_TICKS = 5;
    /** Ticks the implant arms take to rise into view, sink out of it, or let the item back up. */
    public static final float IMPLANT_RISE_TICKS = 7.0F;

    /** True while the first-person hands are drawn, so the third-person pose is not applied to them. */
    public static boolean firstPersonRendering;

    private MantisClient() {
    }

    public static void reset() {
        ANIMS.clear();
        MantisRig.clear();
        NanowireRig.clear();
        lockedSlot = -1;
        localThrustReady = 0L;
        localDeployTick = Long.MIN_VALUE / 2;
        stowingItems = false;
        localOut = false;
        localUnstowTick = Long.MIN_VALUE / 2;
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
            rigThrust(payload.entityId(), anim);
        } else {
            anim.swingArm = payload.arm();
            anim.slashStart[payload.arm()] = ticks;
            rigAttack(payload.entityId(), anim, payload.arm());
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
        if (ticks - anim.slashStart[anim.swingArm] < MantisRules.slashIntervalTicks(anim.variant)) {
            return;
        }
        anim.swingArm = anim.nextArm;
        anim.nextArm = anim.nextArm == MantisRules.ARM_RIGHT ? MantisRules.ARM_LEFT : MantisRules.ARM_RIGHT;
        anim.slashStart[anim.swingArm] = ticks;
        rigAttack(mc.player.getId(), anim, anim.swingArm);
        strike(mc, anim, mc.player.getId());
    }

    /** Starts one arm's attack clip on whichever arm cyberware the entity wears. */
    private static void rigAttack(int entityId, Anim anim, int arm) {
        if (anim.variant == MantisVariant.NANOWIRE) {
            NanowireRig.of(entityId).attack(arm);
        } else {
            MantisRig.of(entityId).attack(arm);
        }
    }

    /** Starts the two-arm attack clip: the blades' lunge or the two-wire lash. */
    private static void rigThrust(int entityId, Anim anim) {
        if (anim.variant == MantisVariant.NANOWIRE) {
            NanowireRig.of(entityId).heavy();
        } else {
            MantisRig.of(entityId).thrust();
        }
    }

    /** Throws sparks or embers from thermal and electric blades as they strike. */
    private static void strike(Minecraft mc, Anim anim, int entityId) {
        if (anim.variant == MantisVariant.STANDARD || anim.variant == MantisVariant.NANOWIRE) {
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
     * How far the regular hand view (not the implant's) is lowered, 0 in place to 1 out of sight:
     * while a held item goes down before the implant rises, and while it comes back up after the
     * implant sinks.
     */
    public static float handsDrop(LocalPlayer player, float partialTick) {
        if (isOut(player)) {
            // Going away: slow to leave, then quicker.
            return stowingItems ? 1.0F - arrive(1.0F - (ticks - localDeployTick + partialTick) / STOW_TICKS) : 0.0F;
        }
        // Coming in: quick at first, settling gently.
        return 1.0F - arrive((ticks - localUnstowTick + partialTick) / IMPLANT_RISE_TICKS);
    }

    /** True once the implant view may take over: at once with empty hands, else once the item is down. */
    public static boolean implantViewReady(LocalPlayer player) {
        return isOut(player) && (!stowingItems || ticks - localDeployTick >= STOW_TICKS);
    }

    /**
     * How far the implant arms are raised into view, 0 (a whole equip drop below) to 1. With an
     * item that was put away they rise after it like a swapped-in item, only slower and eased, and
     * sink over the end of the fold before the item comes back; with empty hands they stay in place.
     */
    public static float implantLift(LocalPlayer player, float partialTick) {
        Anim anim = ANIMS.get(player.getId());
        if (anim == null || !(stowingItems || holdsItems(player))) {
            return 1.0F;
        }
        float rise = stowingItems ? arrive((ticks - localDeployTick - STOW_TICKS + partialTick) / IMPLANT_RISE_TICKS) : 1.0F;
        if (anim.deployed) {
            return rise;
        }
        // Sinking at the end of the fold: the mirror of rising, slow to leave and then quicker.
        float remaining = Mth.lerp(partialTick, anim.previous, anim.progress) * MantisRules.DEPLOY_TICKS;
        return Math.min(rise, arrive(remaining / IMPLANT_RISE_TICKS));
    }

    /** Cubic ease-out of {@code t} clamped to 0..1: quick at first, settling gently at 1. */
    private static float arrive(float t) {
        t = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - t * t * t;
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
        float t = (ticks - anim.slashStart[arm] + partialTick) / MantisRules.slashAnimationTicks(anim.variant);
        return t >= 0.0F && t <= 1.0F ? t : -1.0F;
    }

    /** Lunge progress 0..1, or -1 when no lunge is playing. */
    public static float thrustProgress(Entity entity, float partialTick) {
        Anim anim = ANIMS.get(entity.getId());
        if (anim == null || anim.thrustStart == Long.MIN_VALUE) {
            return -1.0F;
        }
        float t = (ticks - anim.thrustStart + partialTick) / MantisRules.thrustAnimationTicks(anim.variant);
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
            NanowireRig.prune(id -> mc.level.getEntity(id) != null);
        }
        MantisBladeFx.tick(mc);
        MantisHandPose.tick(mc.player);
        LocalPlayer player = mc.player;
        if (player != null) {
            boolean out = isOut(player);
            if (localOut && !out) {
                // The implant has sunk out of view: the held items come back up from below.
                if (holdsItems(player)) {
                    localUnstowTick = ticks;
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
                Anim self = anim(player.getId());
                localThrustReady = ticks + MantisRules.thrustCooldownTicks(self.variant);
                self.thrustStart = ticks;
                rigThrust(player.getId(), self);
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
