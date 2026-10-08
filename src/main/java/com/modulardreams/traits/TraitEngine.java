package com.modulardreams.traits;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.modulardreams.ModularDreams;
import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.equipment.LiningItem;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.material.ModTraits;
import com.modulardreams.stats.StatsEngine;

/**
 * Runs every gameplay effect of the v2 traits. Traits are NEVER stored on
 * the player - each server tick the engine re-derives what every player
 * carries (worn armor, held tools, inventory items) from
 * {@link TraitAggregator} and applies or removes the consequences:
 *
 * <ul>
 *   <li>tick effects - wood repair, stone shadowed attributes, copper
 *       powered scans, iron last stand, gold absorption, netherite nether
 *       buffs, phantom End effects;</li>
 *   <li>damage events - immunity cancels, blazing ignition, resonance
 *       chains, fortunate durability contexts;</li>
 *   <li>windborne launch (called from {@code ModularToolItem.use()}).</li>
 * </ul>
 */
public final class TraitEngine {

    private TraitEngine() {}

    // ------------------------------------------------------------------ per-player state

    private static final EquipmentSlot[] TRAIT_CACHE_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private static final Identifier SHADOWED_SPEED_ID = ModularDreams.id("trait/shadowed_speed");
    private static final Identifier SHADOWED_ARMOR_ID = ModularDreams.id("trait/shadowed_armor");
    private static final Identifier AQUATIC_ID = ModularDreams.id("trait/aquatic");
    private static final Identifier NETHER_HEAT_ATTACK_ID = ModularDreams.id("trait/netherforged_heat");
    private static final Identifier NETHER_HEAT_MINING_ID = ModularDreams.id("trait/netherforged_heat_mining");

    private static final class State {
        long poweredUntil;
        long nextAbsorptionRefreshAt;
        long windborneAt;
        long resonanceLastHit = Long.MIN_VALUE;
        int resonanceChain;
        boolean voidTeleportQueued;
        boolean heatEmpowered;
        /** Exact fall distance recorded at the start of vanilla fall damage; -1 when none. */
        double recordedFall = -1.0D;
        /** Refreshed once per tick: whether the player wears any wool lining. */
        boolean wearsWool;

        // Equipment trait cache: invalidated only when the six trait-bearing slots actually change.
        boolean traitCacheInitialized;
        final Item[] traitItems = new Item[TRAIT_CACHE_SLOTS.length];
        final ModularData[] traitData = new ModularData[TRAIT_CACHE_SLOTS.length];
        List<String> wornTraits = List.of();
        List<String> heldTraits = List.of();

        // Event-driven nearby block caches. Only active positions are stored.
        boolean copperCacheInitialized;
        boolean heatCacheInitialized;
        ServerLevel copperLevel;
        ServerLevel heatLevel;
        BlockPos copperCenter;
        BlockPos heatCenter;
        final LongSet nearbyRedstone = new LongOpenHashSet();
        final LongSet nearbyHeat = new LongOpenHashSet();
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    // ------------------------------------------------------------------ init

    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(TraitEngine::tickLevel);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(TraitEngine::allowDamage);
        // per-player state must not outlive the session (memory leak otherwise)
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> STATES.remove(handler.getPlayer().getUUID()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> STATES.clear());
        ServerLivingEntityEvents.AFTER_DAMAGE.register(TraitEngine::afterDamage);
        // publishes the just-broken block for the diamond "Fortunate" durability save
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register(
                (world, player, pos, state, blockEntity) -> {
                    if (player instanceof ServerPlayer serverPlayer && world instanceof ServerLevel level) {
                        DurabilityHooks.publishBreakContext(state, level.getGameTime());
                    }
                    return true;
                });
    }

    /** Refreshes the six-slot trait cache only when an item or its modular data actually changes. */
    private static void refreshTraitCache(ServerPlayer player, State state) {
        if (state.traitCacheInitialized && traitSourcesMatch(player, state)) {
            return;
        }
        state.wornTraits = TraitAggregator.wornTraits(player);
        state.heldTraits = TraitAggregator.heldTraits(player);
        state.wearsWool = state.wornTraits.contains(ModTraits.WOOL);
        for (int i = 0; i < TRAIT_CACHE_SLOTS.length; i++) {
            ItemStack stack = player.getItemBySlot(TRAIT_CACHE_SLOTS[i]);
            state.traitItems[i] = stack.isEmpty() ? null : stack.getItem();
            state.traitData[i] = stack.isEmpty() ? null : stack.get(ModDataComponents.MODULAR_DATA);
        }
        state.traitCacheInitialized = true;
    }

