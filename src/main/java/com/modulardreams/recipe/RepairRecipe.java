package com.modulardreams.recipe;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.equipment.ModDataHooks;

/**
 * Tinkers'-style repair: combine a damaged modular item with ONE of its repair
 * materials to restore 10% of its max durability (uses, not re-crafts!).
 *
 * The repair material is whatever the item's REPAIRABLE component accepts
 * (the head/plate material's repair items, plus sticks for ROOTED wood).
 */
public class RepairRecipe extends CustomRecipe {

	public static final RepairRecipe INSTANCE = new RepairRecipe();
	public static final MapCodec<RepairRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, RepairRecipe> STREAM_CODEC = StreamCodec
			.unit(INSTANCE);
	public static final RecipeSerializer<RepairRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	/** fraction of max durability restored per repair material item */
	public static final float FRACTION = 0.10F;

	private RepairRecipe() {
		super();
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return findPair(input).isPresent();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		Optional<ItemStack> toolOpt = findPair(input);
		if (toolOpt.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack tool = toolOpt.get().copy();
		int max = tool.getMaxDamage();
		int damage = tool.getDamageValue();
		int heal = Math.max(1, Math.round(max * FRACTION));
		tool.setDamageValue(Math.max(0, damage - heal));
		return tool;
	}

	/** @return the damaged modular tool of a valid [modular tool + repair item] grid. */
	private Optional<ItemStack> findPair(CraftingInput input) {
		ItemStack modularStack = null;
		ItemStack material = null;
		int count = 0;
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			count++;
			if (ModDataHooks.dataOf(stack).isPresent()) {
				if (modularStack != null) {
					return Optional.empty();
				}
				modularStack = stack;
			} else if (material == null) {
				material = stack;
			} else {
				return Optional.empty();
			}
		}
		if (count != 2 || modularStack == null || material == null) {
			return Optional.empty();
		}
		ModularData data = ModDataHooks.dataOf(modularStack).orElseThrow();
		if (!modularStack.isDamageableItem() || modularStack.getDamageValue() <= 0) {
			return Optional.empty();
		}
		var repairable = modularStack.getItem().components().get(DataComponents.REPAIRABLE);
		if (repairable == null || !repairable.isValidRepairItem(material)) {
			return Optional.empty();
		}
		// never consume a cost item that belongs to an applicable modifier (those need 2+)
		Optional<com.modulardreams.modifier.Modifier> asCost = com.modulardreams.modifier.Modifier
				.byCostItem(material.getItem());
		if (asCost.isPresent() && asCost.get().costPerLevel() == 1
				&& asCost.get().category().matches(
						com.modulardreams.equipment.ModularToolType.byId(data.equipmentType()).isPresent())) {
			return Optional.empty();
		}
		return Optional.of(modularStack);
	}

	@Override
	public RecipeSerializer<? extends CustomRecipe> getSerializer() {
		return SERIALIZER;
	}
}
