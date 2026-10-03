package dev.apogee.mantis;

import dev.apogee.Apogee;
import dev.apogee.network.ApogeeNetworking.MantisSwing;
import dev.apogee.network.ApogeeNetworking.MantisSync;
import dev.apogee.network.ApogeeNetworking.MantisThrust;
import dev.apogee.network.ApogeeNetworking.ToggleMantis;
import dev.apogee.item.ArmCyberwareItem;
import dev.apogee.registry.ApogeeSounds;
import dev.apogee.registry.ApogeeEffects;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server authority for the Mantis Blades: validates deploy/retract requests, alternates the
 * slashing arm, runs the two-blade lunge, pins the hotbar slot while the blades are out and
 * tells clients (the wearer and everyone tracking them) what to draw.
 */
public final class MantisManager {
    public static final ResourceLocation DAMAGE_MODIFIER = Apogee.id("mantis_damage");
    public static final ResourceLocation SPEED_MODIFIER = Apogee.id("mantis_attack_speed");
    public static final ResourceLocation REACH_MODIFIER = Apogee.id("nanowire_reach");

    private static final Map<UUID, Deployment> DEPLOYED = new HashMap<>();
    /** Game time before which the wearer cannot lunge again. Kept across deploy cycles. */
    private static final Map<UUID, Long> THRUST_READY = new HashMap<>();
    /** Game time each wearer's two-wire lash reaches its targets. */
    private static final Map<UUID, Long> PENDING_LASHES = new HashMap<>();
    private static final Map<UUID, PendingCut> PENDING_CUTS = new HashMap<>();

    private record PendingCut(long tick, float damage, boolean critical) {
    }

    private MantisManager() {
    }

    private static final class Deployment {
        final int lockedSlot;
        final MantisVariant variant;
        int nextArm = MantisRules.ARM_RIGHT;
        long lastSwingTick = Long.MIN_VALUE / 2;
        long heavyUntil = Long.MIN_VALUE / 2;