    private static boolean traitSourcesMatch(ServerPlayer player, State state) {
        for (int i = 0; i < TRAIT_CACHE_SLOTS.length; i++) {
            ItemStack stack = player.getItemBySlot(TRAIT_CACHE_SLOTS[i]);
            Item item = stack.isEmpty() ? null : stack.getItem();
            ModularData data = stack.isEmpty() ? null : stack.get(ModDataComponents.MODULAR_DATA);
            if (state.traitItems[i] != item || state.traitData[i] != data) {
                return false;
            }
        }
        return true;
    }

    /** Cached trait check for hot damage/freezing hooks; refreshed on demand when equipment changed. */
    public static boolean hasCachedWornTrait(ServerPlayer player, String trait) {
        State state = state(player);
        refreshTraitCache(player, state);
        return state.wornTraits.contains(trait);
    }

    /** Cached trait check for a held slot, refreshed on demand if equipment changed. */
    public static boolean hasCachedHeldTrait(ServerPlayer player, String trait) {
        State state = state(player);
        refreshTraitCache(player, state);
        return state.heldTraits.contains(trait);
    }

    // ------------------------------------------------------------------ ticking

    private static void tickLevel(ServerLevel level) {
        long gameTime = level.getGameTime();
        for (Player p : level.players()) {
            if (p instanceof ServerPlayer player) {
                tickPlayer(player, gameTime);
            }
        }
    }

