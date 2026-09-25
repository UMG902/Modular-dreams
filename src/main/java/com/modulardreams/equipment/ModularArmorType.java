package com.modulardreams.equipment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * The four modular armor slots, inspired by Construct's Armory.
 * Each piece is assembled from plates (main material) and a lining (padding).
 */
public enum ModularArmorType {
	HELMET("helmet", ArmorType.HELMET, EquipmentSlot.HEAD, 3),
	CHESTPLATE("chestplate", ArmorType.CHESTPLATE, EquipmentSlot.CHEST, 4),
	LEGGINGS("leggings", ArmorType.LEGGINGS, EquipmentSlot.LEGS, 3),
	BOOTS("boots", ArmorType.BOOTS, EquipmentSlot.FEET, 2);

	public final String id;
	public final ArmorType armorType;
	public final EquipmentSlot slot;
	/** how many plates are required to assemble this piece */
	public final int plateCount;

	ModularArmorType(String id, ArmorType armorType, EquipmentSlot slot, int plateCount) {
		this.id = id;
		this.armorType = armorType;
		this.slot = slot;
		this.plateCount = plateCount;
	}

	public String translationKey() {
		return "equipment.modular_dreams." + id;
	}

	public static ModularArmorType byId(String id) {
		for (ModularArmorType type : values()) {
			if (type.id.equals(id)) {
				return type;
			}
		}
		throw new IllegalStateException("Unknown armor type: " + id);
	}
}
