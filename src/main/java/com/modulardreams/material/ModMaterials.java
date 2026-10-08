package com.modulardreams.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

import com.modulardreams.ModularDreams;

/**
 * All materials of Modular Dreams v2.
 *
 * <p>Head stats: Tinkers' Construct 3.12 values where TiC defines them
 * (wood 60/2.0/0.0, copper 210/5.0/0.5, iron 250/6.0/2.0); stone keeps the
 * TiC 1.12 / Legacy's Construct values (120/4.0/1.0); gold uses vanilla
 * gold-tool stats (32/12.0/0.0, enchantability 22). Rose gold is the new
 * iron/gold-station alloy. Netherite is a FULL material here (shaped at the
 * diamond station). Blaze and breeze rods are handle-only extras.
 *
 * <p>Station tiers: 1 wood, 2 stone, 3 copper, 4 iron/gold/rose gold,
 * 5 diamond. A tier-N station shapes tier N and tier N+1 materials, so the
 * wood picker makes wood + stone parts and the diamond picker makes
 * diamond + netherite parts.
 */
public class ModMaterials {

    private static final Map<String, ModularMaterial> BY_ID = new HashMap<>();
    private static final List<ModularMaterial> MATERIALS = new ArrayList<>();
    private static final List<ModularMaterial> HEAD_MATERIALS = new ArrayList<>();
    private static final List<ModularMaterial> HANDLE_MATERIALS = new ArrayList<>();

    /** Repair items for rose gold tools (no vanilla tag exists). */
    public static final TagKey<Item> REPAIRS_ROSE_GOLD_TOOLS = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, ModularDreams.id("repairs_rose_gold_tools"));

    /**
     * Ingredient for a tag, resolving it whenever tags are bound (in game)
     * and falling back to an unresolved named set during early bootstrap.
     */
    public static Ingredient ingredientOfTag(TagKey<Item> tag) {
        try {
            return Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(tag));
        } catch (IllegalStateException e) {
            return Ingredient.of(HolderSet.emptyNamed(BuiltInRegistries.ITEM, tag));
        }
    }

    private static void full(String id, int color, Supplier<Ingredient> crafting, TagKey<Item> repair,
            MaterialTier tier, int stationTier, int dur, float speed, float atk, int ench,
            float handleMult, float handleBoost) {
        register(new ModularMaterial(id, color, crafting, repair, tier, stationTier, dur, speed, atk,
                ench, handleMult, handleBoost, id.equals("netherite"), false));
    }

    private static void handleOnly(String id, int color, Supplier<Ingredient> crafting,
            float handleMult, float handleBoost) {
        register(new ModularMaterial(id, color, crafting, null, MaterialTier.WOOD, 1, 0, 0.0F, 0.0F,
                0, handleMult, handleBoost, false, true));
    }

    private static void register(ModularMaterial mat) {
        BY_ID.put(mat.id(), mat);
        MATERIALS.add(mat);
        if (!mat.handleOnly()) {
            HEAD_MATERIALS.add(mat);
        }
        HANDLE_MATERIALS.add(mat);
    }

    public static void initialize() {
        // --- full materials (head + handle) ---
        full("wood", 0x7A5A36, () -> ingredientOfTag(ItemTags.PLANKS),
                ItemTags.PLANKS, MaterialTier.WOOD, 1, 60, 2.0F, 0.0F, 15, 1.0F, 1.0F);
        full("stone", 0x7F7F7F, () -> ingredientOfTag(ModTags.STONE_MATERIALS),
                ModTags.STONE_MATERIALS, MaterialTier.STONE, 2, 120, 4.0F, 1.0F, 5, 0.9F, 1.0F);
        full("copper", 0xE77C56, () -> Ingredient.of(Items.COPPER_INGOT), ItemTags.COPPER_TOOL_MATERIALS,
                MaterialTier.COPPER, 3, 210, 5.0F, 0.5F, 13, 0.9F, 1.0F);
        full("iron", 0xD8D8D8, () -> Ingredient.of(Items.IRON_INGOT), ItemTags.IRON_TOOL_MATERIALS,
                MaterialTier.IRON, 4, 250, 6.0F, 2.0F, 14, 1.1F, 1.0F);
        full("gold", 0xF9DE4B, () -> Ingredient.of(Items.GOLD_INGOT), ItemTags.GOLD_TOOL_MATERIALS,
                MaterialTier.GOLD, 4, 32, 12.0F, 0.0F, 22, 1.0F, 1.0F);
        full("rose_gold", 0xE8A08F, () -> Ingredient.of(com.modulardreams.equipment.ModItems.ROSE_GOLD_INGOT),
                REPAIRS_ROSE_GOLD_TOOLS, MaterialTier.IRON, 4, 180, 7.0F, 1.5F, 18, 1.1F, 1.05F);
        full("diamond", 0x4AEDD9, () -> Ingredient.of(Items.DIAMOND), ItemTags.DIAMOND_TOOL_MATERIALS,
                MaterialTier.DIAMOND, 5, 1500, 8.0F, 3.0F, 10, 1.3F, 1.1F);
        full("netherite", 0x4C4148, () -> Ingredient.of(Items.NETHERITE_INGOT), ItemTags.NETHERITE_TOOL_MATERIALS,
                MaterialTier.NETHERITE, 6, 2031, 9.0F, 4.0F, 15, 1.4F, 1.1F);

        // --- handle-only rods ---
        handleOnly("blaze_rod", 0xFDB02F, () -> Ingredient.of(Items.BLAZE_ROD), 1.1F, 1.0F);
        handleOnly("breeze_rod", 0x8FB6C8, () -> Ingredient.of(Items.BREEZE_ROD), 1.1F, 1.0F);
    }

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

    public static List<ModularMaterial> materials() {
        return List.copyOf(MATERIALS);
    }

    public static List<ModularMaterial> headMaterials() {
        return List.copyOf(HEAD_MATERIALS);
    }

    public static List<ModularMaterial> handleMaterials() {
        return List.copyOf(HANDLE_MATERIALS);
    }

    /** Forward declaration of mod tags (kept here to avoid a tag class dependency cycle). */
    public static final class ModTags {
        /** Every stone variant that counts as cobblestone in crafting. */
        public static final TagKey<Item> STONE_MATERIALS = TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, ModularDreams.id("stone_materials"));
    }
}