    private static void tickPlayer(ServerPlayer player, long gameTime) {
        if (player.isDeadOrDying()) {
            return;
        }
        State state = state(player);
        refreshTraitCache(player, state);
        List<String> worn = state.wornTraits;
        List<String> held = state.heldTraits;
        state.wearsWool = worn.contains(ModTraits.WOOL);

        // ---- queued phantom void teleport ----
        if (state.voidTeleportQueued) {
            state.voidTeleportQueued = false;
            executeVoidTeleport(player);
        }

        boolean inNether = player.level().dimension() == Level.NETHER;
        boolean inEnd = player.level().dimension() == Level.END;

        // ---- wood "Regenerative": feet in water + head in sunlight ----
        if (gameTime % TraitTuning.WOOD_REPAIR_INTERVAL_TICKS == 0 && woodConditionMet(player)) {
            repairWoodItems(player);
        }

        // ---- stone "Shadowed": below Y 63, no sunlight ----
        boolean shadowed = player.getY() < TraitTuning.SHADOWED_Y_LEVEL
                && !player.level().canSeeSky(player.blockPosition());
        // BLOCK_BREAK_SPEED is a multiplier in vanilla; the flat bonus is MINING_EFFICIENCY
        applyTransient(player, Attributes.MINING_EFFICIENCY, SHADOWED_SPEED_ID,
                shadowed && held.contains(ModTraits.STONE)
                        ? TraitTuning.SHADOWED_MINING_SPEED : 0.0D);
        applyTransient(player, Attributes.ARMOR, SHADOWED_ARMOR_ID,
                shadowed && worn.contains(ModTraits.STONE)
                        ? TraitTuning.SHADOWED_ARMOR_BONUS : 0.0D);

        // ---- copper "Powered": event-driven nearby redstone cache -> durability immunity window ----
        boolean copperLike = (worn.contains(ModTraits.COPPER) || held.contains(ModTraits.COPPER))
                || (!inNether && (worn.contains(ModTraits.ROSE_GOLD) || held.contains(ModTraits.ROSE_GOLD)));
        if (copperLike) {
            updateSpatialCache(player, state, false);
            if (gameTime % TraitTuning.COPPER_SCAN_INTERVAL_TICKS == 0
                    && !state.nearbyRedstone.isEmpty()) {
                state.poweredUntil = gameTime + TraitTuning.COPPER_IMMUNITY_TICKS;
            }
        } else {
            clearCopperCache(state);
        }

        // ---- iron "Last Stand": Strength I (tool) / Resistance I (armor) ----
        if (gameTime % 10 == 0 && player.getHealth() <= TraitTuning.LAST_STAND_HP && player.getHealth() > 0.0F) {
            if (held.contains(ModTraits.IRON)) {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,
                        TraitTuning.LAST_STAND_EFFECT_TICKS, 0, false, true));
            }
            if (worn.contains(ModTraits.IRON)) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                        TraitTuning.LAST_STAND_EFFECT_TICKS, 0, false, true));
            }
        }

        // ---- gold "Piglin's Favor" absorption; rose gold copies it in the Nether
        boolean goldLike = inNether
                ? (worn.contains(ModTraits.GOLD) || held.contains(ModTraits.GOLD))
                        || (worn.contains(ModTraits.ROSE_GOLD) || held.contains(ModTraits.ROSE_GOLD))
                : (worn.contains(ModTraits.GOLD) || held.contains(ModTraits.GOLD));
        // Scheduled refresh: Absorption I lasts 5 min 10 s and is re-applied every
        // 5 min, so the hearts never flicker off. Hearts LOST to damage only come
        // back at the next refresh - absorption HP do not regenerate.
        if (inNether && goldLike) {
            State st = state(player);
            if (st.nextAbsorptionRefreshAt == 0L) {
                st.nextAbsorptionRefreshAt = gameTime + TraitTuning.GOLD_ABSORPTION_REFRESH_TICKS;
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                        TraitTuning.GOLD_ABSORPTION_DURATION_TICKS, 0, false, true));
            } else if (gameTime >= st.nextAbsorptionRefreshAt) {
                st.nextAbsorptionRefreshAt = gameTime + TraitTuning.GOLD_ABSORPTION_REFRESH_TICKS;
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                        TraitTuning.GOLD_ABSORPTION_DURATION_TICKS, 0, false, true));
            }
        }

        // ---- netherite "Netherforged": fire/lava NEVER protects the player (that
        // would make Fire Resistance potions useless). The ITEMS are fireproof
        // (baked DamageResistant component). In the Nether, extreme heat EMPOWERS
        // the held item: +20% attack damage on weapons, +20% mining speed on
        // tools; every netherite item takes 50% less durability damage near heat.
        if (inNether && (worn.contains(ModTraits.NETHERITE) || held.contains(ModTraits.NETHERITE))) {
            updateSpatialCache(player, state, true);
            if (gameTime % TraitTuning.NETHER_HEAT_SCAN_INTERVAL_TICKS == 0) {
                state.heatEmpowered = !state.nearbyHeat.isEmpty();
            }
        } else {
            state.heatEmpowered = false;
            clearHeatCache(state);
        }
        applyHeatEmpowerment(player, state, held);

        // ---- phantom membrane: End effects ----
        if (inEnd && worn.contains(ModTraits.PHANTOM_MEMBRANE)) {
            if (player.hasEffect(MobEffects.LEVITATION)) {
                player.removeEffect(MobEffects.LEVITATION);
            }
            if (player.isShiftKeyDown() && player.getEffect(MobEffects.SLOW_FALLING) == null) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                        TraitTuning.PHANTOM_SLOW_FALL_TICKS, 0, false, true));
            }
        }

        // ---- prismarine "Aquatic": no underwater mining penalty while held ----
        applyTransient(player, Attributes.SUBMERGED_MINING_SPEED, AQUATIC_ID,
                player.isEyeInFluid(FluidTags.WATER)
                        && held.contains(ModTraits.PRISMARINE)
                        ? 0.8D : 0.0D);

        // ---- amethyst "Resonance": chain decays when the window expires ----
        if (state.resonanceChain > 0 && gameTime - state.resonanceLastHit > TraitTuning.RESONANCE_WINDOW_TICKS) {
            state.resonanceChain = 0;
            AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
            if (speed != null) {
                speed.removeModifier(ResonanceModifier.ID);
            }
        }
    }

    /**
     * Netherite near heat: transient +20% on the held item's combat stat.
     * Weapons (sword, axe, spear) get attack damage, mining tools (pickaxe,
     * shovel, hoe) get mining speed - the item's material is what reacts to
     * the heat, so the bonus stays on the individual item, not the player.
     */
    private static void applyHeatEmpowerment(ServerPlayer player, State state, List<String> heldTraits) {
        ItemStack held = player.getMainHandItem();
        ModularData data = held.get(ModDataComponents.MODULAR_DATA);
        boolean weapon = false;
        boolean tool = false;
        if (data != null && state.heatEmpowered && heldTraits.contains(ModTraits.NETHERITE)
                && ModTraits.dataHasTrait(data, ModTraits.NETHERITE)) {
            weapon = ModularToolType.byId(data.equipmentType())
                    .map(t -> t == ModularToolType.SWORD || t == ModularToolType.AXE
                            || t == ModularToolType.SPEAR)
                    .orElse(false);
            tool = !weapon && ModularToolType.byId(data.equipmentType()).isPresent();
        }
        applyTransient(player, Attributes.ATTACK_DAMAGE, NETHER_HEAT_ATTACK_ID,
                weapon ? TraitTuning.NETHER_HEAT_ATTACK_MULT : 0.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyTransient(player, Attributes.BLOCK_BREAK_SPEED, NETHER_HEAT_MINING_ID,
                tool ? TraitTuning.NETHER_HEAT_MINING_MULT : 0.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    // ------------------------------------------------------------------ damage events

    /** Public seam for the self-test: the same decision allowDamage() makes. */
    public static boolean testCancelDamage(net.minecraft.world.entity.LivingEntity entity,
            DamageSource source, float amount) {
        return !allowDamage(entity, source, amount);
    }

    /**
     * Cancels: netherite fire/lava, leather lining freezing, slime lining
     * falls + elytra wall hits, phantom full-set void. Returns false to
     * cancel the damage.
     */
    private static boolean allowDamage(net.minecraft.world.entity.LivingEntity entity,
            DamageSource source, float amount) {
        if (!(entity instanceof Player player) || player.isCreative()) {
            return true;
        }
        long gameTime = player.level().getGameTime();

        // NOTE: netherite no longer protects the PLAYER from fire or lava
        // (that would make Fire Resistance potions useless) - the ITEMS are
        // the fireproof ones, via their baked DamageResistant component.

        // slime lining "Bouncy": no fall damage, no elytra wall damage - bounce instead
        if ((source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.FLY_INTO_WALL))
                && hasCachedWornTrait((ServerPlayer) player, ModTraits.SLIME)) {
            // always consume the recorded fall distance, so a zero-damage fall cannot leak into the next hit
            double fallDistance = takeRecordedFall(player);
            // no damage would have been dealt (e.g. a fall inside the safe distance): nothing to absorb, no bounce
            if (amount <= 0.0F) {
                return true;
            }
            player.resetFallDistance();
            double bounce = source.is(DamageTypeTags.IS_FALL)
                    ? bounceFromFall(player, fallDistance)
                    : bounceFromWallDamage(amount);
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, Math.max(v.y, 0.0D) + bounce, v.z);
            syncMotion(player);
            return false;
        }
        // phantom membrane full set: the void sends you home instead of killing you
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && TraitAggregator.hasFullLiningSet(player, ModTraits.PHANTOM_MEMBRANE)) {
            state(player).voidTeleportQueued = true;
            return false;
        }
        return true;
    }

    /** Impact speed (blocks/tick) to bounce, scaled and clamped to the tuned range. */
    private static double bounceFor(double impactSpeed) {
        return Math.min(TraitTuning.BOUNCY_MAX_BOUNCE,
                Math.max(TraitTuning.BOUNCY_MIN_BOUNCE, impactSpeed * TraitTuning.BOUNCY_RESTITUTION));
    }

    /**
     * Bounce from a fall, using the exact fall distance recorded before vanilla
     * applies its multipliers (hay bales, FALL_DAMAGE_MULTIPLIER). Free fall
     * reaches v = sqrt(2 * g * d) blocks/tick; air drag is ignored, and the
     * restitution tuning absorbs it. A non-positive distance (no record) gives
     * the minimum bounce.
     */
    private static double bounceFromFall(Player player, double fallDistance) {
        if (fallDistance <= 0.0D) {
            return TraitTuning.BOUNCY_MIN_BOUNCE;
        }
        double gravity = player.getAttributeValue(Attributes.GRAVITY);
        return bounceFor(Math.sqrt(2.0D * gravity * fallDistance));
    }

    /**
     * Bounce from an elytra wall hit. Vanilla handleFallFlyingCollisions deals
     * (speed lost in the collision) * 10 - 3, with the horizontal speed in
     * blocks/tick. A head-on hit loses roughly the impact speed, so the impact
     * speed is (damage + 3) / 10.
     */
    private static double bounceFromWallDamage(float kineticDamage) {
        return bounceFor((kineticDamage + 3.0D) / 10.0D);
    }

    /** Records the exact fall distance at the start of vanilla fall damage (server players only). */
    public static void recordFallDistance(Player player, double fallDistance) {
        state(player).recordedFall = fallDistance;
    }

    /** Returns and clears the recorded fall distance, or -1 if none was recorded. */
    private static double takeRecordedFall(Player player) {
        State state = state(player);
        double distance = state.recordedFall;
        state.recordedFall = -1.0D;
        return distance;
    }

    private static void afterDamage(net.minecraft.world.entity.LivingEntity entity,
            DamageSource source, float baseAmount, float amount, boolean blocked) {
        if (!(source.getEntity() instanceof ServerPlayer attacker) || entity == attacker) {
            return;
        }
        if (entity.level() instanceof ServerLevel level && !blocked) {
            long gameTime = level.getGameTime();
            var held = TraitAggregator.heldTraits(attacker);

            // blaze rod "Blazing": hits set mobs on fire
            if (held.contains(ModTraits.BLAZE_ROD) && entity.isAlive()) {
                entity.igniteForTicks(TraitTuning.BLAZING_FIRE_TICKS);
            }
            // diamond "Fortunate": publish the attack context for the durability save
            if (held.contains(ModTraits.DIAMOND)) {
                DurabilityHooks.publishAttackContext(entity.getHealth(), gameTime);
            }
            // amethyst "Resonance": chained hits speed up the attacker (cap +0.6)
            if (held.contains(ModTraits.AMETHYST)) {
                State state = state(attacker);
                state.resonanceChain = gameTime - state.resonanceLastHit <= TraitTuning.RESONANCE_WINDOW_TICKS
                        ? state.resonanceChain + 1
                        : 1;
                state.resonanceChain = Math.min(state.resonanceChain, TraitTuning.RESONANCE_MAX_CHAIN);
                state.resonanceLastHit = gameTime;
                applyTransient(attacker, Attributes.ATTACK_SPEED, ResonanceModifier.ID,
                        TraitTuning.RESONANCE_SPEED_PER_HIT * state.resonanceChain);
            }
        }
    }

    /**
     * Players are client-authoritative for movement: a server-side setDeltaMovement is
     * overwritten by the client unless the new velocity is sent to it explicitly.
     */
    private static void syncMotion(Player player) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            serverPlayer.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }

    // ------------------------------------------------------------------ windborne

    /** Breeze rod "Windborne": right-click launches the player with a gust at their feet. */
    public static boolean tryWindborne(ServerPlayer player) {
        long gameTime = player.level().getGameTime();
        State state = state(player);
        if (gameTime - state.windborneAt < TraitTuning.WINDBORNE_COOLDOWN_TICKS) {
            return false;
        }
        state.windborneAt = gameTime;
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, v.y + TraitTuning.WINDBORNE_LAUNCH_POWER, v.z);
        syncMotion(player);
        player.resetFallDistance();
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.GUST,
                    player.getX(), player.getY() + 0.25D, player.getZ(), 6, 0.3D, 0.2D, 0.3D, 0.05D);
        }
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return true;
    }

    // ------------------------------------------------------------------ copper powered

    /** Cheap field check used by the durability mixin (no scanning). */
    public static boolean isRedstonePowered(ServerPlayer player, long gameTime) {
        return state(player).poweredUntil > gameTime;
    }

    /** Whether the player wears any wool lining; refreshes the cache on demand for event hooks. */
    public static boolean wearsWool(ServerPlayer player) {
        State state = state(player);
        refreshTraitCache(player, state);
        return state.wearsWool;
    }

    /** Whether the held netherite item is currently heat-empowered (Nether + heat nearby). */
    public static boolean isHeatEmpowered(ServerPlayer player) {
        return state(player).heatEmpowered;
    }

    /**
     * Applies a block-state change to every cached nearby-block set in the same level.
     * This replaces repeated 2-3 thousand block scans with point updates when redstone/heat changes.
     */
    public static void onBlockChanged(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        boolean wasRedstone = isActiveRedstone(oldState);
        boolean isRedstone = isActiveRedstone(newState);
        boolean redstoneChanged = wasRedstone != isRedstone;
        boolean wasHeat = isHeatSource(oldState);
        boolean isHeat = isHeatSource(newState);
        boolean heatChanged = wasHeat != isHeat;
        if (!redstoneChanged && !heatChanged) {
            return;
        }

        double radius = Math.max(
                redstoneChanged ? TraitTuning.COPPER_SCAN_RADIUS : 0,
                heatChanged ? TraitTuning.NETHER_HEAT_RADIUS : 0);
        AABB query = new AABB(
                pos.getX() - radius, pos.getY() - 4, pos.getZ() - radius,
                pos.getX() + 1.0D + radius, pos.getY() + 1.0D + 4, pos.getZ() + 1.0D + radius);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, query)) {
            State state = STATES.get(player.getUUID());
            if (state == null) {
                continue;
            }
            if (redstoneChanged && state.copperCacheInitialized && state.copperCenter != null && containsCopperArea(state.copperCenter, pos)) {
                if (isRedstone) {
                        state.nearbyRedstone.add(pos.asLong());
                } else {
                    state.nearbyRedstone.remove(pos.asLong());
                }
            }
            if (heatChanged && state.heatCacheInitialized && state.heatCenter != null && containsHeatArea(state.heatCenter, pos)) {
                if (isHeat) {
                        state.nearbyHeat.add(pos.asLong());
                } else {
                    state.nearbyHeat.remove(pos.asLong());
                }
            }
        }
    }

    private static void updateSpatialCache(ServerPlayer player, State state, boolean heat) {
        BlockPos current = player.blockPosition();
        ServerLevel level = (ServerLevel) player.level();
        if (heat) {
            if (!state.heatCacheInitialized || state.heatLevel != level) {
                buildHeatCache(player, state, current);
            } else if (!sameCenter(state.heatCenter, current)) {
                shiftCache(player, state, true, current);
            }
        } else {
            if (!state.copperCacheInitialized || state.copperLevel != level) {
                buildCopperCache(player, state, current);
            } else if (!sameCenter(state.copperCenter, current)) {
                shiftCache(player, state, false, current);
            }
        }
    }

    private static boolean sameCenter(BlockPos a, BlockPos b) {
        return a != null && a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
    }

    private static void buildCopperCache(ServerPlayer player, State state, BlockPos center) {
        state.nearbyRedstone.clear();
        int r = TraitTuning.COPPER_SCAN_RADIUS;
        addMatchingInBox(player, state.nearbyRedstone,
                center.getX() - r, center.getX() + r,
                center.getY() - 4, center.getY() + 4,
                center.getZ() - r, center.getZ() + r, false);
        state.copperLevel = (ServerLevel) player.level();
        state.copperCenter = center.immutable();
        state.copperCacheInitialized = true;
    }

    private static void buildHeatCache(ServerPlayer player, State state, BlockPos center) {
        state.nearbyHeat.clear();
        int r = TraitTuning.NETHER_HEAT_RADIUS;
        addMatchingInBox(player, state.nearbyHeat,
                center.getX() - r, center.getX() + r,
                center.getY() - 4, center.getY() + 4,
                center.getZ() - r, center.getZ() + r, true);
        state.heatLevel = (ServerLevel) player.level();
        state.heatCenter = center.immutable();
        state.heatCacheInitialized = true;
    }

    /**
     * Shifts a cached search volume by one or more blocks without rescanning its intersection.
     * Only outgoing/incoming slabs are touched; the unchanged interior stays cached.
     * Large teleports fall back to one full rebuild because the old volume has no useful overlap.
     */
    private static void shiftCache(ServerPlayer player, State state, boolean heat, BlockPos newCenter) {
        BlockPos oldCenter = heat ? state.heatCenter : state.copperCenter;
        int r = heat ? TraitTuning.NETHER_HEAT_RADIUS : TraitTuning.COPPER_SCAN_RADIUS;
        LongSet set = heat ? state.nearbyHeat : state.nearbyRedstone;

        int dx = newCenter.getX() - oldCenter.getX();
        int dy = newCenter.getY() - oldCenter.getY();
        int dz = newCenter.getZ() - oldCenter.getZ();
        if (Math.abs(dx) > 2 * r + 1 || Math.abs(dy) > 2 * 4 + 1 || Math.abs(dz) > 2 * r + 1) {
            if (heat) buildHeatCache(player, state, newCenter); else buildCopperCache(player, state, newCenter);
            return;
        }

        BlockPos cursor = oldCenter;
        if (dx != 0) {
            shiftCacheAxis(player, set, cursor, newCenter, r, 4, 0, dx, heat);
            cursor = new BlockPos(newCenter.getX(), cursor.getY(), cursor.getZ());
        }
        if (dy != 0) {
            shiftCacheAxis(player, set, cursor, newCenter, r, 4, 1, dy, heat);
            cursor = new BlockPos(cursor.getX(), newCenter.getY(), cursor.getZ());
        }
        if (dz != 0) {
            shiftCacheAxis(player, set, cursor, newCenter, r, 4, 2, dz, heat);
        }

        if (heat) state.heatCenter = newCenter.immutable(); else state.copperCenter = newCenter.immutable();
    }

    private static void shiftCacheAxis(ServerPlayer player, LongSet set, BlockPos oldCenter,
            BlockPos newCenter, int horizontalRadius, int verticalRadius, int axis, int delta, boolean heat) {
        int oldMinX = oldCenter.getX() - horizontalRadius, oldMaxX = oldCenter.getX() + horizontalRadius;
        int oldMinY = oldCenter.getY() - verticalRadius, oldMaxY = oldCenter.getY() + verticalRadius;
        int oldMinZ = oldCenter.getZ() - horizontalRadius, oldMaxZ = oldCenter.getZ() + horizontalRadius;
        int newMinX = newCenter.getX() - horizontalRadius, newMaxX = newCenter.getX() + horizontalRadius;
        int newMinY = newCenter.getY() - verticalRadius, newMaxY = newCenter.getY() + verticalRadius;
        int newMinZ = newCenter.getZ() - horizontalRadius, newMaxZ = newCenter.getZ() + horizontalRadius;

        if (axis == 0) {
            int removeFrom;
            int removeTo;
            if (delta > 0) {
                removeFrom = oldMinX;
                removeTo = newMinX - 1;
            } else {
                removeFrom = newMaxX + 1;
                removeTo = oldMaxX;
            }
            removeBox(set, removeFrom, removeTo, oldMinY, oldMaxY, oldMinZ, oldMaxZ);
            if (delta > 0) {
                addMatchingInBox(player, set, oldMaxX + 1, newMaxX, newMinY, newMaxY, newMinZ, newMaxZ, heat);
            } else {
                addMatchingInBox(player, set, newMinX, oldMinX - 1, newMinY, newMaxY, newMinZ, newMaxZ, heat);
            }
        } else if (axis == 1) {
            if (delta > 0) {
                removeBox(set, oldMinX, oldMaxX, oldMinY, newMinY - 1, oldMinZ, oldMaxZ);
                addMatchingInBox(player, set, newMinX, newMaxX, oldMaxY + 1, newMaxY, newMinZ, newMaxZ, heat);
            } else {
                removeBox(set, oldMinX, oldMaxX, newMaxY + 1, oldMaxY, oldMinZ, oldMaxZ);
                addMatchingInBox(player, set, newMinX, newMaxX, newMinY, oldMinY - 1, newMinZ, newMaxZ, heat);
            }
        } else {
            if (delta > 0) {
                removeBox(set, oldMinX, oldMaxX, oldMinY, oldMaxY, oldMinZ, newMinZ - 1);
                addMatchingInBox(player, set, newMinX, newMaxX, newMinY, newMaxY, oldMaxZ + 1, newMaxZ, heat);
            } else {
                removeBox(set, oldMinX, oldMaxX, oldMinY, oldMaxY, newMaxZ + 1, oldMaxZ);
                addMatchingInBox(player, set, newMinX, newMaxX, newMinY, newMaxY, newMinZ, oldMinZ - 1, heat);
            }
        }
    }

    private static void removeBox(LongSet set, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        if (minX > maxX || minY > maxY || minZ > maxZ) return;
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            set.remove(pos.asLong());
        }
    }

    private static void addMatchingInBox(ServerPlayer player, LongSet set,
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ, boolean heat) {
        if (minX > maxX || minY > maxY || minZ > maxZ) return;
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState blockState = player.level().getBlockState(pos);
            if (heat ? isHeatSource(blockState) : isActiveRedstone(blockState)) {
                set.add(pos.asLong());
            }
        }
    }

    private static boolean containsCopperArea(BlockPos center, BlockPos pos) {
        int r = TraitTuning.COPPER_SCAN_RADIUS;
        return Math.abs(center.getX() - pos.getX()) <= r
                && Math.abs(center.getY() - pos.getY()) <= 4
                && Math.abs(center.getZ() - pos.getZ()) <= r;
    }

    private static boolean containsHeatArea(BlockPos center, BlockPos pos) {
        int r = TraitTuning.NETHER_HEAT_RADIUS;
        return Math.abs(center.getX() - pos.getX()) <= r
                && Math.abs(center.getY() - pos.getY()) <= 4
                && Math.abs(center.getZ() - pos.getZ()) <= r;
    }

    private static void clearCopperCache(State state) {
        state.copperCacheInitialized = false;
        state.copperLevel = null;
        state.copperCenter = null;
        state.nearbyRedstone.clear();
    }

    private static void clearHeatCache(State state) {
        state.heatCacheInitialized = false;
        state.heatLevel = null;
        state.heatCenter = null;
        state.nearbyHeat.clear();
    }

    private static boolean isActiveRedstone(BlockState state) {
        if (state.is(Blocks.REDSTONE_BLOCK)) {
            return true;
        }
        if (state.hasProperty(BlockStateProperties.POWER)
                && state.getValue(BlockStateProperties.POWER) > 0) {
            return true;
        }
        if (state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED)) {
            return true;
        }
        if ((state.getBlock() instanceof RedstoneTorchBlock || state.getBlock() instanceof RedstoneLampBlock)
                && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
            return true;
        }
        return false;
    }

    private static boolean isHeatSource(BlockState state) {
        return state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
    }

    /** Compatibility/testing helper; the live engine never calls this full-volume scan. */
    public static boolean scanForRedstone(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        int r = TraitTuning.COPPER_SCAN_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.getX() - r, center.getY() - 4, center.getZ() - r,
                center.getX() + r, center.getY() + 4, center.getZ() + r)) {
            if (isActiveRedstone(player.level().getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    /** Compatibility/testing helper; the live engine never calls this full-volume scan. */
    public static boolean scanForHeat(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        int r = TraitTuning.NETHER_HEAT_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.getX() - r, center.getY() - 4, center.getZ() - r,
                center.getX() + r, center.getY() + 4, center.getZ() + r)) {
            if (isHeatSource(player.level().getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ wood repair

    public static boolean woodConditionMet(ServerPlayer player) {
        if (!player.isInWater()) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level) || !level.isBrightOutside()
                || level.isRainingAt(player.blockPosition())) {
            return false;
        }
        return level.canSeeSky(player.blockPosition().above());
    }

    /** Repairs one durability point on every damaged wood-trait stack in the whole inventory. */
    public static void repairWoodItems(Player player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            repairIfWood(inventory.getItem(i));
        }
    }

    private static void repairIfWood(ItemStack stack) {
        if (stack.isDamaged() && TraitAggregator.stackHasTrait(stack, ModTraits.WOOD)) {
            boolean wasBroken = com.modulardreams.stats.StatsEngine.isBroken(stack);
            stack.setDamageValue(stack.getDamageValue() - TraitTuning.WOOD_REPAIR_AMOUNT);
            // a repaired-out-of-broken item needs its working stats re-baked
            if (wasBroken && !com.modulardreams.stats.StatsEngine.isBroken(stack)) {
                ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
                if (data != null) {
                    com.modulardreams.stats.StatsEngine.bake(stack, data);
                }
            }
        }
    }

    // ------------------------------------------------------------------ phantom void teleport

    private static void executeVoidTeleport(ServerPlayer player) {
        TeleportTransition transition;
        try {
            transition = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        } catch (Exception e) {
            ModularDreams.LOGGER.warn("Phantom void teleport failed to find a spawn", e);
            return;
        }
        // the lining breaks - the plating stays
        stripLinings(player);
        player.teleport(transition);
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "trait.modular_dreams.phantom_membrane.saved"));
    }

    /**
     * Consumes exactly ONE phantom lining from the worn armor (the price of
     * one void save - a full set is four saves, the plating always stays).
     * An assembled piece loses its lining part; a standalone lining item is
     * consumed whole. Call only AFTER the teleport has been secured.
     */
    public static void stripLinings(Player player) {
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof LiningItem) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                return; // one lining consumed - done
            }
            ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
            if (data != null && data.materialOf(StatsEngine.LINING_PART_ID).isPresent()) {
                ModularData stripped = new ModularData(data.equipmentType(),
                        data.parts().stream()
                                .filter(p -> !p.part().equals(StatsEngine.LINING_PART_ID))
                                .toList(),
                        data.modifiers(), data.unlocks());
                stack.set(ModDataComponents.MODULAR_DATA, stripped);
                StatsEngine.bake(stack, stripped);
                return; // one lining consumed - done
            }
        }
    }

    // ------------------------------------------------------------------ attribute helpers

    private static void applyTransient(Player player, net.minecraft.core.Holder<Attribute> attribute,
            Identifier id, double amount) {
        applyTransient(player, attribute, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyTransient(Player player, net.minecraft.core.Holder<Attribute> attribute,
            Identifier id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (amount == 0.0D) {
            if (current != null) {
                instance.removeModifier(id);
            }
            return;
        }
        // skip the write when nothing changed: every write dirties the attribute
        // and makes the server resync it to the client
        if (current != null && current.amount() == amount && current.operation() == operation) {
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
    }

    /** Identifier of the transient resonance attack-speed modifier. */
    private static final class ResonanceModifier {
        static final Identifier ID = ModularDreams.id("trait/resonance");
    }
}
