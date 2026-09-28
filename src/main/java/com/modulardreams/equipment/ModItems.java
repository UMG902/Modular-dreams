package com.modulardreams.equipment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.component.ModDataComponents.PartData;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.StatsEngine;

/**
 * Registers the 6 modular tools, 4 armor pieces and the guide book, and
 * provides factory helpers to build assembled example items.
 */
public class ModItems {

        private static final Map<String, Item> BY_NAME = new LinkedHashMap<>();

        public static final Map<String, Item> registry() {
                return BY_NAME;
        }

        public static Optional<Item> byName(String name) {
                return Optional.ofNullable(BY_NAME.get(name));
        }

        public static void initialize() {
                ModPartItems.registerAll();
                registerItem("modular_pickaxe", ModularToolItem::new);
                registerItem("modular_axe", ModularToolItem::new);
                registerItem("modular_shovel", ModularToolItem::new);
                registerItem("modular_hoe", ModularToolItem::new);
                registerItem("modular_sword", ModularToolItem::new);
                registerItem("modular_spear", ModularToolItem::new);
                registerItem("modular_guidebook", GuideBookItem::new);
                // NOTE: clay and terracotta molds are the BlockItems of the two
                // mold blocks (registered in ModBlocks) since both are placeable.
        }

        private static void registerItem(String name, java.util.function.Function<Item.Properties, Item> factory) {
                Item item = com.modulardreams.registry.ModRegistry.registerItem(name, factory);
                BY_NAME.put(name, item);
        }

        // ------------------------------------------------------------------ factory helpers

        /**
         * Builds an assembled, fully-baked tool stack (used for the creative tab).
         * Part materials are given in assembly order: handle, binding, head.
         */
        public static ItemStack toolStack(ModularToolType type, String headMaterial, String handleMaterial,
                        String bindingMaterial) {
                List<PartData> parts = new java.util.ArrayList<>();
                parts.add(new PartData(type.handlePart().id, handleMaterial));
                if (bindingMaterial != null) {
                        parts.add(new PartData(type.bindingPart().id, bindingMaterial));
                }
                parts.add(new PartData(type.headPart().id, headMaterial));
                ItemStack stack = new ItemStack(byNameOrThrow("modular_" + type.id));
                ModularData data = new ModularData(type.id, List.copyOf(parts), List.of());
                stack.set(ModDataComponents.MODULAR_DATA, data);
                StatsEngine.bake(stack, data);
                return stack;
        }

        /** A fresh, empty modular tool that is not yet assembled (used as recipe output fallback). */
        public static ItemStack unassembled(String name) {
                return new ItemStack(byNameOrThrow(name));
        }

        private static Item byNameOrThrow(String name) {
                Item item = BY_NAME.get(name);
                if (item == null) {
                        throw new IllegalStateException("Modular item not registered: " + name);
                }
                return item;
        }
}
