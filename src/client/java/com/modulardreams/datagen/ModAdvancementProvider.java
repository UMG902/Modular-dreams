package com.modulardreams.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;

import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;

import com.modulardreams.ModularDreams;
import com.modulardreams.equipment.ModItems;

/**
 * Three simple advancements: the mod root, first assembled tool, three modifiers.
 */
public class ModAdvancementProvider extends FabricAdvancementProvider {

        protected ModAdvancementProvider(FabricPackOutput output,
                        CompletableFuture<HolderLookup.Provider> registryLookup) {
                super(output, registryLookup);
        }

        @Override
        public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> consumer) {
                AdvancementHolder root = Advancement.Builder.advancement()
                                .rootDisplay(
                                                Items.WRITABLE_BOOK,
                                                Component.translatable("advancements.modular_dreams.root.title"),
                                                Component.translatable("advancements.modular_dreams.root.description"),
                                                Identifier.withDefaultNamespace("textures/gui/advancements/backgrounds/adventure.png"),
                                                AdvancementType.TASK,
                                                true,
                                                true,
                                                false)
                                .addCriterion("has_guidebook", InventoryChangeTrigger.TriggerInstance.hasItems(
                                                ModItems.byName("modular_guidebook").orElseThrow()))
                                .save(consumer, ModularDreams.id("root"));

                Advancement.Builder.advancement()
                                .parent(root)
                                .display(
                                                ModItems.byName("modular_pickaxe").orElseThrow(),
                                                Component.translatable("advancements.modular_dreams.first_tool.title"),
                                                Component.translatable("advancements.modular_dreams.first_tool.description"),
                                                AdvancementType.TASK,
                                                true,
                                                true,
                                                false)
                                .addCriterion("has_tool", InventoryChangeTrigger.TriggerInstance.hasItems(
                                                ModItems.byName("modular_pickaxe").orElseThrow(),
                                                ModItems.byName("modular_axe").orElseThrow(),
                                                ModItems.byName("modular_shovel").orElseThrow(),
                                                ModItems.byName("modular_hoe").orElseThrow(),
                                                ModItems.byName("modular_sword").orElseThrow(),
                                                ModItems.byName("modular_spear").orElseThrow()))
                                .save(consumer, ModularDreams.id("first_tool"));

                Advancement.Builder.advancement()
                                .parent(root)
                                .display(
                                                net.minecraft.world.item.Items.REDSTONE,
                                                Component.translatable("advancements.modular_dreams.modded.title"),
                                                Component.translatable("advancements.modular_dreams.modded.description"),
                                                AdvancementType.GOAL,
                                                true,
                                                true,
                                                false)
                                .addCriterion("has_modifier_item", InventoryChangeTrigger.TriggerInstance.hasItems(
                                                ModItems.byName("modular_pickaxe").orElseThrow()))
                                .save(consumer, ModularDreams.id("modded"));
        }
}
