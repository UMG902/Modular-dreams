package com.modulardreams.part;

import java.util.List;

import com.modulardreams.material.ModMaterials;

/**
 * All part types of Modular Dreams, Tinkers'-style.
 *
 * Tools are assembled from a head, a handle and (optionally) a binding or a
 * guard; armor is assembled from plates and a lining. Each part type can be
 * crafted from a defined set of vanilla materials.
 */
public enum PartType {
	PICKAXE_HEAD("pickaxe_head", MaterialClass.TOOL),
	AXE_HEAD("axe_head", MaterialClass.TOOL),
	SHOVEL_HEAD("shovel_head", MaterialClass.TOOL),
	HOE_HEAD("hoe_head", MaterialClass.TOOL),
	SWORD_BLADE("sword_blade", MaterialClass.TOOL),
	SWORD_GUARD("sword_guard", MaterialClass.TOOL),
	MACE_HEAD("mace_head", MaterialClass.TOOL),
	SPEAR_HEAD("spear_head", MaterialClass.TOOL),
	BINDING("binding", MaterialClass.TOOL),
	HANDLE("handle", MaterialClass.TOOL),
	PLATE("plate", MaterialClass.PLATE),
	LINING("lining", MaterialClass.LINING);

	public enum MaterialClass {
		TOOL, PLATE, LINING
	}

	public final String id;
	public final MaterialClass materialClass;

	PartType(String id, MaterialClass materialClass) {
		this.id = id;
		this.materialClass = materialClass;
	}

	public String translationKey() {
		return "part.modular_dreams." + id;
	}

	public boolean isHead() {
		return this == PICKAXE_HEAD || this == AXE_HEAD || this == SHOVEL_HEAD || this == HOE_HEAD
				|| this == SWORD_BLADE || this == MACE_HEAD || this == SPEAR_HEAD;
	}

	public List<com.modulardreams.material.ModularMaterial> allowedMaterials() {
		return switch (materialClass) {
			case TOOL -> ModMaterials.toolMaterials();
			case PLATE -> ModMaterials.plateMaterials();
			case LINING -> ModMaterials.liningMaterials();
		};
	}

	public static void initialize() {
		// static init of all enums
		values();
	}
}
