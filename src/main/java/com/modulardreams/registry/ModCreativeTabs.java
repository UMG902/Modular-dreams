package com.modulardreams.registry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.core.registries.Registries;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.modulardreams.ModularDreams;
import com.modulardreams.block.ModBlocks;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;

/**
 * The "Modular Dreams" creative tab: guide book, stations, molds and example
 * equipment, followed by every generic part of every material.
 */
public class ModCreativeTabs {

        public static final ResourceKey<CreativeModeTab> MODULAR_TAB_KEY = ResourceKey
                        .create(Registries.CREATIVE_MODE_TAB, ModularDreams.id("modular_tab"));

        public static void initialize() {
                CreativeModeTab tab = FabricCreativeModeTab.builder()
                                .icon(() -> ModItems.byName("modular_guidebook").map(ItemStack::new)
                                                .orElse(ItemStack.EMPTY))
                                .title(Component.translatable("itemGroup.modular_dreams"))
                                .displayItems((parameters, output) -> {
                                        // guide first
                                        ModItems.byName("modular_guidebook").ifPresent(output::accept);

                                        // stations & molds
                                        output.accept(ModBlocks.PART_BUILDER);
                                        output.accept(ModBlocks.ASSEMBLY_TABLE);
                                        output.accept(ModBlocks.CLAY_MOLD);
                                        output.accept(ModBlocks.TERRACOTTA_MOLD);
                                        output.accept(ModBlocks.MELTING_UPGRADE);

                                        // example assembled equipment (iron + wood + iron)
                                        for (ModularToolType type : ModularToolType.values()) {
                                                output.accept(ModItems.toolStack(type, "iron", "wood", "iron"));
                                        }

                                        // every part, grouped by part shape, materials in registry order
                                        for (PartType part : PartType.values()) {
                                                List<Item> items = new ArrayList<>();
                                                for (ModularMaterial material : part.allowedMaterials()) {
                                                        items.add(ModPartItems.get(part, material));
                                                }
                                                for (Item item : items) {
                                                        output.accept(item);
                                                }
                                        }
                                })
                                .build();
                Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MODULAR_TAB_KEY, tab);
        }
}
