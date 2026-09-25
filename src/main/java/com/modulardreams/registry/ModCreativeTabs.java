package com.modulardreams.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.minecraft.core.registries.Registries;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.modulardreams.ModularDreams;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModularArmorType;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.part.PartType;

/**
 * The "Modular Dreams" creative tab: guide book, example equipment, and every part.
 */
public class ModCreativeTabs {

	public static final ResourceKey<CreativeModeTab> MODULAR_TAB_KEY = ResourceKey
			.create(Registries.CREATIVE_MODE_TAB, ModularDreams.id("modular_tab"));

	public static void initialize() {
		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.icon(() -> ModItems.byName("modular_guidebook").map(ItemStack::new).orElse(ItemStack.EMPTY))
				.title(Component.translatable("itemGroup.modular_dreams"))
				.displayItems((parameters, output) -> {
					// guide first
					ModItems.byName("modular_guidebook").ifPresent(output::accept);

					// example assembled equipment
					output.accept(ModItems.toolStack(ModularToolType.PICKAXE, "iron", "oak", "iron"));
					output.accept(ModItems.toolStack(ModularToolType.AXE, "iron", "oak", "iron"));
					output.accept(ModItems.toolStack(ModularToolType.SHOVEL, "iron", "oak", null));
					output.accept(ModItems.toolStack(ModularToolType.HOE, "iron", "oak", null));
					output.accept(ModItems.toolStack(ModularToolType.SWORD, "iron", "oak", "iron"));
					output.accept(ModItems.toolStack(ModularToolType.MACE, "iron", "oak", null));
					output.accept(ModItems.toolStack(ModularToolType.SPEAR, "iron", "oak", "iron"));
					output.accept(ModItems.armorStack(ModularArmorType.HELMET, "iron", "leather"));
					output.accept(ModItems.armorStack(ModularArmorType.CHESTPLATE, "iron", "leather"));
					output.accept(ModItems.armorStack(ModularArmorType.LEGGINGS, "iron", "leather"));
					output.accept(ModItems.armorStack(ModularArmorType.BOOTS, "iron", "leather"));

					// diamond + netherite showcases
					output.accept(ModItems.toolStack(ModularToolType.PICKAXE, "diamond", "diamond", "diamond"));
					output.accept(ModItems.toolStack(ModularToolType.SWORD, "netherite", "netherite", "netherite"));
					output.accept(ModItems.toolStack(ModularToolType.SPEAR, "netherite", "netherite", "netherite"));

					// all parts, grouped by part type
					for (PartType part : PartType.values()) {
						for (net.minecraft.world.item.Item item : com.modulardreams.equipment.ModPartItems
								.itemsOf(part)) {
							output.accept(item);
						}
					}
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MODULAR_TAB_KEY, tab);
	}
}
