package com.modulardreams.datagen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;

import com.modulardreams.ModularDreams;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;
import com.modulardreams.recipe.AssemblyRecipe;
import com.modulardreams.recipe.ModifierRecipe;
import com.modulardreams.recipe.RepairRecipe;

/**
 * Generates: part crafting recipes for every material x part combination, the
 * three special recipes (assembly / modifier / repair) and the guide book recipe.
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
				generatePartRecipes(output);
				generateSpecialRecipes(output);
				generateGuideRecipe(output);
			}

			private void generatePartRecipes(RecipeOutput output) {
				for (PartType part : PartType.values()) {
					for (ModularMaterial material : part.allowedMaterials()) {
						Item result = ModPartItems.get(part, material);
						Item materialItem = ModRecipeMaterial.craftingItemFor(material);
						ModRecipeMaterial.PartPattern pattern = ModRecipeMaterial.patternOf(part);
						ShapedRecipeBuilder builder = shaped(RecipeCategory.MISC, result, pattern.resultCount());
						for (Map.Entry<Character, String> row : pattern.rows().entrySet()) {
							builder.pattern(row.getValue());
						}
						for (Map.Entry<Character, Ingredient> slot : pattern.define(materialItem).entrySet()) {
							builder.define(slot.getKey(), slot.getValue());
						}
						builder.unlockedBy("has_" + material.id(), has(materialItem))
								.save(output, ResourceKey.create(Registries.RECIPE,
										ModularDreams.id(material.id() + "_" + part.id)));
					}
				}
			}

			private void generateSpecialRecipes(RecipeOutput output) {
				SpecialRecipeBuilder.special(() -> AssemblyRecipe.INSTANCE)
						.save(output, ResourceKey.create(Registries.RECIPE, ModularDreams.id("assembly")));
				SpecialRecipeBuilder.special(() -> ModifierRecipe.INSTANCE)
						.save(output, ResourceKey.create(Registries.RECIPE, ModularDreams.id("modifier")));
				SpecialRecipeBuilder.special(() -> RepairRecipe.INSTANCE)
						.save(output, ResourceKey.create(Registries.RECIPE, ModularDreams.id("repair")));
			}

			private void generateGuideRecipe(RecipeOutput output) {
				ModItems.byName("modular_guidebook").ifPresent(guide -> shaped(RecipeCategory.MISC, guide)
						.pattern("B")
						.pattern("I")
						.define('B', Ingredient.of(Items.BOOK))
						.define('I', Ingredient.of(Items.IRON_INGOT))
						.unlockedBy("has_book", has(Items.BOOK))
						.save(output, ResourceKey.create(Registries.RECIPE,
								ModularDreams.id("modular_guidebook"))));
			}
		};
	}

	@Override
	public String getName() {
		return "Modular Dreams Recipes";
	}
}

/** Static data tables for the recipe generator. */
final class ModRecipeMaterial {

	private ModRecipeMaterial() {}

	/** The raw item a material's parts are crafted from. */
	static Item craftingItemFor(ModularMaterial material) {
		return switch (material.id()) {
			case "oak" -> Items.OAK_PLANKS;
			case "spruce" -> Items.SPRUCE_PLANKS;
			case "birch" -> Items.BIRCH_PLANKS;
			case "cherry" -> Items.CHERRY_PLANKS;
			case "bamboo" -> Items.BAMBOO_PLANKS;
			case "crimson" -> Items.CRIMSON_PLANKS;
			case "warped" -> Items.WARPED_PLANKS;
			case "bone" -> Items.BONE;
			case "flint" -> Items.FLINT;
			case "stone" -> Items.COBBLESTONE;
			case "deepslate" -> Items.COBBLED_DEEPSLATE;
			case "blackstone" -> Items.BLACKSTONE;
			case "obsidian" -> Items.OBSIDIAN;
			case "amethyst" -> Items.AMETHYST_SHARD;
			case "copper" -> Items.COPPER_INGOT;
			case "iron" -> Items.IRON_INGOT;
			case "gold" -> Items.GOLD_INGOT;
			case "diamond" -> Items.DIAMOND;
			case "netherite" -> Items.NETHERITE_INGOT;
			case "turtle_scute" -> Items.TURTLE_SCUTE;
			case "armadillo_scute" -> Items.ARMADILLO_SCUTE;
			case "leather" -> Items.LEATHER;
			case "rabbit_hide" -> Items.RABBIT_HIDE;
			case "phantom_membrane" -> Items.PHANTOM_MEMBRANE;
			case "wool" -> Items.WOOL.pick(DyeColor.WHITE);
			case "slime" -> Items.SLIME_BALL;
			default -> throw new IllegalStateException("No crafting item for " + material.id());
		};
	}

	static TagKey<Item> tagOf(String name) {
		return TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.withDefaultNamespace(name));
	}

	record PartPattern(LinkedHashMap<Character, String> rows, boolean needsStick, int resultCount) {
		Map<Character, Ingredient> define(Item materialItem) {
			LinkedHashMap<Character, Ingredient> map = new LinkedHashMap<>();
			for (char c : String.join("", rows.values()).toCharArray()) {
				if (c == 'X') {
					map.put(c, Ingredient.of(materialItem));
				} else if (c == 'S') {
					map.put(c, Ingredient.of(Items.STICK));
				}
			}
			return map;
		}
	}

	static PartPattern patternOf(PartType part) {
		return switch (part) {
			case SHOVEL_HEAD -> new PartPattern(rows("X"), false, 1);
			case LINING -> new PartPattern(rows("X"), false, 1);
			case HOE_HEAD -> new PartPattern(rows("XX"), false, 1);
			case SWORD_GUARD -> new PartPattern(rows("X X"), false, 1);
			case BINDING -> new PartPattern(rows("X", "X"), false, 1);
			case HANDLE -> new PartPattern(rows("X", "S"), true, 1);
			case PICKAXE_HEAD -> new PartPattern(rows("XXX"), false, 1);
			case AXE_HEAD -> new PartPattern(rows("XX", "X "), false, 1);
			case SWORD_BLADE -> new PartPattern(rows("X", "X", "X"), false, 1);
			case SPEAR_HEAD -> new PartPattern(rows("XX", " X"), false, 1);
			case MACE_HEAD -> new PartPattern(rows("XXX", " X "), false, 1);
			case PLATE -> new PartPattern(rows("XX", "XX"), false, 2);
		};
	}

	private static LinkedHashMap<Character, String> rows(String... patterns) {
		LinkedHashMap<Character, String> map = new LinkedHashMap<>();
		char key = 'A';
		for (String row : patterns) {
			map.put(key++, row);
		}
		return map;
	}
}
