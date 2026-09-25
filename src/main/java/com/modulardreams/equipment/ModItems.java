package com.modulardreams.equipment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.component.ModDataComponents.PartData;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.StatsEngine;

/**
 * Registers the 7 modular tools, 4 armor pieces and the guide book, and
 * provides factory helpers to build assembled example items.
 */
public class ModItems {

	private static final Map<String, Item> BY_NAME = new LinkedHashMap<>();

	public static final Map<String, Item> registry() {
		return BY_NAME;
	}

	public static Optional<Item> byName(String name) {
		return Optional.ofNullable(BY_NAME.get(name));
	}

	public static void initialize() {
		ModPartItems.registerAll();
		registerItem("modular_pickaxe", ModularToolItem::new);
		registerItem("modular_axe", ModularToolItem::new);
		registerItem("modular_shovel", ModularToolItem::new);
		registerItem("modular_hoe", ModularToolItem::new);
		registerItem("modular_sword", ModularToolItem::new);
		registerItem("modular_mace", ModularMaceItem::new);
		registerItem("modular_spear", ModularToolItem::new);
		registerItem("modular_helmet", ModularArmorItem::new);
		registerItem("modular_chestplate", ModularArmorItem::new);
		registerItem("modular_leggings", ModularArmorItem::new);
		registerItem("modular_boots", ModularArmorItem::new);
		registerItem("modular_guidebook", GuideBookItem::new);
	}

	private static void registerItem(String name, java.util.function.Function<Item.Properties, Item> factory) {
		Item item = com.modulardreams.registry.ModRegistry.registerItem(name, factory);
		BY_NAME.put(name, item);
	}

	// ------------------------------------------------------------------ factory helpers

	/** Builds an assembled, fully-baked tool stack (used for recipes & creative tab). */
	public static ItemStack toolStack(ModularToolType type, String headMaterial, String handleMaterial,
			String bindingMaterial) {
		List<PartData> parts = new java.util.ArrayList<>();
		parts.add(new PartData(type.headPart().id, headMaterial));
		if (type.bindingPart().isPresent() && bindingMaterial != null) {
			PartType binding = type.bindingPart().get();
			parts.add(new PartData(binding.id, bindingMaterial));
		}
		parts.add(new PartData(PartType.HANDLE.id, handleMaterial));
		ItemStack stack = new ItemStack(byNameOrThrow("modular_" + type.id));
		ModularData data = new ModularData(type.id, List.copyOf(parts), List.of());
		stack.set(ModDataComponents.MODULAR_DATA, data);
		StatsEngine.bake(stack, data);
		return stack;
	}

	public static ItemStack armorStack(ModularArmorType type, String plateMaterial, String liningMaterial) {
		List<PartData> parts = List.of(
				new PartData(PartType.PLATE.id, plateMaterial),
				new PartData(PartType.LINING.id, liningMaterial));
		ItemStack stack = new ItemStack(byNameOrThrow("modular_" + type.id));
		ModularData data = new ModularData(type.id, parts, List.of());
		stack.set(ModDataComponents.MODULAR_DATA, data);
		StatsEngine.bake(stack, data);
		return stack;
	}

	/** A fresh, empty modular tool that is not yet assembled (used as recipe output fallback). */
	public static ItemStack unassembled(String name) {
		return new ItemStack(byNameOrThrow(name));
	}

	private static Item byNameOrThrow(String name) {
		Item item = BY_NAME.get(name);
		if (item == null) {
			throw new IllegalStateException("Modular item not registered: " + name);
		}
		return item;
	}
}
