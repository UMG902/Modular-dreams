package com.modulardreams.material;

import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import com.modulardreams.ModularDreams;

/**
 * Armor material stats of Modular Dreams v2 - one 26.3
 * {@link ArmorMaterial} record per plating material (plus netherite for the
 * smithing-only upgrade), built with plain vanilla record instances so the
 * baked attributes, durability and equipment assets match the vanilla
 * armor pipeline exactly ({@code ArmorMaterial.createAttributes}).
 *
 * <p>The ladder follows vanilla 26.3 reference materials (durability
 * multiplier, defense helmet/chestplate/leggings/boots, toughness,
 * knockback resistance):
 * <ul>
 *   <li>wood = leather (5, 1/3/2/1)</li>
 *   <li>stone = one step above leather, below copper (8, 1/4/3/1)</li>
 *   <li>copper = vanilla copper (11, 1/4/3/2)</li>
 *   <li>iron = vanilla iron (15, 2/6/5/2)</li>
 *   <li>gold = vanilla gold (7, 1/5/3/2, enchantability from gold's high roll)</li>
 *   <li>rose gold = between copper and iron (13, 2/5/4/2), iron's sound</li>
 *   <li>diamond = vanilla diamond (33, 3/8/6/3, toughness 2)</li>
 *   <li>netherite = vanilla netherite (37, 3/8/6/3, toughness 3, kb 0.1) -
 *       upgrade-only, never a plating item</li>
 * </ul>
 *
 * <p>Enchantability reuses the material's own v2 value (the tool
 * enchantability from {@link ModMaterials}) so one material keeps one
 * enchantability everywhere. Repair items are the material's tool repair
 * tags as well.
 */
public final class ModArmorMaterials {

    private static final Map<String, ArmorMaterial> BY_MATERIAL = Map.ofEntries(
            entry("wood", 5, 1, 3, 2, 1, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
                    ItemTags.PLANKS),
            entry("stone", 8, 1, 4, 3, 1, 5, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, 0.0F,
                    ModMaterials.ModTags.STONE_MATERIALS),
            entry("copper", 11, 1, 4, 3, 2, 13, SoundEvents.ARMOR_EQUIP_COPPER, 0.0F, 0.0F,
                    ItemTags.COPPER_TOOL_MATERIALS),
            entry("iron", 15, 2, 6, 5, 2, 14, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F,
                    ItemTags.IRON_TOOL_MATERIALS),
            entry("gold", 7, 1, 5, 3, 2, 22, SoundEvents.ARMOR_EQUIP_GOLD, 0.0F, 0.0F,
                    ItemTags.GOLD_TOOL_MATERIALS),
            entry("rose_gold", 13, 2, 5, 4, 2, 18, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F,
                    ModMaterials.REPAIRS_ROSE_GOLD_TOOLS),
            entry("diamond", 33, 3, 8, 6, 3, 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.0F,
                    ItemTags.DIAMOND_TOOL_MATERIALS),
            // upgrade-only: no netherite plating item exists
            entry("netherite", 37, 3, 8, 6, 3, 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F,
                    ItemTags.NETHERITE_TOOL_MATERIALS));

    private ModArmorMaterials() {}

    private static Map.Entry<String, ArmorMaterial> entry(String material, int durability,
            int helmet, int chest, int legs, int boots, int enchantability,
            Holder<SoundEvent> equipSound, float toughness, float knockbackResistance,
            TagKey<Item> repairTag) {
        ArmorMaterial mat = new ArmorMaterial(durability,
                Map.of(ArmorType.HELMET, helmet,
                        ArmorType.CHESTPLATE, chest,
                        ArmorType.LEGGINGS, legs,
                        ArmorType.BOOTS, boots),
                enchantability, equipSound, toughness, knockbackResistance, repairTag,
                ResourceKey.create(EquipmentAssets.ROOT_ID, ModularDreams.id(material)));
        return Map.entry(material, mat);
    }

    /** The armor material of a plating material id (throws when unknown). */
    public static ArmorMaterial getOrThrow(String materialId) {
        ArmorMaterial mat = BY_MATERIAL.get(materialId);
        if (mat == null) {
            throw new IllegalStateException("No armor material for plating material: " + materialId);
        }
        return mat;
    }

    /** Whether an armor material exists for the given material id. */
    public static boolean has(String materialId) {
        return BY_MATERIAL.containsKey(materialId);
    }

    /** Every armor material, keyed by material id. */
    public static Map<String, ArmorMaterial> all() {
        return Map.copyOf(BY_MATERIAL);
    }
}
