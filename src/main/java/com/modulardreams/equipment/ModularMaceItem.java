package com.modulardreams.equipment;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.stats.StatsEngine;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

/**
 * Modular mace: inherits the vanilla mace smash attack mechanics (fall damage
 * bonus, knockback waves, ...) while stats come from the modular parts.
 */
public class ModularMaceItem extends MaceItem {

	public ModularMaceItem(Properties properties) {
		super(properties);
		((net.fabricmc.fabric.impl.item.ItemExtensions) this)
				.fabric_setCustomDamageHandler(this::hurtAndBreakWithTraits);
	}

	private int hurtAndBreakWithTraits(ItemStack stack, int amount, LivingEntity entity,
			net.minecraft.world.entity.EquipmentSlot slot, Runnable breakCallback) {
		ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
		if (data == null) {
			return amount;
		}
		float reduction = StatsEngine.durabilityDamageReduction(stack, data);
		return Math.max(1, Math.round(amount * (1.0F - reduction)));
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
		if (data != null && !target.level().isClientSide()) {
			float seconds = StatsEngine.fierySeconds(stack, data);
			if (seconds > 0.0F && !target.fireImmune()) {
				target.igniteForTicks(Math.round(seconds * 20.0F));
			}
		}
	}
}
