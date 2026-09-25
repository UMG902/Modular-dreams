package com.modulardreams.equipment;

import java.util.List;
import java.util.Optional;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.modulardreams.part.PartType;

/**
 * The modular tool & weapon types, mirroring the vanilla tool set including the
 * mace and the spear. Base stats are chosen so that a modular tool with vanilla
 * material X performs like its vanilla counterpart (attack damage/speed parity).
 */
public enum ModularToolType {
	PICKAXE("pickaxe", 1.0F, -2.8F, BlockTags.MINEABLE_WITH_PICKAXE,
			List.of(PartType.PICKAXE_HEAD, PartType.BINDING, PartType.HANDLE), 1, 0.0F),
	AXE("axe", 6.0F, -3.1F, BlockTags.MINEABLE_WITH_AXE,
			List.of(PartType.AXE_HEAD, PartType.BINDING, PartType.HANDLE), 1, 5.0F),
	SHOVEL("shovel", 1.5F, -3.0F, BlockTags.MINEABLE_WITH_SHOVEL,
			List.of(PartType.SHOVEL_HEAD, PartType.HANDLE), 1, 0.0F),
	HOE("hoe", 0.0F, -1.0F, BlockTags.MINEABLE_WITH_HOE,
			List.of(PartType.HOE_HEAD, PartType.HANDLE), 1, 0.0F),
	SWORD("sword", 3.0F, -2.4F, null,
			List.of(PartType.SWORD_BLADE, PartType.SWORD_GUARD, PartType.HANDLE), 1, 0.0F),
	MACE("mace", 4.0F, -3.4F, null,
			List.of(PartType.MACE_HEAD, PartType.HANDLE), 2, 0.0F),
	SPEAR("spear", 0.0F, -3.35F, null,
			List.of(PartType.SPEAR_HEAD, PartType.BINDING, PartType.HANDLE), 1, 0.0F);

	/** @param baseDamage damage modifier offset (total displayed = 1.0 player base + modifier) */
	ModularToolType(String id, float baseDamage, float baseSpeed, TagKey<Block> mineableTag,
			List<PartType> parts, int weaponDamagePerAttack, float disableBlockingForSeconds) {
		this.id = id;
		this.baseDamage = baseDamage;
		this.baseSpeed = baseSpeed;
		this.mineableTag = mineableTag;
		this.parts = parts;
		this.weaponDamagePerAttack = weaponDamagePerAttack;
		this.disableBlockingForSeconds = disableBlockingForSeconds;
	}

	public final String id;
	public final float baseDamage;
	public final float baseSpeed;
	public final TagKey<Block> mineableTag;
	public final List<PartType> parts;
	public final int weaponDamagePerAttack;
	public final float disableBlockingForSeconds;

	public String translationKey() {
		return "equipment.modular_dreams." + id;
	}

	public PartType headPart() {
		return parts.stream().filter(PartType::isHead).findFirst().orElseThrow();
	}

	public Optional<PartType> bindingPart() {
		return parts.stream().filter(p -> p == PartType.BINDING || p == PartType.SWORD_GUARD).findFirst();
	}

	public boolean isRangedChargeWeapon() {
		return this == SPEAR;
	}

	/** @return the part type consumed by the given stack, or empty if it is not a part of this tool. */
	public static Optional<ModularToolType> byId(String id) {
		for (ModularToolType type : values()) {
			if (type.id.equals(id)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}
}