        Deployment(int lockedSlot, MantisVariant variant) {
            this.lockedSlot = lockedSlot;
            this.variant = variant;
        }
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ToggleMantis.TYPE, (payload, context) -> toggle(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(MantisThrust.TYPE, (payload, context) -> thrust(context.player()));

        ServerTickEvents.START_SERVER_TICK.register(MantisManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (UUID id : List.copyOf(DEPLOYED.keySet())) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    retract(player, false);
                }
            }
            DEPLOYED.clear();
            THRUST_READY.clear();
            PENDING_LASHES.clear();
            PENDING_CUTS.clear();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> retract(handler.getPlayer(), false));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> retract(player, false));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> removeModifiers(newPlayer));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                retract(player, false);
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!blocked && damageTaken > 0.0F) {
                onDamage(entity, source);
            }
        });
        EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
            if (tracked instanceof ServerPlayer wearer && DEPLOYED.containsKey(wearer.getUUID())
                    && ServerPlayNetworking.canSend(viewer, MantisSync.TYPE)) {
                ServerPlayNetworking.send(viewer, new MantisSync(wearer.getId(), true, DEPLOYED.get(wearer.getUUID()).variant.ordinal()));
            }
        });
    }

    public static boolean isDeployed(ServerPlayer player) {
        return DEPLOYED.containsKey(player.getUUID());
    }

    public static boolean isEquipped(LivingEntity entity) {
        return equippedVariant(entity) != null;
    }

    /** Variant of the equipped Mantis Blades, or null when none are worn. */
    public static MantisVariant equippedVariant(LivingEntity entity) {
        return TrinketsApi.getTrinketComponent(entity)
                .flatMap(component -> component.getEquipped(stack -> stack.getItem() instanceof ArmCyberwareItem).stream()
                        .map(pair -> ((ArmCyberwareItem) pair.getB().getItem()).variant())
                        .findFirst())
                .orElse(null);
    }

    // ------------------------------------------------------------------ deploy

    public static void toggle(ServerPlayer player) {
        if (DEPLOYED.containsKey(player.getUUID())) {
            retract(player, true);
            return;
        }
        MantisVariant variant = equippedVariant(player);
        if (!player.isAlive() || player.isSpectator() || variant == null) {
            return;
        }
        DEPLOYED.put(player.getUUID(), new Deployment(player.getInventory().selected, variant));
        addModifiers(player);
        play(player, switch (variant) {
            case ELECTRIC -> ApogeeSounds.MANTIS_ELECTRIC_DEPLOY;
            case NANOWIRE, NANOWIRE_WHITE -> ApogeeSounds.NANOWIRE_DEPLOY;
            default -> ApogeeSounds.MANTIS_DEPLOY;
        });
        broadcast(player, true, variant);
    }

    /** Folds the blades away (manual, unequip, death, dimension change, disconnect). */
    public static void retract(ServerPlayer player, boolean audible) {
        Deployment deployment = DEPLOYED.remove(player.getUUID());
        if (deployment == null) {
            return;
        }
        PENDING_LASHES.remove(player.getUUID());
        PENDING_CUTS.remove(player.getUUID());
        removeModifiers(player);
        if (audible) {
            play(player, deployment.variant.isNanowire() ? ApogeeSounds.NANOWIRE_RETRACT : ApogeeSounds.MANTIS_RETRACT);
        }
        broadcast(player, false, MantisVariant.STANDARD);
    }

    /**
     * Sets the wearer's attack damage and speed to the blades' own values. Whatever the item left
     * in the locked slot adds to them is subtracted, so a sword in hand does not stack on top of
     * the blades; effects such as Strength still count. Run on deploy and every tick after (the
     * locked slot can still change, for instance when an item is picked up into it).
     */
    private static void addModifiers(ServerPlayer player) {
        Deployment deployment = DEPLOYED.get(player.getUUID());
        boolean wire = deployment != null && deployment.variant.isNanowire();
        double[] held = heldBonuses(player);
        setModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER,
                (wire ? NanowireRules.ATTACK_DAMAGE : MantisRules.ATTACK_DAMAGE) - held[0]);
        setModifier(player.getAttribute(Attributes.ATTACK_SPEED), SPEED_MODIFIER,
                (wire ? NanowireRules.ATTACK_SPEED : MantisRules.ATTACK_SPEED) - held[1]);
        AttributeInstance reach = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (wire && reach != null && reach.getModifier(REACH_MODIFIER) == null) {
            // The wire lashes further than an arm reaches.
            reach.addTransientModifier(new AttributeModifier(REACH_MODIFIER, NanowireRules.REACH_BONUS, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** Flat attack damage and speed the main-hand item adds on top of the attributes' base values. */
    private static double[] heldBonuses(ServerPlayer player) {
        double[] held = new double[2];
        player.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (modifier.operation() != AttributeModifier.Operation.ADD_VALUE) {
                return;
            }
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
                held[0] += modifier.amount();
            } else if (attribute.is(Attributes.ATTACK_SPEED)) {
                held[1] += modifier.amount();
            }
        });
        return held;
    }

    private static void setModifier(AttributeInstance instance, ResourceLocation id, double target) {
        if (instance == null) {
            return;
        }
        double amount = target - instance.getBaseValue();
        AttributeModifier current = instance.getModifier(id);
        if (current == null || current.amount() != amount) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** Damage the blades' variant adds to a cut or lunge against {@code target}. */
    public static float variantBonus(ServerPlayer attacker, Entity target) {
        Deployment deployment = DEPLOYED.get(attacker.getUUID());
        if (deployment != null && deployment.variant == MantisVariant.ELECTRIC && target.isInWater()) {
            return MantisRules.ELECTRIC_WATER_BONUS;
        }
        return 0.0F;
    }

    private static void removeModifiers(ServerPlayer player) {
        AttributeInstance reach = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach != null) {
            reach.removeModifier(REACH_MODIFIER);
        }
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.removeModifier(DAMAGE_MODIFIER);
        }
        AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER);
        }
    }

    private static void tick(MinecraftServer server) {
        if (DEPLOYED.isEmpty()) {
            return;
        }
        cuts(server);
        lashes(server);
        for (Map.Entry<UUID, Deployment> entry : List.copyOf(DEPLOYED.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive() || player.isSpectator() || !isEquipped(player)) {
                if (player != null) {
                    retract(player, true);
                } else {
                    DEPLOYED.remove(entry.getKey());
                }
                continue;
            }
            addModifiers(player);
            int locked = entry.getValue().lockedSlot;
            if (player.getInventory().selected != locked) {
                player.getInventory().selected = locked;
                player.connection.send(new ClientboundSetCarriedItemPacket(locked));
            }
            if (player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
        }
    }

    // ------------------------------------------------------------------ combat

    /** Called on the server whenever a deployed wearer starts a swing; picks the alternating arm. */
    public static void onSwing(ServerPlayer player) {
        Deployment deployment = DEPLOYED.get(player.getUUID());
        if (deployment == null || !player.isAlive() || player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        if (deployment.variant.isNanowire() && now < deployment.heavyUntil) {
            return;
        }
        // Mining swings every tick while the button is held: one cut (and sound) at a time.
        if (now - deployment.lastSwingTick < MantisRules.slashIntervalTicks(deployment.variant)) {
            return;
        }
        deployment.lastSwingTick = now;
        if (deployment.variant.isNanowire()) {
            float charge = player.getAttackStrengthScale(0.5F);
            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.2F + charge * charge * 0.8F);
            boolean critical = charge > 0.9F && player.fallDistance > 0.0F && !player.onGround()
                    && !player.onClimbable() && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS)
                    && !player.isPassenger() && !player.isSprinting();
            PENDING_CUTS.put(player.getUUID(), new PendingCut(now + NanowireRules.LASH_HIT_DELAY_TICKS,
                    damage * (critical ? 1.5F : 1.0F), critical));
            player.resetAttackStrengthTicker();
        }
        int arm = deployment.nextArm;
        deployment.nextArm = arm == MantisRules.ARM_RIGHT ? MantisRules.ARM_LEFT : MantisRules.ARM_RIGHT;
        play(player, switch (deployment.variant) {
            case ELECTRIC -> ApogeeSounds.MANTIS_ELECTRIC_SWING;
            case NANOWIRE, NANOWIRE_WHITE -> ApogeeSounds.NANOWIRE_SWING;
            default -> ApogeeSounds.MANTIS_SWING;
        });
        sendToTrackers(player, new MantisSwing(player.getId(), arm), false);
    }

    private static void onDamage(LivingEntity victim, DamageSource source) {
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof ServerPlayer attacker) {
            Deployment deployment = DEPLOYED.get(attacker.getUUID());
            if (deployment != null) {
                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), switch (deployment.variant) {
                            case ELECTRIC -> ApogeeSounds.MANTIS_ELECTRIC_HIT;
                            case NANOWIRE, NANOWIRE_WHITE -> ApogeeSounds.NANOWIRE_HIT;
                            default -> ApogeeSounds.MANTIS_HIT;
                        }, SoundSource.PLAYERS, 1.0F, 1.0F);
                applyVariantEffect(victim, deployment.variant);
            }
        }
    }

    /** Thermal blades set targets alight; electric blades stun them briefly (and hit harder in water, see {@link #variantBonus}). */
    private static void applyVariantEffect(LivingEntity victim, MantisVariant variant) {
        if (variant.isNanowire()) {
            victim.addEffect(new MobEffectInstance(ApogeeEffects.BLEEDING, NanowireRules.BLEED_TICKS, 0, false, false, true));
        } else if (variant == MantisVariant.THERMAL) {
            victim.igniteForSeconds(MantisRules.THERMAL_BURN_SECONDS);
        } else if (variant == MantisVariant.ELECTRIC) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MantisRules.ELECTRIC_STUN_TICKS, MantisRules.ELECTRIC_STUN_AMPLIFIER));
        }
    }

    /**
     * The two-arm attack: the blades' lunge (a short forward impulse and a piercing cut along the
     * line of sight) or the nanowire's two-wire lash (both wires sweep the arc in front).
     */
    public static void thrust(ServerPlayer player) {
        Deployment deployment = DEPLOYED.get(player.getUUID());
        if (deployment == null || !player.isAlive()) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < THRUST_READY.getOrDefault(player.getUUID(), 0L)) {
            return;
        }
        THRUST_READY.put(player.getUUID(), now + MantisRules.thrustCooldownTicks(deployment.variant));
        if (deployment.variant.isNanowire()) {
            PENDING_CUTS.remove(player.getUUID());
            deployment.heavyUntil = now + NanowireRules.HEAVY_TICKS;
            // The wires reach the targets partway through the lash.
            PENDING_LASHES.put(player.getUUID(), now + NanowireRules.HEAVY_HIT_DELAY_TICKS);
            play(player, ApogeeSounds.NANOWIRE_HEAVY);
            sendToTrackers(player, new MantisSwing(player.getId(), MantisRules.ARM_BOTH), false);
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() > 1.0E-4) {
            Vec3 push = flat.normalize().scale(MantisRules.THRUST_IMPULSE);
            player.setDeltaMovement(player.getDeltaMovement().add(push.x, player.onGround() ? 0.12 : 0.0, push.z));
            player.hurtMarked = true;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(look.scale(MantisRules.THRUST_REACH));
        AABB sweep = player.getBoundingBox().expandTowards(look.scale(MantisRules.THRUST_REACH)).inflate(1.0);
        DamageSource source = player.damageSources().playerAttack(player);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, sweep,
                e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable())) {
            if (target.getBoundingBox().inflate(0.5).clip(eye, end).isPresent()) {
                target.hurt(source, MantisRules.THRUST_DAMAGE + variantBonus(player, target));
            }
        }
        // A slash with a deeper hit on top, louder than a single cut: electric or plain blades.
        if (DEPLOYED.get(player.getUUID()).variant == MantisVariant.ELECTRIC) {
            play(player, ApogeeSounds.MANTIS_ELECTRIC_SWING);
            play(player, ApogeeSounds.MANTIS_ELECTRIC_THRUST);
        } else {
            play(player, ApogeeSounds.MANTIS_SWING);
            play(player, ApogeeSounds.MANTIS_THRUST);
        }
        sendToTrackers(player, new MantisSwing(player.getId(), MantisRules.ARM_BOTH), false);
    }

    /** Vanilla entity attacks and air swings both enter the same single-wire sweep. */
    public static boolean isNanowireDeployed(ServerPlayer player) {
        Deployment deployment = DEPLOYED.get(player.getUUID());
        return deployment != null && deployment.variant.isNanowire();
    }

    private static void cuts(MinecraftServer server) {
        for (Map.Entry<UUID, PendingCut> entry : List.copyOf(PENDING_CUTS.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !isNanowireDeployed(player)) {
                PENDING_CUTS.remove(entry.getKey());
            } else if (player.level().getGameTime() >= entry.getValue().tick()) {
                PENDING_CUTS.remove(entry.getKey());
                sweep(player, entry.getValue().damage(), entry.getValue().critical());
            }
        }
    }

    /** One hit per eligible target in the cable's forward sweep, including air swings. */
    private static void sweep(ServerPlayer player, float damage, boolean critical) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        double reach = player.entityInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 flat = new Vec3(player.getLookAngle().x, 0.0, player.getLookAngle().z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        flat = flat.normalize();
        double minCos = Math.cos(Math.toRadians(NanowireRules.SWEEP_HALF_ANGLE)) - 1.0E-7;
        DamageSource source = player.damageSources().playerAttack(player);
        boolean hit = false;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable() && !player.isAlliedTo(e))) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
            Vec3 toFlat = new Vec3(to.x, 0.0, to.z);
            double distance = target.getBoundingBox().distanceToSqr(eye);
            if (distance > reach * reach || (toFlat.lengthSqr() >= 1.0E-4 && toFlat.normalize().dot(flat) < minCos)
                    || !player.hasLineOfSight(target)) {
                continue;
            }
            if (target.hurt(source, damage)) {
                hit = true;
                if (critical) {
                    player.crit(target);
                }
            }
        }
        if (hit) {
            player.causeFoodExhaustion(0.1F);
        }
    }

    /** Lands the two-wire lashes that are due: every living target in the arc in front, within reach. */
    private static void lashes(MinecraftServer server) {
        if (PENDING_LASHES.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Long> entry : List.copyOf(PENDING_LASHES.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !DEPLOYED.containsKey(entry.getKey())) {
                PENDING_LASHES.remove(entry.getKey());
                continue;
            }
            if (player.level().getGameTime() < entry.getValue()) {
                continue;
            }
            PENDING_LASHES.remove(entry.getKey());
            sweep(player, NanowireRules.HEAVY_DAMAGE, false);
        }
    }

    // ------------------------------------------------------------------ sync

    private static void broadcast(ServerPlayer player, boolean deployed, MantisVariant variant) {
        sendToTrackers(player, new MantisSync(player.getId(), deployed, variant.ordinal()), true);
    }

    private static void sendToTrackers(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload,
                                       boolean includeSelf) {
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            if (ServerPlayNetworking.canSend(viewer, payload.type())) {
                ServerPlayNetworking.send(viewer, payload);
            }
        }
        if (includeSelf && ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void play(ServerPlayer player, SoundEvent sound) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
