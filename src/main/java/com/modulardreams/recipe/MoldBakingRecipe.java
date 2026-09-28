package com.modulardreams.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.equipment.MoldBlockItem;

/**
 * Furnace recipe that bakes a shaped CLAY mold into a TERRACOTA mold of the
 * SAME shape. It lives in the vanilla {@code minecraft:smelting} recipe type
 * (so any furnace can cook it), but uses its own serializer because the
 * output must copy the input's {@code mold_part} component - vanilla smelting
 * recipes cannot carry component data over.
 */
public class MoldBakingRecipe extends AbstractCookingRecipe {

        public static final MapCodec<MoldBakingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                        Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
                                        CookingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
                                        Ingredient.CODEC.fieldOf("ingredient").forGetter(MoldBakingRecipe::input),
                                        ItemStackTemplate.MAP_CODEC.fieldOf("result").forGetter(MoldBakingRecipe::resultTemplate))
                                        .apply(instance, MoldBakingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MoldBakingRecipe> STREAM_CODEC = StreamCodec.composite(
                        Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
                        CookingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
                        Ingredient.CONTENTS_STREAM_CODEC, MoldBakingRecipe::input,
                        ItemStackTemplate.STREAM_CODEC, MoldBakingRecipe::resultTemplate,
                        MoldBakingRecipe::new);

        public MoldBakingRecipe(Recipe.CommonInfo commonInfo, CookingBookInfo bookInfo,
                        Ingredient input, ItemStackTemplate result) {
                super(commonInfo, bookInfo, input, result, 0.0F, 200);
        }

        public ItemStackTemplate resultTemplate() {
                return super.result();
        }

        /**
         * Only SHAPED clay molds bake: an unshaped (blank) mold has no part
         * component, so there is no shape to carry over to the terracotta
         * result - the furnace simply refuses it.
         */
        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
                return super.matches(input, level)
                                && input.item().get(ModDataComponents.MOLD_PART) != null;
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input) {
                ItemStack result = super.assemble(input);
                String part = input.item().get(ModDataComponents.MOLD_PART);
                if (part != null && MoldBlockItem.partOf(result).isEmpty()) {
                        result.set(ModDataComponents.MOLD_PART, part);
                }
                return result;
        }

        @Override
        public RecipeSerializer<? extends AbstractCookingRecipe> getSerializer() {
                return ModRecipeTypes.MOLD_BAKING_SERIALIZER;
        }

        /** The vanilla smelting type: this is what makes furnaces cook the mold. */
        @Override
        public RecipeType<? extends AbstractCookingRecipe> getType() {
                return RecipeType.SMELTING;
        }

        @Override
        protected net.minecraft.world.item.Item furnaceIcon() {
                return Items.CLAY_BALL;
        }

        @Override
        public RecipeBookCategory recipeBookCategory() {
                return RecipeBookCategories.FURNACE_MISC;
        }

        /** Kept out of the recipe book / toast systems. */
        @Override
        public boolean isSpecial() {
                return true;
        }
}
