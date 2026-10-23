package dev.apogee.sandevistan;

import dev.apogee.Apogee;
import dev.apogee.berserk.BerserkManager;
import dev.apogee.config.ApogeeServerConfig;
import dev.apogee.config.SandevistanRules;
import dev.apogee.network.ApogeeNetworking.RulesSync;
import dev.apogee.network.ApogeeNetworking.TimeFieldSync;
import dev.apogee.network.ApogeeNetworking.ToggleSandevistan;
import dev.apogee.network.ApogeeNetworking.WearerStatus;
import dev.apogee.registry.ApogeeAttachments;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.time.TimeScaledEntity;
import dev.emi.trinkets.api.TrinketsApi;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server authority for the Sandevistan: validates activation requests, owns timers and
 * cooldowns, selects which entities are slowed each tick, applies bonuses and keeps clients
 * in sync. Runs identically on an Essential/LAN host's integrated server and on a dedicated one.
 */
public final class SandevistanManager {
    public static final ResourceLocation SPEED_MODIFIER = Apogee.id("sandevistan_movement");

    private static final Map<UUID, ActiveSession> SESSIONS = new HashMap<>();
    private static final CombatMemory COMBAT = new CombatMemory();
    private static Map<ServerLevel, Set<Entity>> slowedByLevel = new IdentityHashMap<>();
    private static final Map<ServerLevel, TimeFieldSync> LAST_SENT = new IdentityHashMap<>();
    private static final Set<UUID> PENDING_RESYNC = new LinkedHashSet<>();

