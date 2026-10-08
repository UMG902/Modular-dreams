package com.modulardreams.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.component.ModDataComponents.PartData;
import com.modulardreams.equipment.ModArmorItems;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.equipment.ModPlatingItems;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;
import com.modulardreams.part.PlatingType;
import com.modulardreams.stats.StatsEngine;

/**
 * Matching logic of the v2 stations:
 *
 * - Part Picker    : converts material items into parts (cost in units);
 *                    a tier-N station shapes every material up to one tier
 *                    above it, so a diamond picker still makes wood parts.
 *                    Options are tool parts (handle + one head per tool
 *                    type) plus the 4 armor platings where the material
 *                    has them
 * - Assembler      : assembles handle + head into a tool, or converts a
 *                    vanilla tool into its modular equivalent (head of the
 *                    tool's material, wooden handle; netherite tools convert
 *                    with a netherite handle)
 */
public final class StationLogic {

    private StationLogic() {}

    // ------------------------------------------------------------------ material items

    /** @return the material this stack can be shaped into, if any. */
    public static ModularMaterial materialOf(ItemStack stack) {
        // Deliberately NOT cached by Ingredient: tag-backed ingredients captured
        // once would go stale across world loads and /reload.
        for (ModularMaterial mat : ModMaterials.materials()) {
            if (mat.crafting().test(stack)) {
                return mat;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ part picker

    /**
     * @return whether a station of the given tier can shape this material.
     *         Cumulative: a station shapes its own tier, every tier BELOW it
     *         (a diamond picker still makes wood parts) and one tier ABOVE
     *         (the wood picker already makes stone, the diamond picker
     *         netherite).
     */
    public static boolean canShape(int stationTier, ModularMaterial material) {
        return material.stationTier() <= stationTier + 1;
    }

    /** Builds one part of the given material (Part Picker result). */
    public static ItemStack makePart(PartType part, ModularMaterial material) {
        return new ItemStack(ModPartItems.get(part, material), 1);
    }

    /** Builds one plating of the given material (Part Picker result). */
    public static ItemStack makePlating(PlatingType plating, ModularMaterial material) {
        return new ItemStack(ModPlatingItems.get(plating, material), 1);
    }

    // ------------------------------------------------------------------ part picker options

    /**
     * One selectable Part Picker option: its material cost and a prototype
     * of the output stack.
     */
    public record Option(int cost, ItemStack prototype) {}

    /**
     * @return every option a station of the given tier offers for this
     *         material, in display order: handle, tool heads, armor
     *         platings. Only options that EXIST for the material are
     *         offered - handle-only rods get the handle alone (heads have
     *         no rod items), and netherite gets no platings (netherite
     *         armor is upgrade-only).
     */
    public static List<Option> optionsFor(ModularMaterial material, int stationTier) {
        if (material == null || !canShape(stationTier, material)) {
            return List.of();
        }
        List<Option> options = new ArrayList<>();
        options.add(new Option(PartType.HANDLE.cost, makePart(PartType.HANDLE, material)));
        if (!material.handleOnly()) {
            for (PartType part : PartType.values()) {
                if (part.isHead()) {
                    options.add(new Option(part.cost, makePart(part, material)));
                }
            }
        }
        if (ModPlatingItems.isPlatingMaterial(material)) {
            for (PlatingType plating : PlatingType.values()) {
                options.add(new Option(plating.cost, makePlating(plating, material)));
            }
        }
        return List.copyOf(options);
    }

    // ------------------------------------------------------------------ assembly (Assembler)

    /**
     * Assembles the modular tool for a handle + head pair. The tool type
     * comes from the head.
     */
    public static ItemStack assemble(ModularToolType type, String handleMaterial, String headMaterial) {
        List<PartData> parts = List.of(
                new PartData(PartType.HANDLE.id, handleMaterial),
                new PartData(type.headPart().id, headMaterial));
        ModularData data = new ModularData(type.id, parts, java.util.List.of(), 0);
        ItemStack result = new ItemStack(ModItems.toolItem(type));
        result.set(ModDataComponents.MODULAR_DATA, data);
        StatsEngine.bake(result, data);
        return result;
    }

    // ------------------------------------------------------------------ armor assembly

    /**
     * Assembles the modular armor piece for a plating + lining pair. The
     * plating decides the slot and every protection stat, the lining carries
     * its trait. Parts are stored in assembly order: lining (inner) first,
     * plating (outer) second - the plating's part id is the plating type id.
     */
    public static ItemStack assembleArmor(PlatingType plating, String platingMaterial, String liningMaterial) {
        List<PartData> parts = List.of(
                new PartData(StatsEngine.LINING_PART_ID, liningMaterial),
                new PartData(plating.id, platingMaterial));
        ModularData data = new ModularData(plating.armorTypeId(), parts, java.util.List.of(), 0);
        ItemStack result = new ItemStack(ModArmorItems.piece(plating));
        result.set(ModDataComponents.MODULAR_DATA, data);
        StatsEngine.bake(result, data);
        return result;
    }

    // ------------------------------------------------------------------ vanilla tool conversion

    /**
     * @return the conversion target of a vanilla tool: its tool type and
     *         material, or empty for non-convertible items.
     */
    public static Optional<Conversion> conversionOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return ConversionTable.lookup(stack.getItem());
    }

    public record Conversion(ModularToolType type, String headMaterial, String handleMaterial) {}

    /** Assembles the modular equivalent of a converted vanilla tool. */
    public static ItemStack convert(Conversion conversion) {
        return assemble(conversion.type(), conversion.handleMaterial(), conversion.headMaterial());
    }

    /**
     * Converts a concrete vanilla tool: like {@link #convert(Conversion)} but the result
     * keeps the source's enchantments and custom name and its relative wear, so converting
     * neither strips enchantments nor repairs the tool for free.
     */
    public static ItemStack convert(Conversion conversion, ItemStack source) {
        ItemStack result = convert(conversion);
        if (result.isEmpty() || source.isEmpty()) {
            return result;
        }
        var enchantments = source.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        if (!enchantments.isEmpty()) {
            result.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, enchantments);
        }
        var name = source.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (name != null) {
            result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
        }
        if (source.isDamageableItem() && source.getMaxDamage() > 0 && result.getMaxDamage() > 0) {
            float wear = Math.min(1.0F, source.getDamageValue() / (float) source.getMaxDamage());
            result.setDamageValue(Math.min(result.getMaxDamage() - 1, Math.round(wear * result.getMaxDamage())));
        }
        return result;
    }

    /** The vanilla-tool -> modular conversion table (netherite keeps a netherite handle). */
    private static final class ConversionTable {
        private static final Map<net.minecraft.world.item.Item, Conversion> MAP = build();

        private static Map<net.minecraft.world.item.Item, Conversion> build() {
            Map<net.minecraft.world.item.Item, Conversion> map = new HashMap<>();
            register(map, "wood", Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL,
                    Items.WOODEN_HOE, Items.WOODEN_SWORD);
            register(map, "stone", Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL,
                    Items.STONE_HOE, Items.STONE_SWORD);
            register(map, "copper", Items.COPPER_PICKAXE, Items.COPPER_AXE, Items.COPPER_SHOVEL,
                    Items.COPPER_HOE, Items.COPPER_SWORD);
            register(map, "iron", Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL,
                    Items.IRON_HOE, Items.IRON_SWORD);
            register(map, "gold", Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL,
                    Items.GOLDEN_HOE, Items.GOLDEN_SWORD);
            register(map, "diamond", Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL,
                    Items.DIAMOND_HOE, Items.DIAMOND_SWORD);
            // netherite tools convert with a NETHERITE handle carrying the wood trait
            // (the trait itself arrives with the traits milestone)
            register(map, "netherite", Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE,
                    Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_SWORD);
            // vanilla spears (26.3)
            map.put(Items.WOODEN_SPEAR, new Conversion(ModularToolType.SPEAR, "wood", "wood"));
            map.put(Items.COPPER_SPEAR, new Conversion(ModularToolType.SPEAR, "copper", "copper"));
            return Map.copyOf(map);
        }

        private static void register(Map<net.minecraft.world.item.Item, Conversion> map, String material,
                net.minecraft.world.item.Item... items) {
            ModularToolType[] types = { ModularToolType.PICKAXE, ModularToolType.AXE, ModularToolType.SHOVEL,
                    ModularToolType.HOE, ModularToolType.SWORD };
            String handle = material.equals("netherite") ? "netherite" : "wood";
            for (int i = 0; i < items.length; i++) {
                map.put(items[i], new Conversion(types[i], material, handle));
            }
        }

        static Optional<Conversion> lookup(net.minecraft.world.item.Item item) {
            return Optional.ofNullable(MAP.get(item));
        }
    }
}
