package com.modulardreams.equipment;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.equipment.ModPartItems.PartIdentity;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.ModTraits;

/**
 * A modular armor piece (helmet, chestplate, leggings or boots).
 * All defense/toughness/durability stats are baked per instance.
 */
public class ModularArmorItem extends Item {

	public ModularArmorItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context,
			net.minecraft.world.item.component.TooltipDisplay display,
			java.util.function.Consumer<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
		ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
		if (data == null || data.equipmentType().equals("none")) {
			return;
		}
		ModularArmorType armorType = ModularArmorType.byId(data.equipmentType());
		tooltip.accept(Component.translatable("tooltip.modular_dreams.parts").withStyle(ChatFormatting.DARK_GRAY));
		for (var part : data.parts()) {
			PartType partType = part.part().equals(PartType.PLATE.id) ? PartType.PLATE : PartType.LINING;
			MutableComponent line = Component.literal("")
					.append(Component.translatable(partType.translationKey()).withStyle(ChatFormatting.GRAY))
					.append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
					.append(Component.translatable("material.modular_dreams." + part.material())
							.withStyle(ChatFormatting.GRAY));
			tooltip.accept(line);
			Optional<ModularMaterial> mat = ModDataHooks.material(part.material());
			if (mat.isPresent() && !mat.get().traits().isEmpty()) {
				MutableComponent traitLine = Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY);
				List<ModTraits> traits = mat.get().traits();
				for (int i = 0; i < traits.size(); i++) {
					if (i > 0) {
						traitLine.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
					}
					traitLine.append(Component.translatable(traits.get(i).nameKey()).withStyle(ChatFormatting.BLUE));
				}
				tooltip.accept(traitLine);
			}
		}
		ModDataHooks.appendModifierTooltip(data, tooltip);
	}
}
