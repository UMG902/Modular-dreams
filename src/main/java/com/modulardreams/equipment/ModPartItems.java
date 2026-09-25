package com.modulardreams.equipment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.core.registries.Registries;

import com.modulardreams.ModularDreams;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.ModTraits;

/**
 * All {@link PartType} x material part items (e.g. "Iron Pickaxe Head").
 * Parts are plain crafting ingredients; all stats live in the assembled item.
 */
public class ModPartItems {

	/** A part item instance carrying its identity. */
	public static class PartItem extends Item {
		private final PartType partType;
		private final ModularMaterial material;

		public PartItem(PartType partType, ModularMaterial material, Properties properties) {
			super(properties);
			this.partType = partType;
			this.material = material;
		}

		public PartType partType() {
			return partType;
		}

		public ModularMaterial material() {
			return material;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context,
				net.minecraft.world.item.component.TooltipDisplay display,
				java.util.function.Consumer<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
			tooltip.accept(Component.translatable("part.modular_dreams.material_line",
					Component.translatable(material.nameKey())).withStyle(ChatFormatting.GRAY));
			for (ModTraits trait : material.traits()) {
				if (trait.category.matches(true)) {
					tooltip.accept(Component.translatable(trait.nameKey()).withStyle(ChatFormatting.BLUE)
							.append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
							.append(Component.translatable(trait.descriptionKey())
									.withStyle(ChatFormatting.DARK_GRAY)));
				}
			}
		}
	}

	private static final Map<String, Item> BY_NAME = new LinkedHashMap<>();
	private static final Map<PartType, List<Item>> BY_PART = new LinkedHashMap<>();

	public static final Map<String, Item> registry() {
		return BY_NAME;
	}

	public static Item get(PartType part, ModularMaterial material) {
		Item item = BY_NAME.get(material.id() + "_" + part.id);
		if (item == null) {
			throw new IllegalStateException("No part item for " + material.id() + "_" + part.id);
		}
		return item;
	}

	public static List<Item> itemsOf(PartType part) {
		return BY_PART.getOrDefault(part, List.of());
	}

	/** @return the part identity of this stack, if it is a modular part. */
	public static Optional<PartIdentity> resolve(ItemStack stack) {
		if (stack.getItem() instanceof PartItem partItem) {
			return Optional.of(new PartIdentity(partItem.partType(), partItem.material()));
		}
		return Optional.empty();
	}

	public record PartIdentity(PartType part, ModularMaterial material) {}

	public static void registerAll() {
		for (PartType part : PartType.values()) {
			BY_PART.put(part, new ArrayList<>());
			for (ModularMaterial material : part.allowedMaterials()) {
				register(part, material);
			}
		}
	}

	private static void register(PartType part, ModularMaterial material) {
		String name = material.id() + "_" + part.id;
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ModularDreams.id(name));
		PartItem item = new PartItem(part, material, new Item.Properties().setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		BY_NAME.put(name, item);
		BY_PART.get(part).add(item);
	}
}
