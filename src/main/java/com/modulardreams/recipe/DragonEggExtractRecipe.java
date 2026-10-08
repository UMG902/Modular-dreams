package com.modulardreams.recipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.modulardreams.ModularDreams;
import com.modulardreams.component.ModDataComponents;
import com.modulardreams.material.ModTraits;
import com.modulardreams.stats.StatsEngine;

/**
 * Dragon egg extraction: the egg-carrying tool ALONE in a crafting grid
 * gives the tool back (egg mark removed, its LAST applied modifier deleted,
 * modifier cap back down one step) plus the dragon egg itself, returned via
 * the recipe remainder. The extraction is the only way to undo the third
 * unlock - the egg is one per world, so a player can lend it or move it to
 * another tool, but never duplicate it.
 */
public class DragonEggExtractRecipe extends CustomRecipe {

    public static final MapCodec<DragonEggExtractRecipe> MAP_CODEC = MapCodec.unit(new DragonEggExtractRecipe());

    public static final RecipeSerializer<DragonEggExtractRecipe> SERIALIZER = new RecipeSerializer<>(
            MAP_CODEC, net.minecraft.network.codec.StreamCodec.unit(new DragonEggExtractRecipe()));

    public DragonEggExtractRecipe() {
        super();
    }

    /** Exactly one item in the grid, and it must be a dragon-egg-carrying tool. */
    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack found = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!found.isEmpty()) {
                return false; // more than one item in the grid
            }
            found = stack;
        }
        if (found.isEmpty()) {
            return false;
        }
        ModDataComponents.ModularData data = found.get(ModDataComponents.MODULAR_DATA);
        return data != null && !data.equipmentType().equals("none") && data.hasDragonEgg();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            ModDataComponents.ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
            if (stack.isEmpty() || data == null || !data.hasDragonEgg()) {
                continue;
            }
            ItemStack out = stack.copy();
            out.setCount(1);
            ModDataComponents.ModularData extracted = ModTraits.withDragonEggExtracted(data);
            out.set(ModDataComponents.MODULAR_DATA, extracted);
            StatsEngine.bake(out, extracted);
            return out;
        }
        return ItemStack.EMPTY;
    }

    /** The dragon egg pops back out of the grid slot the tool occupied. */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainders = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ModDataComponents.ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
            if (data != null && data.hasDragonEgg()) {
                remainders.set(i, new ItemStack(Items.DRAGON_EGG));
            }
        }
        return remainders;
    }

    /** Hidden from the recipe book: the extraction is a discovered secret. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<DragonEggExtractRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }
}
