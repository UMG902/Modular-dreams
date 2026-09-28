package com.modulardreams.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;

/**
 * One "shape a material into a part" recipe of the Part Builder.
 *
 * This is deliberately a SEPARATE recipe type from the vanilla stonecutter
 * (mirrors the vanilla {@code minecraft:stonecutting} type, but its own
 * registry entry): the vanilla stonecutter only ever shows {@code stonecutting}
 * recipes, and the Part Builder only ever shows {@code part_builder} recipes -
 * neither block can process the other's recipes.
 *
 * In addition to the single input ingredient and the result, the recipe carries
 * a {@code cost}: how many units of the input one craft consumes (a pickaxe
 * head costs 3 ingots, a handle 2, ...). Vanilla stonecutter recipes are
 * always 1:1, so the menu enforces/consumes the cost itself.
 */
public class PartBuilderRecipe extends SingleItemRecipe {

        public static final MapCodec<PartBuilderRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                        Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
                                        Ingredient.CODEC.fieldOf("ingredient").forGetter(PartBuilderRecipe::input),
                                        ItemStackTemplate.MAP_CODEC.fieldOf("result").forGetter(PartBuilderRecipe::resultTemplate),
                                        Codec.INT.optionalFieldOf("cost", 1).forGetter(PartBuilderRecipe::cost))
                                        .apply(instance, PartBuilderRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, PartBuilderRecipe> STREAM_CODEC = StreamCodec.composite(
                        Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
                        Ingredient.CONTENTS_STREAM_CODEC, PartBuilderRecipe::input,
                        ItemStackTemplate.STREAM_CODEC, PartBuilderRecipe::resultTemplate,
                        ByteBufCodecs.VAR_INT, PartBuilderRecipe::cost,
                        PartBuilderRecipe::new);

        private final int cost;

        public PartBuilderRecipe(Recipe.CommonInfo commonInfo, Ingredient input, ItemStackTemplate result, int cost) {
                super(commonInfo, input, result);
                this.cost = cost;
        }

        /** How many input items one craft consumes. */
        public int cost() {
                return this.cost;
        }

        public ItemStackTemplate resultTemplate() {
                return super.result();
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
                return this.input().test(input.item());
        }

        @Override
        public RecipeSerializer<? extends SingleItemRecipe> getSerializer() {
                return ModRecipeTypes.PART_BUILDER_SERIALIZER;
        }

        @Override
        public RecipeType<? extends SingleItemRecipe> getType() {
                return ModRecipeTypes.PART_BUILDER_TYPE;
        }

        @Override
        public String group() {
                return "";
        }

        @Override
        public RecipeBookCategory recipeBookCategory() {
                return RecipeBookCategories.STONECUTTER;
        }

        /** Kept out of the recipe book / toast systems entirely. */
        @Override
        public boolean isSpecial() {
                return true;
        }

        @Override
        public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
                return java.util.List.of();
        }
}
