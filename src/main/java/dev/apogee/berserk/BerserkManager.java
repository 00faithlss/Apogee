package dev.apogee.berserk;

import dev.apogee.Apogee;
import dev.apogee.config.ApogeeServerConfig;
import dev.apogee.config.BerserkRules;
import dev.apogee.item.BerserkItem;
import dev.apogee.network.ApogeeNetworking.BerserkRulesSync;
import dev.apogee.network.ApogeeNetworking.BerserkStatus;
import dev.apogee.registry.ApogeeAttachments;
import dev.apogee.registry.ApogeeSounds;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Server authority for the Berserk: validates activation, owns timers and cooldowns, applies the
 * attribute bonuses, damage rules and the superhero landing, and keeps each wearer's client in
 * sync. Runs the same on an Essential/LAN host and on a dedicated server.
 */
public final class BerserkManager {
    public static final ResourceLocation ATTACK_SPEED_MODIFIER = Apogee.id("berserk_attack_speed");
    public static final ResourceLocation MOVEMENT_MODIFIER = Apogee.id("berserk_movement");
    public static final ResourceLocation FALL_DAMAGE_MODIFIER = Apogee.id("berserk_fall_damage");

    /** Downward speed of the superhero landing dive, in blocks per tick. */
    private static final double DIVE_SPEED = 1.6;
    /** Fall damage is ignored this many ticks after a landing, whichever packet arrives first. */
    private static final int LANDING_GRACE_TICKS = 10;
    private static final DustParticleOptions RAGE_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.08F, 0.12F), 1.1F);

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Game time of each wearer's last landing, for the fall damage grace. */
    private static final Map<UUID, Long> LANDED_AT = new HashMap<>();

    private BerserkManager() {
    }

    private static final class Session {
        final BerserkVariant variant;
        final long startTime;
        long endTime;
        float activationBonus;
        int comboHits;
        int comboCycle;
        long lastHitTime = Long.MIN_VALUE / 2;
        int kills;
        boolean diving;
        double diveStartY;

        Session(BerserkVariant variant, long startTime, long endTime) {
            this.variant = variant;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> resetState());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (UUID wearer : List.copyOf(SESSIONS.keySet())) {
                ServerPlayer player = server.getPlayerList().getPlayer(wearer);
                if (player != null) {
                    end(player, false);
                }
            }
            resetState();
        });
        ServerTickEvents.START_SERVER_TICK.register(BerserkManager::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            removeModifiers(player);
            if (ServerPlayNetworking.canSend(player, BerserkRulesSync.TYPE)) {
                ServerPlayNetworking.send(player, new BerserkRulesSync(rules()));
            }
            sendStatus(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> end(handler.getPlayer(), false));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> end(player, false));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            removeModifiers(newPlayer);
            sendStatus(newPlayer);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(BerserkManager::allowDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(BerserkManager::onDeath);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(BerserkManager::afterDamage);
    }

    public static BerserkRules rules() {
        return ApogeeServerConfig.berserkRules();
    }

    public static boolean isActive(Entity entity) {
        return entity instanceof ServerPlayer && SESSIONS.containsKey(entity.getUUID());
    }

    /** The variant running for {@code entity}, or {@code null} when it has no active Berserk. */
    public static BerserkVariant activeVariant(Entity entity) {
        Session session = entity instanceof ServerPlayer ? SESSIONS.get(entity.getUUID()) : null;
        return session == null ? null : session.variant;
    }

    /** The Berserk installed in the operating system slot, if any. */
    public static Optional<BerserkVariant> installed(LivingEntity entity) {
        return TrinketsApi.getTrinketComponent(entity)
                .flatMap(component -> component.getEquipped(stack -> stack.getItem() instanceof BerserkItem).stream().findFirst())
                .map(entry -> entry.getB())
                .map(ItemStack::getItem)
                .map(item -> ((BerserkItem) item).variant());
    }

    public static boolean isEquipped(LivingEntity entity) {
        return installed(entity).isPresent();
    }

    public static long cooldownEndTime(ServerPlayer player) {
        return player.getAttachedOrElse(ApogeeAttachments.BERSERK_COOLDOWN_END, 0L);
    }

    /** Game time at which the active Berserk ends, or -1 when inactive. */
    public static long activeEndTime(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session == null ? -1L : session.endTime;
    }

    public static int kills(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session == null ? 0 : session.kills;
    }

    public static boolean isDiving(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.diving;
    }

    // ------------------------------------------------------------------ activation

    /** Handles the operating system key when a Berserk is installed or running. */
    public static void toggle(ServerPlayer player) {
        if (SESSIONS.containsKey(player.getUUID())) {
            end(player, true);
            return;
        }
        long now = gameTime(player.server);
        Optional<BerserkVariant> variant = installed(player);
        boolean ready = player.isAlive()
                && !player.isSpectator()
                && variant.isPresent()
                && now >= cooldownEndTime(player);
        if (ready) {
            BerserkRules.Stats stats = rules().stats(variant.get());
            Session session = new Session(variant.get(), now, now + stats.durationTicks());
            session.activationBonus = lowHealthBonus(player.getHealth() / player.getMaxHealth(), stats.lowHealthDamagePercent());
            SESSIONS.put(player.getUUID(), session);
            applyModifiers(player, stats);
            playAt(player, ApogeeSounds.BERSERK_ACTIVATE, 1.0F);
        }
        sendStatus(player);
    }

    /**
     * Ends an activation (manual, expiry, death, unequip, dimension change or disconnect). A
     * living Militech wearer then heals a share of their maximum health for each kill.
     */
    public static void end(ServerPlayer player, boolean heal) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        removeModifiers(player);
        long now = gameTime(player.server);
        BerserkRules rules = rules();
        player.setAttached(ApogeeAttachments.BERSERK_COOLDOWN_END, now + rules.stats(session.variant).cooldownTicks());
        if (heal && session.variant == BerserkVariant.MILITECH && player.isAlive() && session.kills > 0) {
            float share = Math.min(1.0F, session.kills * rules.healPerKillPercent() / 100.0F);
            player.heal(player.getMaxHealth() * share);
        }
        if (player.isAlive()) {
            playAt(player, ApogeeSounds.BERSERK_DEACTIVATE, 1.0F);
        }
        sendStatus(player);
    }

    private static void applyModifiers(ServerPlayer player, BerserkRules.Stats stats) {
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER, activeVariant(player) == BerserkVariant.BIODYNE ? 0.0 : stats.attackSpeedPercent() / 100.0);
        setModifier(player, Attributes.MOVEMENT_SPEED, MOVEMENT_MODIFIER, stats.movementPercent() / 100.0);
        setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE_MODIFIER, -stats.fallDamageReductionPercent() / 100.0);
    }

    private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeModifiers(ServerPlayer player) {
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER, 0.0);
        setModifier(player, Attributes.MOVEMENT_SPEED, MOVEMENT_MODIFIER, 0.0);
        setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE_MODIFIER, 0.0);
    }

    // ------------------------------------------------------------------ tick

    private static void tick(MinecraftServer server) {
        long now = gameTime(server);
        for (UUID id : List.copyOf(SESSIONS.keySet())) {
            Session session = SESSIONS.get(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (session == null || player == null) {
                SESSIONS.remove(id);
                continue;
            }
            Optional<BerserkVariant> installed = installed(player);
            if (!player.isAlive() || player.isSpectator() || installed.isEmpty() || installed.get() != session.variant) {
                end(player, false);
                continue;
            }
            if (now >= session.endTime) {
                end(player, true);
                continue;
            }
            if (session.variant == BerserkVariant.ZETATECH) {
                tickLanding(player, session, now);
            } else if (session.variant == BerserkVariant.BIODYNE
                    && session.comboHits > 0 && now - session.lastHitTime > BerserkRules.COMBO_WINDOW_TICKS) {
                session.comboHits = 0;
                session.comboCycle = 0;
                setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER, 0.0);
                sendStatus(player);
            }
            if ((now - session.startTime) % 3L == 0L) {
                Vec3 center = player.getBoundingBox().getCenter();
                // Solo para los demás: en primera persona la cámara queda dentro de la nube.
                for (ServerPlayer other : player.serverLevel().players()) {
                    if (other != player && other.distanceToSqr(player) < 64.0 * 64.0) {
                        player.serverLevel().sendParticles(other, RAGE_DUST, false, center.x, center.y, center.z, 2, 0.3, 0.45, 0.3, 0.0);
                    }
                }
            }
        }
        if (now % 200L == 0L) {
            LANDED_AT.values().removeIf(time -> now - time > LANDING_GRACE_TICKS);
        }
    }

    /**
     * The superhero landing: holding sneak high enough in the air dives straight down, and
     * touching the ground sends out a shockwave. The wearer takes no damage from that fall.
     */
    private static void tickLanding(ServerPlayer player, Session session, long now) {
        BerserkRules rules = rules();
        if (!session.diving) {
            if (rules.slamRadius() > 0.0 && canStartDive(player, rules)) {
                session.diving = true;
                session.diveStartY = player.getY();
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x * 0.3, -DIVE_SPEED, motion.z * 0.3);
                player.hurtMarked = true;
                playAt(player, ApogeeSounds.BERSERK_DIVE, 1.0F);
            }
            return;
        }
        if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable() || player.getAbilities().flying) {
            session.diving = false;
            LANDED_AT.put(player.getUUID(), now);
            if (player.onGround()) {
                slam(player, rules, session.diveStartY - player.getY());
            }
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        if (motion.y > -DIVE_SPEED) {
            player.setDeltaMovement(motion.x, -DIVE_SPEED, motion.z);
            player.hurtMarked = true;
        }
    }

    private static boolean canStartDive(ServerPlayer player, BerserkRules rules) {
        if (!player.isShiftKeyDown() || player.onGround() || player.getAbilities().flying || player.isFallFlying()
                || player.isInWater() || player.isInLava() || player.onClimbable() || player.isPassenger()) {
            return false;
        }
        Vec3 feet = player.position();
        Vec3 below = feet.subtract(0.0, rules.slamMinHeight(), 0.0);
        HitResult hit = player.level().clip(new ClipContext(feet, below, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** The shockwave of a superhero landing; public for the game tests. */
    public static void slam(ServerPlayer player, BerserkRules rules, double dropped) {
        if (activeVariant(player) != BerserkVariant.ZETATECH || rules.slamRadius() <= 0.0) {
            return;
        }
        float power = BerserkRules.landingPower(dropped, rules.slamMinHeight());
        int enemiesHit = 0;
        ServerLevel level = player.serverLevel();
        double radius = rules.slamRadius();
        Vec3 center = player.position();
        AABB box = new AABB(center, center).inflate(radius, 2.0, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, entity -> isSlamTarget(player, entity))) {
            double distance = Math.sqrt(target.distanceToSqr(center.x, target.getY(), center.z));
            if (distance > radius) {
                continue;
            }
            float falloff = (float) (1.0 - 0.5 * distance / radius);
            if (!target.hurt(player.damageSources().playerAttack(player), rules.slamDamage() * falloff * power)) {
                continue;
            }
            if (!(target instanceof Animal)) {
                enemiesHit++;
            }
            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;
            double length = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
            double push = (0.6 + 0.8 * falloff) * power;
            target.push(dx / length * push, (0.35 + 0.25 * falloff) * power, dz / length * push);
            target.hurtMarked = true;
        }

        player.heal(player.getMaxHealth() * Math.min(BerserkRules.LANDING_HEAL_CAP_PERCENT,
                enemiesHit * BerserkRules.LANDING_HEAL_PERCENT) / 100.0F);
        BlockPos ground = player.blockPosition().below();
        BlockState state = level.getBlockState(ground);
        if (!state.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), center.x, center.y + 0.1, center.z,
                    90, radius * 0.35, 0.05, radius * 0.35, 0.25);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.3, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        int ring = 32;
        for (int i = 0; i < ring; i++) {
            double angle = Mth.TWO_PI * i / ring;
            level.sendParticles(ParticleTypes.DUST_PLUME, center.x + Math.cos(angle) * radius * 0.8, center.y + 0.1,
                    center.z + Math.sin(angle) * radius * 0.8, 1, 0.0, 0.0, 0.0, 0.0);
        }
        float volume = (float) Mth.clamp(0.8 + dropped / 20.0, 0.8, 1.6);
        playAt(player, ApogeeSounds.BERSERK_SLAM, volume);
    }

    private static boolean isSlamTarget(ServerPlayer wearer, LivingEntity entity) {
        if (entity == wearer || !entity.isAlive() || entity.isSpectator() || entity.isAlliedTo(wearer)) {
            return false;
        }
        if (entity instanceof ArmorStand) {
            return false;
        }
        return !(entity instanceof OwnableEntity pet) || !wearer.getUUID().equals(pet.getOwnerUUID());
    }

    // ------------------------------------------------------------------ damage hooks

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            Session session = SESSIONS.get(player.getUUID());
            Long landed = LANDED_AT.get(player.getUUID());
            if (session != null && session.diving
                    || landed != null && gameTime(player.server) - landed <= LANDING_GRACE_TICKS) {
                return false;
            }
        }
        BerserkVariant variant = activeVariant(player);
        return variant == null || !rules().stats(variant).invulnerable() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /**
     * Applied at the start of {@code hurt}: the wearer's damage reduction on incoming damage, and
     * the Biodyne's critical hits and the Militech's low-health bonus on the wearer's own melee.
     */
    public static float modifyDamage(LivingEntity victim, DamageSource source, float amount) {
        BerserkRules rules = rules();
        BerserkVariant defending = activeVariant(victim);
        if (defending != null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= 1.0F - rules.stats(defending).damageReductionPercent() / 100.0F;
        }
        Entity attacker = source.getEntity();
        BerserkVariant attacking = activeVariant(attacker);
        if (attacking == null || !source.is(DamageTypes.PLAYER_ATTACK) || source.getDirectEntity() != attacker) {
            return amount;
        }
        ServerPlayer player = (ServerPlayer) attacker;
        BerserkRules.Stats stats = rules.stats(attacking);
        Session session = SESSIONS.get(player.getUUID());
        int critChance = attacking == BerserkVariant.BIODYNE
                ? stats.critChancePercent() * session.comboHits / BerserkRules.COMBO_HITS
                : stats.critChancePercent();
        if (critChance > 0 && player.getRandom().nextInt(100) < critChance) {
            amount *= 1.0F + stats.critDamagePercent() / 100.0F;
            player.crit(victim);
            player.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        if (stats.lowHealthDamagePercent() > 0) {
            amount *= 1.0F + session.activationBonus;
        }
        return amount;
    }

    /** Bonus fraction for the Militech: none at full health, the whole bonus at 20% or less. */
    public static float lowHealthBonus(float healthFraction, int maxBonusPercent) {
        float missing = Mth.clamp((1.0F - healthFraction) / (1.0F - BerserkRules.LOW_HEALTH_FULL_BONUS), 0.0F, 1.0F);
        return missing * maxBonusPercent / 100.0F;
    }

    /**
     * The health a hit leaves the wearer with, after the variant's floor: an active Zetatech or
     * Biodyne keeps at least a quarter of the maximum (or what it had, if already below).
     */
    public static float floorHealth(ServerPlayer player, DamageSource source, float newHealth) {
        BerserkVariant variant = activeVariant(player);
        if (variant == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return newHealth;
        }
        int floorPercent = rules().stats(variant).healthFloorPercent();
        if (floorPercent <= 0) {
            return newHealth;
        }
        float floor = Math.min(player.getHealth(), player.getMaxHealth() * floorPercent / 100.0F);
        return Math.max(newHealth, Math.max(floor, 0.5F));
    }

    /** Sprinting, jumping and fighting cost no hunger while a Berserk runs. */
    public static boolean blocksExhaustion(Entity player) {
        return isActive(player);
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            end(player, false);
        }
        if (!(source.getEntity() instanceof ServerPlayer killer) || entity instanceof Animal || entity == killer || entity instanceof ArmorStand || entity.isAlliedTo(killer)
                || entity instanceof OwnableEntity pet && killer.getUUID().equals(pet.getOwnerUUID())) {
            return;
        }
        Session session = SESSIONS.get(killer.getUUID());
        if (session != null) {
            session.kills++;
            if (session.variant == BerserkVariant.MILITECH) {
                long cap = session.startTime + rules().militech().durationTicks() * 2L;
                session.endTime = Math.min(cap, session.endTime + BerserkRules.KILL_EXTENSION_TICKS);
            }
            sendStatus(killer);
        }
    }

    /** Only successful melee damage advances a combo; one step per tick also covers implant AoE. */
    private static void afterDamage(LivingEntity victim, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer player)
                || source.getDirectEntity() != player || !source.is(DamageTypes.PLAYER_ATTACK)
                || victim == player || victim instanceof Animal || victim instanceof ArmorStand
                || victim.isAlliedTo(player)
                || victim instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID())) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.variant != BerserkVariant.BIODYNE) {
            return;
        }
        long now = gameTime(player.server);
        if (now == session.lastHitTime) {
            return;
        }
        if (now - session.lastHitTime > BerserkRules.COMBO_WINDOW_TICKS) {
            session.comboHits = 0;
            session.comboCycle = 0;
        }
        session.lastHitTime = now;
        session.comboHits = Math.min(BerserkRules.COMBO_HITS, session.comboHits + 1);
        if (++session.comboCycle >= BerserkRules.COMBO_HITS) {
            session.comboCycle = 0;
            player.heal(player.getMaxHealth() * BerserkRules.COMBO_HEAL_PERCENT / 100.0F);
        }
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER,
                rules().biodyne().attackSpeedPercent() / 100.0 * session.comboHits / BerserkRules.COMBO_HITS);
        sendStatus(player);
    }

    public static int comboHits(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session == null ? 0 : session.comboHits;
    }

    // ------------------------------------------------------------------ sync and helpers

    public static void sendStatus(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, BerserkStatus.TYPE)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        BerserkStatus status = session == null
                ? new BerserkStatus(false, installed(player).map(Enum::ordinal).orElse(-1), 0L, 0L, cooldownEndTime(player), 0)
                : new BerserkStatus(true, session.variant.ordinal(), session.startTime, session.endTime, cooldownEndTime(player), session.kills, session.comboHits);
        ServerPlayNetworking.send(player, status);
    }

    private static void playAt(ServerPlayer player, SoundEvent sound, float volume) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, 1.0F);
    }

    private static long gameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static void resetState() {
        SESSIONS.clear();
        LANDED_AT.clear();
    }
}
