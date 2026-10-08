package com.modulardreams.equipment;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.component.ModDataComponents.PartData;
import com.modulardreams.part.PartType;
import com.modulardreams.registry.ModRegistry;
import com.modulardreams.stats.StatsEngine;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Registers the modular tools and the rose gold ingot, and
 * provides factory helpers to build assembled stacks.
 */
public class ModItems {

    public static Item ROSE_GOLD_INGOT;

    private static final java.util.Map<String, Item> BY_NAME = new java.util.LinkedHashMap<>();

    public static void initialize() {
        ROSE_GOLD_INGOT = ModRegistry.registerItem("rose_gold_ingot", Item::new);
        registerTool("modular_pickaxe", ModularToolType.PICKAXE);
        registerTool("modular_axe", ModularToolType.AXE);
        registerTool("modular_shovel", ModularToolType.SHOVEL);
        registerTool("modular_hoe", ModularToolType.HOE);
        registerTool("modular_sword", ModularToolType.SWORD);
        registerTool("modular_spear", ModularToolType.SPEAR);
    }

    private static void registerTool(String name, ModularToolType type) {
        Item item = ModRegistry.registerItem(name, ModularToolItem::new);
        BY_NAME.put(type.id, item);
    }

    public static Item toolItem(ModularToolType type) {
        Item item = BY_NAME.get(type.id);
        if (item == null) {
            throw new IllegalStateException("Modular tool not registered: " + type.id);
        }
        return item;
    }

    // ------------------------------------------------------------------ factory helpers

    /**
     * Builds an assembled, fully-baked tool stack (creative tab, conversion).
     * Parts are given in assembly order: handle, head.
     */
    public static ItemStack toolStack(ModularToolType type, String headMaterial, String handleMaterial) {
        List<PartData> parts = List.of(
                new PartData(PartType.HANDLE.id, handleMaterial),
                new PartData(type.headPart().id, headMaterial));
        ItemStack stack = new ItemStack(toolItem(type));
        ModularData data = new ModularData(type.id, parts, List.of(), 0);
        stack.set(ModDataComponents.MODULAR_DATA, data);
        StatsEngine.bake(stack, data);
        return stack;
    }
}
