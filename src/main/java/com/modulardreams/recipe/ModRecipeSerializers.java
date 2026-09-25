package com.modulardreams.recipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

import net.minecraft.core.registries.Registries;

import com.modulardreams.ModularDreams;

/**
 * Registers the three custom recipe serializers of Modular Dreams.
 *
 * All three are singleton {@code CustomRecipe}s: their JSON form is just
 * {@code {"type": "modular_dreams:<id>"}} and all logic lives in code.
 */
public class ModRecipeSerializers {

	public static final ResourceKey<net.minecraft.world.item.crafting.RecipeSerializer<?>> ASSEMBLY_KEY = key(
			"assembly");
	public static final ResourceKey<net.minecraft.world.item.crafting.RecipeSerializer<?>> MODIFIER_KEY = key(
			"modifier");
	public static final ResourceKey<net.minecraft.world.item.crafting.RecipeSerializer<?>> REPAIR_KEY = key(
			"repair");

	private static ResourceKey<net.minecraft.world.item.crafting.RecipeSerializer<?>> key(String name) {
		return ResourceKey.create(Registries.RECIPE_SERIALIZER, ModularDreams.id(name));
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, ASSEMBLY_KEY, AssemblyRecipe.SERIALIZER);
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, MODIFIER_KEY, ModifierRecipe.SERIALIZER);
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, REPAIR_KEY, RepairRecipe.SERIALIZER);
	}
}
