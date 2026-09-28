package com.modulardreams.recipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.modulardreams.ModularDreams;

/**
 * Registry entries of the Part Builder recipe system.
 *
 * The {@code modular_dreams:part_builder} recipe type is a sibling of the
 * vanilla {@code minecraft:stonecutting} type: data-driven JSON recipes with a
 * single input ingredient and a single output, but a completely independent
 * registry entry. The vanilla stonecutter can never craft parts and the Part
 * Builder can never craft stonecutter recipes.
 */
public final class ModRecipeTypes {

        private ModRecipeTypes() {}

        public static final ResourceKey<RecipeType<?>> PART_BUILDER_TYPE_KEY = ResourceKey.create(
                        net.minecraft.core.registries.Registries.RECIPE_TYPE, ModularDreams.id("part_builder"));
        public static final ResourceKey<RecipeSerializer<?>> PART_BUILDER_SERIALIZER_KEY = ResourceKey.create(
                        net.minecraft.core.registries.Registries.RECIPE_SERIALIZER, ModularDreams.id("part_builder"));

        public static final RecipeType<PartBuilderRecipe> PART_BUILDER_TYPE = Registry.register(
                        BuiltInRegistries.RECIPE_TYPE, PART_BUILDER_TYPE_KEY, new RecipeType<PartBuilderRecipe>() {
                                @Override
                                public String toString() {
                                        return "part_builder";
                                }
                        });

        public static final RecipeSerializer<PartBuilderRecipe> PART_BUILDER_SERIALIZER = Registry.register(
                        BuiltInRegistries.RECIPE_SERIALIZER, PART_BUILDER_SERIALIZER_KEY,
                        new RecipeSerializer<>(PartBuilderRecipe.MAP_CODEC, PartBuilderRecipe.STREAM_CODEC));

        // ---- mold baking (clay mold -> terracotta mold, shape-preserving) ----

        public static final ResourceKey<RecipeSerializer<?>> MOLD_BAKING_SERIALIZER_KEY = ResourceKey.create(
                        net.minecraft.core.registries.Registries.RECIPE_SERIALIZER, ModularDreams.id("mold_baking"));

        public static final RecipeSerializer<MoldBakingRecipe> MOLD_BAKING_SERIALIZER = Registry.register(
                        BuiltInRegistries.RECIPE_SERIALIZER, MOLD_BAKING_SERIALIZER_KEY,
                        new RecipeSerializer<>(MoldBakingRecipe.MAP_CODEC, MoldBakingRecipe.STREAM_CODEC));

        /** @return every registered part recipe of the given type (server side). */
        public static java.util.List<net.minecraft.world.item.crafting.RecipeHolder<PartBuilderRecipe>> allPartRecipes(
                        net.minecraft.world.item.crafting.RecipeManager manager) {
                java.util.List<net.minecraft.world.item.crafting.RecipeHolder<PartBuilderRecipe>> list = new java.util.ArrayList<>();
                for (net.minecraft.world.item.crafting.RecipeHolder<?> holder : manager.getRecipes()) {
                        if (holder.value() instanceof PartBuilderRecipe recipe) {
                                list.add(castHolder(holder));
                        }
                }
                return list;
        }

        @SuppressWarnings("unchecked")
        private static net.minecraft.world.item.crafting.RecipeHolder<PartBuilderRecipe> castHolder(
                        net.minecraft.world.item.crafting.RecipeHolder<?> holder) {
                return (net.minecraft.world.item.crafting.RecipeHolder<PartBuilderRecipe>) holder;
        }

        public static Identifier recipeId(String path) {
                return ModularDreams.id(path);
        }

        static {
                // touch both constants so the entries register on class load
                initialize();
        }

        public static void initialize() {
                // static initialization
        }
}
