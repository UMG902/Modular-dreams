package com.modulardreams.traits;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import com.modulardreams.material.ModTraits;
import com.modulardreams.component.ModDataComponents;

/**
 * Hooks for the two block-drop traits:
 * <ul>
 *   <li>{@code slime} (Vacuum, applied by smithing) - broken blocks go
 *       straight into the player's inventory;</li>
 *   <li>{@code blaze_rod} (Blazing, inherent to the handle) - broken blocks
 *       are smelted automatically when a smelting recipe exists.</li>
 * </ul>
 * {@code BlockDropMixin} opens the context around the player-mining drop
 * call ({@code Block.dropResources} with breaker + tool) and hands every
 * dropped stack to {@link #intercept}; containers and explosions are never
 * affected because the context is only open for the player path.
 */
public final class DropHooks {

    /** Open while the player-mining drop path runs (set/cleared by the mixin). */
    public record Context(ServerPlayer player, boolean vacuum, boolean smelt) {}

    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();

    private DropHooks() {}

    public static void open(ServerPlayer player, ItemStack tool) {
        ModDataComponents.ModularData data = tool.get(ModDataComponents.MODULAR_DATA);
        boolean vacuum = data != null && data.hasModifier(ModTraits.SLIME);
        boolean smelt = TraitAggregator.stackHasTrait(tool, ModTraits.BLAZE_ROD);
        if (vacuum || smelt) {
            ACTIVE.set(new Context(player, vacuum, smelt));
        }
    }

    public static void close() {
        ACTIVE.remove();
    }

    /**
     * Called for every stack the player's block drop is about to scatter.
     * Returns the stack to actually drop, or null when it was vacuumed up.
     */
    public static ItemStack intercept(ServerLevel level, ItemStack stack) {
        Context ctx = ACTIVE.get();
        if (ctx == null || stack.isEmpty()) {
            return stack;
        }
        ItemStack out = stack;
        if (ctx.smelt()) {
            out = smelted(level, out).orElse(out);
        }
        if (ctx.vacuum()) {
            // add() inserts as much as fits (mutating the stack down) and
            // returns whether everything landed; leftovers drop at the player
            if (!ctx.player.getInventory().add(out)) {
                ctx.player.spawnAtLocation(level, out); // leftover drops at the player
            }
            return null;
        }
        return out;
    }

    /** The smelting result of a dropped stack (count multiplied), or empty. */
    public static Optional<ItemStack> smelted(ServerLevel level, ItemStack input) {
        if (!(level.recipeAccess() instanceof RecipeManager manager)) {
            return Optional.empty();
        }
        Optional<RecipeHolder<SmeltingRecipe>> recipe =
                manager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        if (recipe.isEmpty()) {
            return Optional.empty();
        }
        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(input));
        if (result.isEmpty()) {
            return Optional.empty();
        }
        result.setCount(result.getCount() * input.getCount());
        return Optional.of(result);
    }

    // ------------------------------------------------------------------ helpers

    /** Small helper used by the mixin to re-scatter a leftover item. */
    public static void scatter(ServerLevel level, double x, double y, double z, ItemStack stack) {
        // a smelted result can exceed the max stack size: never spawn oversized stacks
        ItemStack rest = stack.copy();
        while (!rest.isEmpty()) {
            ItemStack part = rest.split(Math.min(rest.getCount(), Math.max(1, rest.getMaxStackSize())));
            level.addFreshEntity(new ItemEntity(level, x, y, z, part));
        }
    }

    /** Max-durability bonus factor from the emerald modifier (1.0 = none). */
    public static double durableFactor(boolean reinforced) {
        return 1.0D + (reinforced ? TraitTuning.DURABLE_MULT : 0.0D);
    }
}
