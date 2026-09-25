package com.modulardreams.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAssets;

import com.modulardreams.ModularDreams;
import com.modulardreams.stats.ModTraits;

/**
 * All vanilla materials supported by Modular Dreams, with Tinkers'-style stats.
 *
 * Tool material balance follows vanilla ToolMaterials where a direct vanilla
 * equivalent exists; other materials are balanced around vanilla progression.
 * Armor plate stats mirror the vanilla armor of the same material.
 */
public class ModMaterials {

	private static final Map<String, ModularMaterial> BY_ID = new HashMap<>();
	private static final List<ModularMaterial> TOOL_MATERIALS = new ArrayList<>();
	private static final List<ModularMaterial> PLATE_MATERIALS = new ArrayList<>();
	private static final List<ModularMaterial> LINING_MATERIALS = new ArrayList<>();

	// ------------------------------------------------------------------
	// Tool materials (also usable for armor plates where noted)
	// ------------------------------------------------------------------

	private static void tool(String id, TagKey<Item> repair, MaterialTier tier, int dur, float speed, float dmg,
			int ench, float handleDur, float handleSpeed, float bindDur, ModTraits... traits) {
		ModularMaterial mat = new ModularMaterial(id, repair, tier, dur, speed, dmg, ench,
				handleDur, handleSpeed, bindDur, List.of(traits), null, null, hasTrait(traits, ModTraits.FIREPROOF));
		register(mat, true, false, false);
	}

	private static boolean hasTrait(ModTraits[] traits, ModTraits t) {
		for (ModTraits tr : traits) {
			if (tr == t) {
				return true;
			}
		}
		return false;
	}

	// Woods — cheap, fast early-game materials (vanilla: 59 durability, speed 2)
	private static void wood(String id, int dur, float speed, int ench, float handleDur, float handleSpeed,
			float bindDur, ModTraits... traits) {
		tool(id, ItemTags.PLANKS, MaterialTier.WOOD, dur, speed, 0.0F, ench, handleDur, handleSpeed, bindDur, traits);
	}

