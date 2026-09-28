package com.modulardreams.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;

import com.modulardreams.ModularDreams;
import com.modulardreams.block.ModBlocks;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.menu.StationLogic;
import com.modulardreams.part.PartType;
import com.modulardreams.recipe.MoldBakingRecipe;
import com.modulardreams.recipe.PartBuilderRecipe;

/**
 * Vanilla crafting recipes of the overhaul build:
 *
 * - Part Builder block: 9 logs (placeholder, as designed)
 * - Assembly Table block: 3 logs in a vertical column (placeholder)
 * - Clay Mold block: 2x2 clay balls (placeholder)
 * - Melting Upgrade block: iron ingots around a furnace on stone (placeholder)
 * - Guide book: book + stick (placeholder)
 * - Part Builder recipes: every material x part combination EXCEPT metals
 *   (metal parts come exclusively from the Melting Upgrade).
 * - Mold baking: one shape-preserving smelting recipe (clay -> terracotta).
 */
public class ModRecipeProvider extends FabricRecipeProvider {

        protected ModRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
                super(output, registriesFuture);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registryLookup,
                        net.minecraft.data.worldgen.BootstrapContext<net.minecraft.world.item.crafting.Recipe<?>> recipeContext,
                        net.minecraft.data.worldgen.BootstrapContext<net.minecraft.advancements.Advancement> advancementContext) {
                return new RecipeProvider(recipeContext, advancementContext) {

                        @Override
                        public void buildRecipes() {
                                RecipeOutput output = this.output;
                                generateStationRecipes(output);
                                generateGuideRecipe(output);
                                generatePartRecipes(output);
                                generateMoldBakingRecipe(output);
                        }

                        /**
                         * Every material x part combination as a data-driven
                         * {@code modular_dreams:part_builder} recipe (stonecutter-style:
                         * one input ingredient, one output, plus the material cost the
                         * Part Builder menu enforces). Metals are NOT part-builder
                         * materials: copper, iron and gold parts can only be melted.
                         */
                        private void generatePartRecipes(RecipeOutput output) {
                                for (PartType part : PartType.values()) {
                                        for (ModularMaterial material : part.allowedMaterials()) {
                                                if (com.modulardreams.material.ModMaterials.isMeltableMetal(material.id())) {
                                                        continue;
                                                }
                                                PartBuilderRecipe recipe = new PartBuilderRecipe(
                                                                new net.minecraft.world.item.crafting.Recipe.CommonInfo(true),
                                                                material.crafting(),
                                                                new net.minecraft.world.item.ItemStackTemplate(
                                                                                ModPartItems.get(part, material),
                                                                                1),
                                                                part.cost());
                                                output.accept(ResourceKey.create(Registries.RECIPE,
                                                                ModularDreams.id("parts/" + material.id() + "_" + part.id)),
                                                                recipe, null);
                                        }
                                }
                        }

                        /** Shape-preserving furnace recipe: shaped clay mold -> terracotta mold. */
                        private void generateMoldBakingRecipe(RecipeOutput output) {
                                MoldBakingRecipe recipe = new MoldBakingRecipe(
                                                new net.minecraft.world.item.crafting.Recipe.CommonInfo(true),
                                                new net.minecraft.world.item.crafting.AbstractCookingRecipe.CookingBookInfo(
                                                                net.minecraft.world.item.crafting.CookingBookCategory.MISC, ""),
                                                Ingredient.of(ModBlocks.CLAY_MOLD),
                                                new net.minecraft.world.item.ItemStackTemplate(
                                                                ModBlocks.TERRACOTTA_MOLD.asItem(), 1));
                                output.accept(ResourceKey.create(Registries.RECIPE, ModularDreams.id("mold_baking")),
                                                recipe, null);
                        }

                        private void generateStationRecipes(RecipeOutput output) {
                                // Part Builder: 9 logs (placeholder)
                                shaped(RecipeCategory.DECORATIONS, ModBlocks.PART_BUILDER)
                                                .pattern("LLL")
                                                .pattern("LLL")
                                                .pattern("LLL")
                                                .define('L', com.modulardreams.material.ModMaterials.ingredientOfTag(net.minecraft.tags.ItemTags.LOGS))
                                                .unlockedBy("has_logs", has(Items.OAK_LOG))
                                                .save(output, stationKey("part_builder"));

                                // Assembly Table: 3 logs in a vertical column (placeholder)
                                shaped(RecipeCategory.DECORATIONS, ModBlocks.ASSEMBLY_TABLE)
                                                .pattern("L")
                                                .pattern("L")
                                                .pattern("L")
                                                .define('L', com.modulardreams.material.ModMaterials.ingredientOfTag(net.minecraft.tags.ItemTags.LOGS))
                                                .unlockedBy("has_logs", has(Items.OAK_LOG))
                                                .save(output, stationKey("assembly_table"));

                                // Clay Mold: 2x2 clay balls (placeholder)
                                shaped(RecipeCategory.DECORATIONS, ModBlocks.CLAY_MOLD)
                                                .pattern("CC")
                                                .pattern("CC")
                                                .define('C', Ingredient.of(Items.CLAY_BALL))
                                                .unlockedBy("has_clay", has(Items.CLAY_BALL))
                                                .save(output, stationKey("clay_mold"));

                                // Melting Upgrade: iron ring around a furnace, stone base (placeholder)
                                shaped(RecipeCategory.DECORATIONS, ModBlocks.MELTING_UPGRADE)
                                                .pattern("III")
                                                .pattern("IFI")
                                                .pattern("SSS")
                                                .define('I', Ingredient.of(Items.IRON_INGOT))
                                                .define('F', Ingredient.of(Items.FURNACE))
                                                .define('S', Ingredient.of(Items.STONE))
                                                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                                                .save(output, stationKey("melting_upgrade"));
                        }

                        private void generateGuideRecipe(RecipeOutput output) {
                                ModItems.byName("modular_guidebook").ifPresent(guide -> shaped(RecipeCategory.MISC, guide)
                                                .pattern("B")
                                                .pattern("S")
                                                .define('B', Ingredient.of(Items.BOOK))
                                                .define('S', Ingredient.of(Items.STICK))
                                                .unlockedBy("has_book", has(Items.BOOK))
                                                .save(output, stationKey("modular_guidebook")));
                        }

                        private ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> stationKey(String name) {
                                return ResourceKey.create(Registries.RECIPE, ModularDreams.id(name));
                        }
                };
        }

        @Override
        public String getName() {
                return "Modular Dreams Recipes";
        }
}
