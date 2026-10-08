package com.modulardreams.traits;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;

import com.modulardreams.material.MaterialTier;
import com.modulardreams.material.ModTraits;
import com.modulardreams.part.PartType;

/**
 * Decision logic for the durability traits, consumed by
 * {@code ItemStackDurabilityMixin} (the single hurtAndBreak choke point):
 * <ul>
 *   <li>Copper / rose-gold "Powered": the item takes no durability damage
 *       while the holder's redstone-boost window is open;</li>
 *   <li>Diamond "Fortunate": 50% durability saves for lower-tier mining,
 *       low-HP melee targets and small armor hits.</li>
 * </ul>
 * The context flags (mining tier, attack target, incoming damage) are
 * published by the events that run before the durability call in the same
 * server thread.
 */
public final class DurabilityHooks {

    /** Block just broken this tick, for the Fortunate lower-tier check. */
    public static final ThreadLocal<BlockState> BROKEN_BLOCK = new ThreadLocal<>();
    /** Health of the entity just hit this tick, or -1. */
    public static final ThreadLocal<Float> ATTACKED_HP = ThreadLocal.withInitial(() -> -1.0F);
    /** Raw damage currently being applied to the player (armor path), or -1. */
    public static final ThreadLocal<Float> INCOMING_DAMAGE = ThreadLocal.withInitial(() -> -1.0F);
    /** Game time when the attack context was published, to avoid stale flags. */
    public static final ThreadLocal<Long> CONTEXT_TICK = ThreadLocal.withInitial(() -> 0L);

    private DurabilityHooks() {}

    /** Whether one durability damage call on this stack is prevented. */
    public static boolean shouldPrevent(ItemStack stack, ServerPlayer holder, long gameTime) {
        if (stack.isEmpty() || holder == null || !stack.isDamageableItem()) {
            return false;
        }
        // Copper "Powered" (and rose gold's Overworld copy of it)
        boolean copper = TraitAggregator.stackHasTrait(stack, ModTraits.COPPER);
        boolean roseGoldActive = TraitAggregator.stackHasTrait(stack, ModTraits.ROSE_GOLD)
                && holder.level().dimension() != net.minecraft.world.level.Level.NETHER;
        if (copper || roseGoldActive) {
            if (TraitEngine.isRedstonePowered(holder, gameTime)) {
                return true;
            }
        }

        // Diamond "Fortunate"
        if (TraitAggregator.stackHasTrait(stack, ModTraits.DIAMOND)
                && holder.getRandom().nextFloat() < TraitTuning.FORTUNATE_SAVE_CHANCE) {
            if (CONTEXT_TICK.get() == gameTime) {
                BlockState broken = BROKEN_BLOCK.get();
                if (broken != null && toolTier(stack) > blockTier(broken)) {
                    return true; // mined a block of a lower mining tier
                }
                float attackedHp = ATTACKED_HP.get();
                if (attackedHp >= 0.0F && attackedHp <= TraitTuning.FORTUNATE_MOB_HP_LIMIT) {
                    return true; // attacked a non-boss mob at or below 20 HP
                }
                float incoming = INCOMING_DAMAGE.get();
                if (incoming >= 0.0F && incoming < TraitTuning.FORTUNATE_ARMOR_DAMAGE_LIMIT) {
                    return true; // small hit on worn armor
                }
            }
        }
        return false;
    }

    /** Publish the context that the durability of the NEXT stack damage this tick can use. */
    public static void publishBreakContext(BlockState state, long gameTime) {
        BROKEN_BLOCK.set(state);
        ATTACKED_HP.set(-1.0F);
        INCOMING_DAMAGE.set(-1.0F);
        CONTEXT_TICK.set(gameTime);
    }

    public static void publishAttackContext(float targetHp, long gameTime) {
        ATTACKED_HP.set(targetHp);
        BROKEN_BLOCK.set(null);
        INCOMING_DAMAGE.set(-1.0F);
        CONTEXT_TICK.set(gameTime);
    }

    public static void publishArmorContext(float incomingDamage, long gameTime) {
        INCOMING_DAMAGE.set(incomingDamage);
        BROKEN_BLOCK.set(null);
        ATTACKED_HP.set(-1.0F);
        CONTEXT_TICK.set(gameTime);
    }

    public static void clearContext() {
        BROKEN_BLOCK.remove();
        ATTACKED_HP.remove();
        INCOMING_DAMAGE.remove();
        CONTEXT_TICK.remove();
    }

    // ------------------------------------------------------------------ tiers

    /** The tool's harvest tier level from its head material (-1 if not a tool). */
    public static int toolTier(ItemStack stack) {
        var modular = stack.get(com.modulardreams.component.ModDataComponents.MODULAR_DATA);
        if (modular == null) {
            return -1;
        }
        return com.modulardreams.equipment.ModularToolType.byId(modular.equipmentType())
                .flatMap(type -> modular.materialOf(type.headPart().id))
                .flatMap(id -> com.modulardreams.material.ModMaterials.byId(id))
                .map(m -> m.tier().level)
                .orElse(-1);
    }

    /** The minimum tier level required to harvest the block (0 = any tool). */
    public static int blockTier(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return 3;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return 2;
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return 1;
        }
        return 0;
    }
}