	public static void initialize() {
		// --- woods (all traits: ROOTED — repairable with sticks too) ---
		wood("oak", 59, 2.0F, 15, 0.25F, 0.0F, 0.10F, ModTraits.ROOTED);
		wood("spruce", 65, 2.0F, 14, 0.30F, 0.0F, 0.10F, ModTraits.ROOTED);
		wood("birch", 52, 2.5F, 16, 0.20F, 0.05F, 0.10F, ModTraits.ROOTED);
		wood("cherry", 52, 2.0F, 18, 0.20F, 0.0F, 0.15F, ModTraits.ROOTED);
		wood("bamboo", 42, 3.0F, 12, 0.10F, 0.10F, 0.05F, ModTraits.ROOTED);
		wood("crimson", 72, 2.5F, 12, 0.25F, 0.0F, 0.10F, ModTraits.ROOTED);
		wood("warped", 72, 2.5F, 12, 0.25F, 0.0F, 0.10F, ModTraits.ROOTED);

		// --- bone: light, springy, crude (vanilla bone meal/bone theme) ---
		tool("bone", ItemTags.STONE_TOOL_MATERIALS, MaterialTier.STONE, 105, 4.0F, 1.0F, 8,
				-0.05F, 0.15F, 0.10F, ModTraits.FEATHERWEIGHT);

		// --- flint: crude cutting edges (Tinkers' Crude) ---
		tool("flint", ItemTags.STONE_TOOL_MATERIALS, MaterialTier.STONE, 90, 4.5F, 1.5F, 8,
				0.0F, 0.0F, 0.0F, ModTraits.SPIKY);

		// --- stones ---
		tool("stone", ItemTags.STONE_TOOL_MATERIALS, MaterialTier.STONE, 131, 4.0F, 1.0F, 5, 0.0F, 0.0F, 0.05F);
		tool("deepslate", ItemTags.STONE_TOOL_MATERIALS, MaterialTier.STONE, 155, 4.0F, 1.0F, 5, 0.0F, 0.0F, 0.10F);
		tool("blackstone", ItemTags.STONE_TOOL_MATERIALS, MaterialTier.STONE, 140, 4.5F, 1.0F, 8, 0.0F, 0.05F, 0.10F);

		// --- obsidian: extremely hard, diamond-tier (Tinkers made obsidian high-tier) ---
		tool("obsidian", ItemTags.DIAMOND_TOOL_MATERIALS, MaterialTier.DIAMOND, 380, 5.5F, 2.0F, 10,
				0.10F, -0.05F, 0.20F, ModTraits.DENSE);

		// --- amethyst: resonant crystals (vanilla: growing, musical) ---
		tool("amethyst", ItemTags.IRON_TOOL_MATERIALS, MaterialTier.IRON, 210, 5.0F, 1.5F, 14,
				0.0F, 0.10F, 0.15F, ModTraits.RESONANT);

		// --- metals (vanilla ToolMaterial parity) ---
		tool("copper", ItemTags.COPPER_TOOL_MATERIALS, MaterialTier.COPPER, 190, 5.0F, 1.0F, 13, 0.10F, 0.0F, 0.10F);
		tool("iron", ItemTags.IRON_TOOL_MATERIALS, MaterialTier.IRON, 250, 6.0F, 2.0F, 14, 0.15F, 0.0F, 0.15F,
				ModTraits.STURDY);
		tool("gold", ItemTags.GOLD_TOOL_MATERIALS, MaterialTier.GOLD, 32, 12.0F, 0.0F, 22, 0.0F, 0.10F, 0.0F,
				ModTraits.GILDED);
		tool("diamond", ItemTags.DIAMOND_TOOL_MATERIALS, MaterialTier.DIAMOND, 1561, 8.0F, 3.0F, 10, 0.30F, 0.0F,
				0.25F, ModTraits.PRECIOUS);
		tool("netherite", ItemTags.NETHERITE_TOOL_MATERIALS, MaterialTier.NETHERITE, 2031, 9.0F, 4.0F, 15,
				0.40F, -0.05F, 0.35F, ModTraits.FIERY, ModTraits.FIREPROOF);

		// ------------------------------------------------------------------
		// Armor plate materials (vanilla armor parity)
		// ------------------------------------------------------------------
		plate("copper", ItemTags.REPAIRS_COPPER_ARMOR, 11, mapDef(1, 3, 4, 2), 0.0F, 0.0F, 8,
				EquipmentAssets.COPPER, SoundEvents.ARMOR_EQUIP_COPPER);
		plate("iron", ItemTags.REPAIRS_IRON_ARMOR, 15, mapDef(2, 5, 6, 2), 0.0F, 0.0F, 9,
				EquipmentAssets.IRON, SoundEvents.ARMOR_EQUIP_IRON);
		plate("gold", ItemTags.REPAIRS_GOLD_ARMOR, 7, mapDef(1, 3, 5, 2), 0.0F, 0.0F, 25,
				EquipmentAssets.GOLD, SoundEvents.ARMOR_EQUIP_GOLD);
		plate("diamond", ItemTags.REPAIRS_DIAMOND_ARMOR, 33, mapDef(3, 6, 8, 3), 2.0F, 0.0F, 10,
				EquipmentAssets.DIAMOND, SoundEvents.ARMOR_EQUIP_DIAMOND);
		plate("netherite", ItemTags.REPAIRS_NETHERITE_ARMOR, 37, mapDef(3, 6, 8, 3), 3.0F, 0.1F, 15,
				EquipmentAssets.NETHERITE, SoundEvents.ARMOR_EQUIP_NETHERITE, ModTraits.FIREPROOF);
		plate("turtle_scute", ItemTags.REPAIRS_TURTLE_HELMET, 25, mapDef(0, 0, 0, 2), 0.0F, 0.0F, 9,
				EquipmentAssets.TURTLE_SCUTE, SoundEvents.ARMOR_EQUIP_TURTLE, ModTraits.AQUATIC);
		plate("armadillo_scute", ItemTags.REPAIRS_WOLF_ARMOR, 25, mapDef(2, 4, 5, 2), 0.0F, 0.05F, 9,
				EquipmentAssets.ARMADILLO_SCUTE, SoundEvents.ARMOR_EQUIP_WOLF, ModTraits.PLATED);

		// ------------------------------------------------------------------
		// Armor lining materials (soft padding & utility)
		// ------------------------------------------------------------------
		lining("leather", ItemTags.REPAIRS_LEATHER_ARMOR, 0.10F, 1,
				new ModularMaterial.AttrBonus(Attributes.ARMOR, 0.5, AttributeModifier.Operation.ADD_VALUE));
		lining("rabbit_hide", ItemTags.REPAIRS_LEATHER_ARMOR, 0.05F, 2,
				new ModularMaterial.AttrBonus(Attributes.JUMP_STRENGTH, 0.02, AttributeModifier.Operation.ADD_VALUE));
		lining("phantom_membrane", ItemTags.REPAIRS_LEATHER_ARMOR, 0.15F, 2,
				new ModularMaterial.AttrBonus(Attributes.FALL_DAMAGE_MULTIPLIER, -0.15,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		lining("wool", ItemTags.REPAIRS_LEATHER_ARMOR, 0.05F, 3,
				new ModularMaterial.AttrBonus(Attributes.SNEAKING_SPEED, 0.15,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		lining("slime", ItemTags.REPAIRS_LEATHER_ARMOR, 0.15F, 1,
				new ModularMaterial.AttrBonus(Attributes.SAFE_FALL_DISTANCE, 1.0,
						AttributeModifier.Operation.ADD_VALUE));
	}

	private static Map<ArmorType, Integer> mapDef(int boots, int legs, int chest, int head) {
		Map<ArmorType, Integer> map = new HashMap<>();
		map.put(ArmorType.BOOTS, boots);
		map.put(ArmorType.LEGGINGS, legs);
		map.put(ArmorType.CHESTPLATE, chest);
		map.put(ArmorType.HELMET, head);
		return map;
	}

	// ------------------------------------------------------------------

	private static void register(ModularMaterial mat, boolean tool, boolean plate, boolean lining) {
		BY_ID.put(mat.id(), mat);
		if (tool) {
			TOOL_MATERIALS.add(mat);
		}
		if (plate) {
			PLATE_MATERIALS.add(mat);
		}
		if (lining) {
			LINING_MATERIALS.add(mat);
		}
	}

	private static void plate(String id, TagKey<Item> repair, int durMult, Map<ArmorType, Integer> defense,
			float toughness, float kb, int ench,
			net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> asset,
			net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, ModTraits... traits) {
		ModularMaterial.Plate p = new ModularMaterial.Plate(durMult, defense, toughness, kb, ench, repair, asset,
				sound);
		ModularMaterial mat = new ModularMaterial(id, repair, MaterialTier.IRON, 250, 6.0F, 2.0F, ench,
				0, 0, 0, List.of(traits), p, null, hasTrait(traits, ModTraits.FIREPROOF));
		register(mat, false, true, false);
	}

	private static void lining(String id, TagKey<Item> repair, float durBonus, int enchBonus,
			ModularMaterial.AttrBonus attr) {
		ModularMaterial.Lining l = new ModularMaterial.Lining(durBonus, enchBonus, List.of(attr));
		ModularMaterial mat = new ModularMaterial(id, repair, MaterialTier.WOOD, 30, 1.0F, 0.0F, enchBonus,
				0, 0, 0, List.of(), null, l, false);
		register(mat, false, false, true);
	}

	// ------------------------------------------------------------------

	public static Optional<ModularMaterial> byId(String id) {
		return Optional.ofNullable(BY_ID.get(id));
	}

	public static ModularMaterial getOrThrow(String id) {
		ModularMaterial mat = BY_ID.get(id);
		if (mat == null) {
			throw new IllegalStateException("Unknown modular material: " + id);
		}
		return mat;
	}

	public static List<ModularMaterial> toolMaterials() {
		return List.copyOf(TOOL_MATERIALS);
	}

	public static List<ModularMaterial> plateMaterials() {
		return List.copyOf(PLATE_MATERIALS);
	}

	public static List<ModularMaterial> liningMaterials() {
		return List.copyOf(LINING_MATERIALS);
	}
}
