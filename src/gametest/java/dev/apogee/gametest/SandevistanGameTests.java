package dev.apogee.gametest;

import dev.apogee.Apogee;
import dev.apogee.config.SandevistanRules;
import dev.apogee.registry.ApogeeAttachments;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.sandevistan.SandevistanManager;
import dev.apogee.time.TimeScaledEntity;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side checks of the Sandevistan rules with mock players. Every test runs in its own
 * batch so no two fields overlap. Run with {@code gradlew runGametest}.
 */
public final class SandevistanGameTests implements FabricGameTest {
    private static final int SAMPLE_TICKS = 100;

    // ------------------------------------------------------------------ helpers

    private static ServerPlayer player(GameTestHelper helper, double x, double z, GameType mode) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(x, 2.0, z));
        player.teleportTo(helper.getLevel(), pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setGameMode(mode);
        return player;
    }

    private static ServerPlayer wearer(GameTestHelper helper, double x, double z) {
        ServerPlayer player = player(helper, x, z, GameType.SURVIVAL);
        helper.assertTrue(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.SANDEVISTAN)), "Sandevistan should equip into cyberware/os");
        return player;
    }

    private static <T extends Mob> T mob(GameTestHelper helper, EntityType<T> type, double x, double z) {
        T mob = helper.spawn(type, new Vec3(x, 2.0, z));
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        return mob;
    }

    private static void cleanup(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer player : players) {
            SandevistanManager.end(player);
            player.getServer().getPlayerList().remove(player);
        }
        helper.killAllEntities();
    }

    private static boolean slowed(LivingEntity entity) {
        return TimeScaledEntity.of(entity).apogee$isSlowed();
    }

    private static SandevistanRules rules() {
        return SandevistanManager.rules();
    }

    private static long now(GameTestHelper helper) {
        return helper.getLevel().getServer().overworld().getGameTime();
    }

    // ------------------------------------------------------------------ crafting and equipment

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_recipes")
    public void recipesProduceOneItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Map<String, Integer> expectedIngredients = Map.of(
                "os_core_t5", 9, "apogee_overdrive_kernel_t5", 9, "quantum_sync_lattice_t5", 9, "sandevistan", 9);
        expectedIngredients.forEach((name, count) -> {
            Optional<RecipeHolder<?>> recipe = level.getRecipeManager().byKey(Apogee.id(name));
            helper.assertTrue(recipe.isPresent(), "Missing recipe apogee:" + name);
            ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
            helper.assertTrue(result.getCount() == 1, name + " should yield one item");
            helper.assertTrue(recipe.get().value().getIngredients().size() == count, name + " ingredient count");
        });
        helper.assertTrue(new ItemStack(ApogeeItems.SANDEVISTAN).getMaxStackSize() == 1, "Implant must not stack");
        helper.assertTrue(!new ItemStack(ApogeeItems.SANDEVISTAN).isDamageableItem(), "Implant has no durability");
        helper.assertTrue(new ItemStack(ApogeeItems.OS_CORE_T5).getMaxStackSize() == 64, "Components stack");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_advancement")
    public void craftingSandevistanUnlocksChallengeAdvancement(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1.5, 1.5, GameType.SURVIVAL);
        var advancement = player.getServer().getAdvancements().get(Apogee.id("craft_sandevistan"));
        helper.assertTrue(advancement != null, "Missing advancement apogee:craft_sandevistan");
        helper.assertTrue(advancement.value().display().orElseThrow().getType() == net.minecraft.advancements.AdvancementType.CHALLENGE,
                "Sandevistan advancement should be a challenge (purple) frame");
        helper.assertTrue(!player.getAdvancements().getOrStartProgress(advancement).isDone(), "Advancement starts locked");
        net.minecraft.advancements.CriteriaTriggers.RECIPE_CRAFTED.trigger(player, Apogee.id("sandevistan"), List.of());
        helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "Crafting the Sandevistan unlocks it");
        cleanup(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_slot")
    public void implantUsesCyberwareSlotAndKeepsArmor(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1.5, 1.5, GameType.SURVIVAL);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        helper.assertTrue(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.SANDEVISTAN)), "equip");
        var inventory = TrinketsApi.getTrinketComponent(player).orElseThrow().getInventory();
        helper.assertTrue(inventory.containsKey("cyberware") && inventory.get("cyberware").containsKey("os"), "cyberware/os slot exists");
        helper.assertTrue(inventory.get("cyberware").get("os").getContainerSize() == 1, "single os slot");
        helper.assertTrue(inventory.get("cyberware").get("os").getItem(0).is(ApogeeItems.SANDEVISTAN), "implant sits in os slot");
        helper.assertTrue(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.ELYTRA), "elytra stays equipped");
        helper.assertTrue(!TrinketItem.equipItem(player, new ItemStack(ApogeeItems.SANDEVISTAN)), "second implant has no slot");
        cleanup(helper, player);
        helper.succeed();
    }

    // ------------------------------------------------------------------ activation, cooldown, persistence

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_activation", timeoutTicks = 200)
    public void activationRequiresImplantAndStartsCooldownOnManualEnd(GameTestHelper helper) {
        ServerPlayer bare = player(helper, 1.5, 1.5, GameType.SURVIVAL);
        SandevistanManager.toggle(bare);
        helper.assertTrue(!SandevistanManager.isActive(bare), "no activation without the implant");

        ServerPlayer player = wearer(helper, 3.5, 3.5);
        double baseSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        SandevistanManager.toggle(player);
        helper.assertTrue(SandevistanManager.isActive(player), "activates when equipped");
        long start = now(helper);
        helper.assertTrue(SandevistanManager.activeEndTime(player) == start + rules().durationTicks(), "10 s duration");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - baseSpeed) < 1.0E-6, "movement bonus starts at zero");
        double[] halfway = new double[1];

        helper.startSequence()
                .thenExecuteAfter(rules().movementRampTicks() / 2, () -> {
                    halfway[0] = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
                    helper.assertTrue(halfway[0] > baseSpeed && halfway[0] < baseSpeed * 1.5, "movement bonus eases in, got " + halfway[0]);
                })
                .thenExecuteAfter(rules().movementRampTicks() / 2 + 2, () -> {
                    double boosted = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
                    helper.assertTrue(Math.abs(boosted - baseSpeed * 1.5) < 1.0E-6, "+50% movement after the ramp, got " + boosted + " from " + baseSpeed);
                })
                .thenExecuteAfter(5, () -> {
                    SandevistanManager.toggle(player);
                    helper.assertTrue(!SandevistanManager.isActive(player), "manual end");
                    helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - baseSpeed) < 1.0E-6, "speed restored");
                    helper.assertTrue(SandevistanManager.cooldownEndTime(player) == now(helper) + rules().cooldownTicks(), "30 s cooldown from the end");
                    SandevistanManager.toggle(player);
                    helper.assertTrue(!SandevistanManager.isActive(player), "cannot activate during cooldown");

                    CompoundTag saved = player.saveWithoutId(new CompoundTag());
                    String tag = saved.toString();
                    helper.assertTrue(tag.contains(ApogeeAttachments.COOLDOWN_END.identifier().toString()), "cooldown saved with the player: " + tag.length());
                })
                .thenExecute(() -> cleanup(helper, bare, player))
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_expiry", timeoutTicks = 400)
    public void expiresAfterDuration(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, 2.5, 2.5);
        SandevistanManager.toggle(player);
        long end = SandevistanManager.activeEndTime(player);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!SandevistanManager.isActive(player), "still active"))
                .thenExecute(() -> {
                    long elapsedPastEnd = now(helper) - end;
                    helper.assertTrue(elapsedPastEnd >= 0 && elapsedPastEnd <= 2, "ended on time, off by " + elapsedPastEnd);
                    cleanup(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_unequip", timeoutTicks = 100)
    public void unequipEndsAndCleansUp(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, 2.5, 2.5);
        Zombie zombie = mob(helper, EntityType.ZOMBIE, 5.5, 2.5);
        zombie.setTarget(player);
        SandevistanManager.toggle(player);
        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(slowed(zombie), "zombie slowed while active");
                    TrinketsApi.getTrinketComponent(player).orElseThrow().getInventory().get("cyberware").get("os").setItem(0, ItemStack.EMPTY);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(!SandevistanManager.isActive(player), "unequip ends the effect");
                    helper.assertTrue(!slowed(zombie), "no residual slowdown");
                    helper.assertTrue(SandevistanManager.cooldownEndTime(player) > now(helper), "cooldown started");
                    helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Apogee.id("sandevistan_movement")) == null, "modifier removed");
                    cleanup(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_death", timeoutTicks = 100)
    public void deathEndsEffect(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, 2.5, 2.5);
        SandevistanManager.toggle(player);
        helper.startSequence()
                .thenExecuteAfter(2, () -> player.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(!SandevistanManager.isActive(player), "death ends the effect");
                    cleanup(helper, player);
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------ field selection and time scale

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_field", timeoutTicks = 300)
    public void fieldSlowsOnlyCombatants(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, 2.5, 2.5);
        Zombie attacker = mob(helper, EntityType.ZOMBIE, 7.5, 2.5);
        attacker.setTarget(player);
        Zombie farmZombie = mob(helper, EntityType.ZOMBIE, 2.5, 7.5);
        Cow cow = mob(helper, EntityType.COW, 6.5, 6.5);
        Zombie farAttacker = mob(helper, EntityType.ZOMBIE, 2.5, 15.5);
        farAttacker.setTarget(player);
        ServerPlayer survivor = player(helper, 4.5, 4.5, GameType.SURVIVAL);
        ServerPlayer creative = player(helper, 5.5, 4.5, GameType.CREATIVE);

        int[] start = new int[4];
        SandevistanManager.toggle(player);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(slowed(attacker), "zombie targeting the wearer is slowed");
                    helper.assertTrue(!slowed(farmZombie), "idle monster keeps its rhythm");
                    helper.assertTrue(!slowed(cow), "animals keep their rhythm");
                    helper.assertTrue(!slowed(farAttacker), "outside the 10 block radius");
                    helper.assertTrue(slowed(survivor), "survival players are slowed without line of sight rules");
                    helper.assertTrue(!slowed(creative), "creative players are not targets");
                    helper.assertTrue(!slowed(player), "the wearer is never slowed");
                    start[0] = attacker.tickCount;
                    start[1] = farmZombie.tickCount;
                    start[2] = cow.tickCount;
                    start[3] = farAttacker.tickCount;
                })
                .thenExecuteAfter(SAMPLE_TICKS, () -> {
                    int slowTicks = attacker.tickCount - start[0];
                    float expected = SAMPLE_TICKS * rules().timeScale();
                    helper.assertTrue(Math.abs(slowTicks - expected) <= 1.5F, "slowed entity ran " + slowTicks + " ticks, expected ~" + expected);
                    helper.assertTrue(farmZombie.tickCount - start[1] == SAMPLE_TICKS, "idle zombie ran " + (farmZombie.tickCount - start[1]));
                    helper.assertTrue(cow.tickCount - start[2] == SAMPLE_TICKS, "cow ran " + (cow.tickCount - start[2]));
                    // The far zombie can sit in a chunk without entity ticking; only its selection matters.
                    helper.assertTrue(!slowed(farAttacker), "far zombie never selected");
                    SandevistanManager.toggle(player);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(!slowed(attacker) && !slowed(survivor), "field cleared after ending");
                    start[0] = attacker.tickCount;
                })
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(attacker.tickCount - start[0] == 20, "normal rhythm restored");
                    cleanup(helper, player, survivor, creative);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_follow", timeoutTicks = 100)
    public void fieldFollowsWearerAndReleasesOnExit(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, 2.5, 2.5);
        Zombie attacker = mob(helper, EntityType.ZOMBIE, 9.5, 2.5);
        attacker.setTarget(player);
        SandevistanManager.toggle(player);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(slowed(attacker), "inside at 7 blocks");
                    Vec3 away = helper.absoluteVec(new Vec3(-12.5, 2.0, 2.5));
                    player.teleportTo(helper.getLevel(), away.x, away.y, away.z, 0.0F, 0.0F);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(!slowed(attacker), "released once the field moved away");
                    cleanup(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_memory", timeoutTicks = 400)
    public void recentDamageExchangeSelectsMonster(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, 2.5, 2.5);
        Skeleton skeleton = mob(helper, EntityType.SKELETON, 6.5, 2.5);
        skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        skeleton.setHealth(200.0F);
        skeleton.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        SandevistanManager.toggle(player);
        long[] exchange = {now(helper)};
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(slowed(skeleton), "monster that exchanged damage is slowed"))
                .thenWaitUntil(() -> helper.assertTrue(!slowed(skeleton), "memory not expired yet"))
                .thenExecute(() -> {
                    long elapsed = now(helper) - exchange[0];
                    helper.assertTrue(elapsed >= rules().combatMemoryTicks() && elapsed <= rules().combatMemoryTicks() + 3,
                            "released after " + elapsed + " ticks");
                    cleanup(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_immunity", timeoutTicks = 100)
    public void activeWearersAreImmuneToEachOther(GameTestHelper helper) {
        ServerPlayer first = wearer(helper, 2.5, 2.5);
        ServerPlayer second = wearer(helper, 5.5, 2.5);
        SandevistanManager.toggle(first);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(slowed(second), "inactive wearer inside a field is slowed");
                    SandevistanManager.toggle(second);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(SandevistanManager.isActive(second), "can activate while inside a field");
                    helper.assertTrue(!slowed(first) && !slowed(second), "active wearers are immune");
                    cleanup(helper, first, second);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_staggered", timeoutTicks = 100)
    public void staggeredWearersKeepIndependentTimers(GameTestHelper helper) {
        ServerPlayer first = wearer(helper, 2.5, 2.5);
        ServerPlayer second = wearer(helper, 5.5, 2.5);
        double baseSpeed = first.getAttributeValue(Attributes.MOVEMENT_SPEED);
        SandevistanManager.toggle(first);
        long firstEnd = SandevistanManager.activeEndTime(first);
        helper.startSequence()
                .thenExecuteAfter(6, () -> {
                    helper.assertTrue(slowed(second), "second wearer is slowed while inactive");
                    SandevistanManager.toggle(second);
                    helper.assertTrue(SandevistanManager.activeEndTime(second) == now(helper) + rules().durationTicks(), "second wearer gets their own full duration");
                    helper.assertTrue(SandevistanManager.activeEndTime(first) == firstEnd, "first wearer's timer is unchanged");
                })
                .thenExecuteAfter(rules().movementRampTicks() + 2, () -> {
                    helper.assertTrue(!slowed(first) && !slowed(second), "both active wearers are free of slowdown");
                    helper.assertTrue(first.getAttributeValue(Attributes.MOVEMENT_SPEED) > baseSpeed
                            && second.getAttributeValue(Attributes.MOVEMENT_SPEED) > baseSpeed, "both keep the movement bonus");
                    SandevistanManager.end(first);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(slowed(first), "first wearer is slowed by the other field once their effect ends");
                    helper.assertTrue(!slowed(second) && SandevistanManager.isActive(second), "second wearer continues unaffected");
                    cleanup(helper, first, second);
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------ projectiles and damage

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_projectiles", timeoutTicks = 100)
    public void projectilesFollowOwnership(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, 2.5, 2.5);
        Zombie attacker = mob(helper, EntityType.ZOMBIE, 8.5, 2.5);
        attacker.setTarget(player);
        SandevistanManager.toggle(player);
        ServerLevel level = helper.getLevel();
        Vec3 arrowPos = helper.absoluteVec(new Vec3(4.5, 6.0, 4.5));

        Arrow own = new Arrow(level, arrowPos.x, arrowPos.y, arrowPos.z, new ItemStack(Items.ARROW), null);
        own.setOwner(player);
        own.setNoGravity(true);
        level.addFreshEntity(own);
        Arrow hostile = new Arrow(level, arrowPos.x + 1, arrowPos.y, arrowPos.z, new ItemStack(Items.ARROW), null);
        hostile.setOwner(attacker);
        hostile.setNoGravity(true);
        level.addFreshEntity(hostile);
        Arrow dispenser = new Arrow(level, arrowPos.x + 2, arrowPos.y, arrowPos.z, new ItemStack(Items.ARROW), null);
        dispenser.setNoGravity(true);
        level.addFreshEntity(dispenser);

        helper.assertTrue(Boolean.TRUE.equals(own.getAttached(ApogeeAttachments.BOOSTED_PROJECTILE)), "launched during activation keeps the bonus");
        helper.assertTrue(!Boolean.TRUE.equals(hostile.getAttached(ApogeeAttachments.BOOSTED_PROJECTILE)), "other owners get no bonus");
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(TimeScaledEntity.of(own).apogee$isSlowed(), "wearer's projectile slowed inside the field");
                    helper.assertTrue(TimeScaledEntity.of(hostile).apogee$isSlowed(), "combatant's projectile slowed");
                    helper.assertTrue(!TimeScaledEntity.of(dispenser).apogee$isSlowed(), "dispenser projectile excluded");
                    cleanup(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_damage", timeoutTicks = 100)
    public void meleeAndProjectileDamageBonus(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, 2.5, 2.5);
        Zombie dummy = mob(helper, EntityType.ZOMBIE, 4.5, 2.5);
        dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0);
        dummy.setHealth(500.0F);
        dummy.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        float[] health = new float[1];
        helper.startSequence()
                .thenExecute(() -> {
                    health[0] = dummy.getHealth();
                    dummy.hurt(helper.getLevel().damageSources().playerAttack(player), 4.0F);
                    helper.assertTrue(Math.abs(health[0] - dummy.getHealth() - 4.0F) < 1.0E-3, "inactive melee 4, got " + (health[0] - dummy.getHealth()));
                    SandevistanManager.toggle(player);
                    dummy.invulnerableTime = 0;
                    health[0] = dummy.getHealth();
                    dummy.hurt(helper.getLevel().damageSources().playerAttack(player), 4.0F);
                    float expected = 4.0F * rules().damageMultiplier();
                    helper.assertTrue(Math.abs(health[0] - dummy.getHealth() - expected) < 1.0E-3, "active melee " + expected + ", got " + (health[0] - dummy.getHealth()));

                    Arrow arrow = new Arrow(helper.getLevel(), dummy.getX(), dummy.getY() + 3, dummy.getZ(), new ItemStack(Items.ARROW), null);
                    arrow.setOwner(player);
                    helper.getLevel().addFreshEntity(arrow);
                    SandevistanManager.toggle(player);
                    dummy.invulnerableTime = 0;
                    health[0] = dummy.getHealth();
                    dummy.hurt(helper.getLevel().damageSources().arrow(arrow, player), 4.0F);
                    helper.assertTrue(Math.abs(health[0] - dummy.getHealth() - expected) < 1.0E-3, "boosted arrow after ending, got " + (health[0] - dummy.getHealth()));
                    cleanup(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_knockback", timeoutTicks = 100)
    public void slowedTargetsTakeReducedKnockback(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, 2.5, 2.5);
        Zombie slowedZombie = mob(helper, EntityType.ZOMBIE, 5.5, 2.5);
        slowedZombie.setTarget(player);
        Zombie normalZombie = mob(helper, EntityType.ZOMBIE, 2.5, 20.5);
        SandevistanManager.toggle(player);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(slowed(slowedZombie) && !slowed(normalZombie), "setup");
                    slowedZombie.setDeltaMovement(Vec3.ZERO);
                    normalZombie.setDeltaMovement(Vec3.ZERO);
                    slowedZombie.knockback(0.4, -1.0, 0.0);
                    normalZombie.knockback(0.4, -1.0, 0.0);
                    double slowedPush = slowedZombie.getDeltaMovement().horizontalDistance();
                    double normalPush = normalZombie.getDeltaMovement().horizontalDistance();
                    double expected = normalPush * rules().slowedKnockbackMultiplier();
                    helper.assertTrue(Math.abs(slowedPush - expected) < 1.0E-6,
                            "slowed knockback " + slowedPush + ", expected " + expected + " from " + normalPush);
                    cleanup(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_kills", timeoutTicks = 100)
    public void killsExtendUpToMaximum(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, 2.5, 2.5);
        List<Zombie> zombies = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Zombie zombie = mob(helper, EntityType.ZOMBIE, 4.5 + (i % 4), 4.5 + (i / 4));
            zombie.setTarget(player);
            zombies.add(zombie);
        }
        Cow cow = mob(helper, EntityType.COW, 1.5, 6.5);
        Zombie outsider = mob(helper, EntityType.ZOMBIE, 2.5, 30.5);
        SandevistanManager.toggle(player);
        long start = now(helper);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    long before = SandevistanManager.activeEndTime(player);
                    cow.hurt(helper.getLevel().damageSources().playerAttack(player), 1000.0F);
                    helper.assertTrue(SandevistanManager.activeEndTime(player) == before, "animal kills do not extend");
                    outsider.hurt(helper.getLevel().damageSources().playerAttack(player), 1000.0F);
                    helper.assertTrue(SandevistanManager.activeEndTime(player) == before, "unaffected kills do not extend");
                    zombies.get(0).hurt(helper.getLevel().damageSources().generic(), 1000.0F);
                    helper.assertTrue(SandevistanManager.activeEndTime(player) == before, "deaths not attributed to the wearer do not extend");

                    zombies.get(1).hurt(helper.getLevel().damageSources().playerAttack(player), 1000.0F);
                    helper.assertTrue(SandevistanManager.activeEndTime(player) == before + rules().killExtensionTicks(), "a kill adds 2 s");
                    for (int i = 2; i < zombies.size(); i++) {
                        zombies.get(i).hurt(helper.getLevel().damageSources().playerAttack(player), 1000.0F);
                    }
                    helper.assertTrue(SandevistanManager.activeEndTime(player) == start + rules().maxDurationTicks(), "capped at 20 s from the start, got " + (SandevistanManager.activeEndTime(player) - start));
                    cleanup(helper, player);
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------ performance

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_perf", timeoutTicks = 200)
    public void fieldCostWithSeveralWearers(GameTestHelper helper) {
        List<ServerPlayer> wearers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            wearers.add(wearer(helper, 2.5 + i * 2, 2.5));
        }
        for (int i = 0; i < 160; i++) {
            Zombie zombie = mob(helper, EntityType.ZOMBIE, 1.5 + (i % 8), 5.5 + (i / 8) * 0.5);
            zombie.setTarget(wearers.get(i % wearers.size()));
        }
        wearers.forEach(SandevistanManager::toggle);
        long[] total = new long[2];
        helper.startSequence()
                .thenExecuteFor(100, () -> {
                    total[0] += SandevistanManager.lastTickNanos();
                    total[1]++;
                })
                .thenExecute(() -> {
                    double averageMs = total[0] / (double) total[1] / 1_000_000.0;
                    Apogee.LOGGER.info("APOGEE_PERF 4 wearers x 160 slowed zombies: manager {} ms/tick over {} ticks",
                            String.format(java.util.Locale.ROOT, "%.3f", averageMs), total[1]);
                    helper.assertTrue(averageMs < 2.0, "field selection too slow: " + averageMs + " ms/tick");
                    cleanup(helper, wearers.toArray(ServerPlayer[]::new));
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------ world rhythm

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_farm", timeoutTicks = 200)
    public void hoppersKeepGlobalRhythm(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(3, 3, 3);
        BlockPos hopperPos = new BlockPos(3, 2, 3);
        BlockPos outputPos = new BlockPos(3, 1, 3);
        helper.setBlock(outputPos, Blocks.CHEST);
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(chestPos, Blocks.CHEST);
        ChestBlockEntity source = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(chestPos));
        source.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        ServerPlayer player = wearer(helper, 5.5, 3.5);
        Zombie attacker = mob(helper, EntityType.ZOMBIE, 3.5, 5.5);
        attacker.setTarget(player);
        SandevistanManager.toggle(player);
        helper.startSequence()
                .thenExecuteAfter(80, () -> {
                    helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(hopperPos)) instanceof HopperBlockEntity, "hopper");
                    ChestBlockEntity output = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(outputPos));
                    int moved = 0;
                    for (int i = 0; i < output.getContainerSize(); i++) {
                        moved += output.getItem(i).getCount();
                    }
                    helper.assertTrue(moved >= 9, "hopper moved " + moved + " items in 80 ticks next to an active field");
                    helper.assertTrue(slowed(attacker), "combatant still slowed during the farm check");
                    cleanup(helper, player);
                })
                .thenSucceed();
    }
}