    private SandevistanManager() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ToggleSandevistan.TYPE, (payload, context) -> {
            // One key for the operating system slot, whichever implant it holds.
            ServerPlayer player = context.player();
            if (BerserkManager.isActive(player) || BerserkManager.isEquipped(player)) {
                BerserkManager.toggle(player);
            } else {
                toggle(player);
            }
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ApogeeServerConfig.load();
            resetState();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (UUID wearer : List.copyOf(SESSIONS.keySet())) {
                ServerPlayer player = server.getPlayerList().getPlayer(wearer);
                if (player != null) {
                    end(player);
                }
            }
            resetState();
        });
        ServerTickEvents.START_SERVER_TICK.register(SandevistanManager::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            removeSpeedModifier(player);
            if (ServerPlayNetworking.canSend(player, RulesSync.TYPE)) {
                ServerPlayNetworking.send(player, new RulesSync(rules()));
            }
            PENDING_RESYNC.add(player.getUUID());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> end(handler.getPlayer()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            end(player);
            PENDING_RESYNC.add(player.getUUID());
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            removeSpeedModifier(newPlayer);
            PENDING_RESYNC.add(newPlayer.getUUID());
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) ->
                recordCombat(entity, source));
        ServerLivingEntityEvents.AFTER_DEATH.register(SandevistanManager::onDeath);
    }

    public static SandevistanRules rules() {
        return ApogeeServerConfig.rules();
    }

    public static boolean isActive(Entity entity) {
        return entity instanceof ServerPlayer && SESSIONS.containsKey(entity.getUUID());
    }

    /** Server game time at which the player's current activation ends, or -1 when inactive. */
    public static long activeEndTime(ServerPlayer player) {
        ActiveSession session = SESSIONS.get(player.getUUID());
        return session == null ? -1L : session.endTime;
    }

    public static long cooldownEndTime(ServerPlayer player) {
        return cooldownEnd(player);
    }

    public static boolean isEquipped(LivingEntity entity) {
        return TrinketsApi.getTrinketComponent(entity)
                .map(component -> component.isEquipped(ApogeeItems.SANDEVISTAN))
                .orElse(false);
    }

    // ------------------------------------------------------------------ activation

    /** Handles a validated-by-server toggle request from the player's key press. */
    public static void toggle(ServerPlayer player) {
        if (SESSIONS.containsKey(player.getUUID())) {
            end(player);
            return;
        }
        long now = gameTime(player.server);
        SandevistanRules rules = rules();
        boolean ready = player.isAlive()
                && !player.isSpectator()
                && isEquipped(player)
                && now >= cooldownEnd(player);
        if (ready) {
            ActiveSession session = new ActiveSession(player.getUUID(), now, now + rules.durationTicks(), now + rules.maxDurationTicks());
            SESSIONS.put(player.getUUID(), session);
            updateSpeedModifier(player, session, rules, now);
        }
        sendStatus(player);
    }

    /**
     * Eases the movement bonus in over the ramp so the speed-driven FOV change is gradual
     * instead of a jump. Only touches the attribute when the value actually changes.
     */
    private static void updateSpeedModifier(ServerPlayer player, ActiveSession session, SandevistanRules rules, long now) {
        double progress = rules.movementRampTicks() <= 0 ? 1.0 : Math.min(1.0, (now - session.startTime) / (double) rules.movementRampTicks());
        double eased = progress * progress * (3.0 - 2.0 * progress);
        double amount = rules.movementMultiplier() * eased;
        if (amount == session.appliedSpeedBonus) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER);
            speed.addTransientModifier(new AttributeModifier(SPEED_MODIFIER, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        session.appliedSpeedBonus = amount;
    }

    /** Ends an activation (manual, expiry, death, unequip, dimension change or disconnect). */
    public static void end(ServerPlayer player) {
        ActiveSession session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        removeSpeedModifier(player);
        long now = gameTime(player.server);
        player.setAttached(ApogeeAttachments.COOLDOWN_END, now + rules().cooldownTicks());
        sendStatus(player);
    }

    private static void removeSpeedModifier(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER);
        }
    }

    private static long cooldownEnd(ServerPlayer player) {
        return player.getAttachedOrElse(ApogeeAttachments.COOLDOWN_END, 0L);
    }

    private static long gameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    // ------------------------------------------------------------------ tick

    /** Wall time of the last manager tick, for performance checks. */
    public static long lastTickNanos() {
        return lastTickNanos;
    }

    private static long lastTickNanos;

    private static void tick(MinecraftServer server) {
        long started = System.nanoTime();
        tickFields(server);
        lastTickNanos = System.nanoTime() - started;
    }

    private static void tickFields(MinecraftServer server) {
        long now = gameTime(server);
        SandevistanRules rules = rules();

        for (ActiveSession session : List.copyOf(SESSIONS.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.wearer);
            if (player == null) {
                SESSIONS.remove(session.wearer);
            } else if (!player.isAlive() || player.isSpectator() || !isEquipped(player) || now >= session.endTime) {
                end(player);
            } else {
                updateSpeedModifier(player, session, rules, now);
            }
        }

        Map<ServerLevel, Set<Entity>> next = new IdentityHashMap<>();
        Map<ServerLevel, IntArrayList> wearers = new IdentityHashMap<>();
        for (ActiveSession session : SESSIONS.values()) {
            ServerPlayer wearer = server.getPlayerList().getPlayer(session.wearer);
            if (wearer == null) {
                continue;
            }
            ServerLevel level = wearer.serverLevel();
            wearers.computeIfAbsent(level, ignored -> new IntArrayList()).add(wearer.getId());
            collectField(wearer, session, rules, now, next.computeIfAbsent(level, ignored -> new LinkedHashSet<>()));
        }

        for (Map.Entry<ServerLevel, Set<Entity>> entry : slowedByLevel.entrySet()) {
            Set<Entity> stillSlowed = next.getOrDefault(entry.getKey(), Collections.emptySet());
            for (Entity entity : entry.getValue()) {
                if (!stillSlowed.contains(entity)) {
                    TimeScaledEntity.of(entity).apogee$setSlowed(false);
                }
            }
        }
        for (Set<Entity> entities : next.values()) {
            for (Entity entity : entities) {
                TimeScaledEntity.of(entity).apogee$setSlowed(true);
            }
        }
        slowedByLevel = next;

        for (ServerLevel level : server.getAllLevels()) {
            TimeFieldSync snapshot = snapshot(next.get(level), wearers.get(level));
            TimeFieldSync previous = LAST_SENT.get(level);
            if (previous == null || !previous.equals(snapshot)) {
                if (previous != null || !snapshot.slowedEntities().isEmpty() || !snapshot.activeWearers().isEmpty()) {
                    for (ServerPlayer player : level.players()) {
                        if (ServerPlayNetworking.canSend(player, TimeFieldSync.TYPE)) {
                            ServerPlayNetworking.send(player, snapshot);
                        }
                    }
                }
                LAST_SENT.put(level, snapshot);
            }
        }

        if (!PENDING_RESYNC.isEmpty()) {
            for (UUID id : List.copyOf(PENDING_RESYNC)) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null && ServerPlayNetworking.canSend(player, TimeFieldSync.TYPE)) {
                    ServerLevel level = player.serverLevel();
                    ServerPlayNetworking.send(player, snapshot(next.get(level), wearers.get(level)));
                    sendStatus(player);
                }
            }
            PENDING_RESYNC.clear();
        }

        if (now % 100L == 0L) {
            COMBAT.prune(now);
        }
    }

    private static void collectField(ServerPlayer wearer, ActiveSession session, SandevistanRules rules, long now, Set<Entity> out) {
        double radius = rules.fieldRadius();
        double radiusSqr = radius * radius;
        Vec3 center = wearer.getBoundingBox().getCenter();
        AABB box = new AABB(center, center).inflate(radius);
        List<Entity> nearby = wearer.serverLevel().getEntities(wearer, box, Entity::isAlive);

        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living) || entity.getBoundingBox().distanceToSqr(center) > radiusSqr) {
                continue;
            }
            if (isValidTarget(wearer, living, rules, now)) {
                targets.add(living);
            }
        }
        for (LivingEntity target : targets) {
            session.affected.add(target.getUUID());
            out.add(target);
        }
        for (Entity entity : nearby) {
            if (entity instanceof Projectile projectile && entity.getBoundingBox().distanceToSqr(center) <= radiusSqr) {
                Entity owner = projectile.getOwner();
                // Ownerless projectiles (dispensers) keep their normal rhythm.
                if (owner != null && (owner == wearer || targets.contains(owner))) {
                    out.add(projectile);
                }
            }
        }
    }

    private static boolean isValidTarget(ServerPlayer wearer, LivingEntity entity, SandevistanRules rules, long now) {
        if (SESSIONS.containsKey(entity.getUUID())) {
            return false; // Active wearers are immune to every field.
        }
        if (entity instanceof ServerPlayer player) {
            return player.gameMode.isSurvival();
        }
        if (entity instanceof Mob mob) {
            boolean targeting = mob.getTarget() == wearer;
            if (mob instanceof Enemy) {
                return targeting || COMBAT.recent(mob.getUUID(), wearer.getUUID(), now, rules.combatMemoryTicks());
            }
            return mob instanceof NeutralMob && targeting;
        }
        return false;
    }

    private static TimeFieldSync snapshot(Set<Entity> slowed, IntArrayList wearers) {
        IntArrayList slowedIds = new IntArrayList();
        if (slowed != null) {
            for (Entity entity : slowed) {
                slowedIds.add(entity.getId());
            }
        }
        slowedIds.sort(null);
        IntArrayList wearerIds = wearers == null ? new IntArrayList() : new IntArrayList(wearers);
        wearerIds.sort(null);
        return new TimeFieldSync(slowedIds, wearerIds);
    }

    // ------------------------------------------------------------------ combat hooks

    private static void recordCombat(LivingEntity victim, DamageSource source) {
        Entity attacker = source.getEntity();
        long now = gameTime(victim.getServer());
        if (attacker instanceof ServerPlayer player && victim instanceof Mob) {
            COMBAT.record(victim.getUUID(), player.getUUID(), now);
        } else if (victim instanceof ServerPlayer player && attacker instanceof Mob) {
            COMBAT.record(attacker.getUUID(), player.getUUID(), now);
        }
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            end(player);
        }
        if (!(source.getEntity() instanceof ServerPlayer killer) || entity instanceof Animal) {
            return;
        }
        ActiveSession session = SESSIONS.get(killer.getUUID());
        if (session != null && session.affected.contains(entity.getUUID())) {
            session.endTime = Math.min(session.endTime + rules().killExtensionTicks(), session.maxEndTime);
            sendStatus(killer);
        }
    }

    /** Melee hits and projectiles launched during an activation deal bonus damage. */
    public static float modifyDamage(LivingEntity victim, DamageSource source, float amount) {
        Entity direct = source.getDirectEntity();
        boolean melee = source.is(DamageTypes.PLAYER_ATTACK) && direct == source.getEntity() && isActive(direct);
        boolean boostedProjectile = direct instanceof Projectile
                && Boolean.TRUE.equals(direct.getAttached(ApogeeAttachments.BOOSTED_PROJECTILE));
        return melee || boostedProjectile ? amount * rules().damageMultiplier() : amount;
    }

    public static void onEntitySpawn(Entity entity) {
        if (entity instanceof Projectile projectile && isActive(projectile.getOwner())) {
            projectile.setAttached(ApogeeAttachments.BOOSTED_PROJECTILE, true);
        }
    }

    // ------------------------------------------------------------------ sync

    private static void sendStatus(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, WearerStatus.TYPE)) {
            return;
        }
        ActiveSession session = SESSIONS.get(player.getUUID());
        WearerStatus status = session == null
                ? new WearerStatus(false, 0L, 0L, 0L, cooldownEnd(player))
                : new WearerStatus(true, session.startTime, session.endTime, session.maxEndTime, cooldownEnd(player));
        ServerPlayNetworking.send(player, status);
    }

    private static void resetState() {
        for (Set<Entity> entities : slowedByLevel.values()) {
            for (Entity entity : entities) {
                TimeScaledEntity.of(entity).apogee$setSlowed(false);
            }
        }
        SESSIONS.clear();
        slowedByLevel = new IdentityHashMap<>();
        LAST_SENT.clear();
        PENDING_RESYNC.clear();
        COMBAT.clear();
    }
}
