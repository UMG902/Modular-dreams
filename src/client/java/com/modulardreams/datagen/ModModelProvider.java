package com.modulardreams.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import com.modulardreams.ModularDreams;
import com.modulardreams.block.ModBlocks;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.part.PartType;

/**
 * Generates block models and the base item models of the overhaul build.
 *
 * - One FLAT_ITEM model per part shape (the shared grayscale texture).
 * - Tool fallback models (grayscale head silhouette) for unassembled stacks.
 * - Block models for the stations, the clay mold and the melting upgrade.
 * - Mold / guide book item models.
 */
public class ModModelProvider extends FabricModelProvider {

        protected ModModelProvider(FabricPackOutput output) {
                super(output);
        }

        @Override
        public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
                registerStation(blockStateModelGenerator, ModBlocks.PART_BUILDER, "part_builder");
                registerStation(blockStateModelGenerator, ModBlocks.ASSEMBLY_TABLE, "assembly_table");
                registerCubeBlock(blockStateModelGenerator, ModBlocks.MELTING_UPGRADE, "melting_upgrade");
                // NOTE: the clay and terracotta molds are NOT generated here -
                // they use hand-written flat cutting-board models (one pixel tall)
                // in src/main/resources, because vanilla templates cannot emit
                // custom element models.
        }

        /** cube_bottom_top blockstate + block item model for one station block. */
        private void registerStation(BlockModelGenerators generator, net.minecraft.world.level.block.Block block,
                        String name) {
                var mapping = new TextureMapping()
                                .put(net.minecraft.client.data.models.model.TextureSlot.TOP,
                                                blockTexture(name + "_top"))
                                .put(net.minecraft.client.data.models.model.TextureSlot.SIDE,
                                                blockTexture(name + "_side"))
                                .put(net.minecraft.client.data.models.model.TextureSlot.BOTTOM,
                                                blockTexture(name + "_bottom"));
                Identifier modelId = ModelTemplates.CUBE_BOTTOM_TOP.create(ModularDreams.id("block/" + name),
                                mapping, generator.modelOutput);
                generator.blockStateOutput.accept(
                                generator.createSimpleBlock(block, generator.plainVariant(modelId)));
                // Point the client item definition straight at the block model so the block
                // renders as a proper 3D block in the inventory (vanilla crafting-table style).
                generator.registerSimpleItemModel(block, modelId);
        }

        /** cube_all blockstate + block item model (mold, melting upgrade). */
        private void registerCubeBlock(BlockModelGenerators generator, net.minecraft.world.level.block.Block block,
                        String name) {
                Identifier modelId = ModelTemplates.CUBE_ALL.create(ModularDreams.id("block/" + name),
                                new TextureMapping().put(net.minecraft.client.data.models.model.TextureSlot.ALL,
                                                blockTexture(name)),
                                generator.modelOutput);
                generator.blockStateOutput.accept(
                                generator.createSimpleBlock(block, generator.plainVariant(modelId)));
                generator.registerSimpleItemModel(block, modelId);
        }

        @Override
        public void generateItemModels(ItemModelGenerators generators) {
                var modelOutput = generators.modelOutput;
                var itemModelOutput = generators.itemModelOutput;

                // ---- one grayscale model per part shape (shared by every material) ----
                for (PartType part : PartType.values()) {
                        ModelTemplates.FLAT_ITEM.create(
                                        ModularDreams.id(part.id),
                                        TextureMapping.layer0(texture(part.id)),
                                        modelOutput);
                }

                // ---- render-only part shapes ----
                // Assembled tools use the UNIVERSAL handle and UNIVERSAL
                // binding parts, but their item definitions render those
                // layers with these dedicated per-tool models (Tinkers'-style
                // shapes). No part items exist for them.
                for (String renderOnly : new String[] {
                                "sword_handle", "spear_handle",
                                "pickaxe_binding", "axe_binding", "shovel_binding", "hoe_binding" }) {
                        ModelTemplates.FLAT_ITEM.create(
                                        ModularDreams.id(renderOnly),
                                        TextureMapping.layer0(texture(renderOnly)),
                                        modelOutput);
                }

                // ---- modular tools ----
                // Only the neutral fallback MODEL is generated here (the definition that
                // references it comes from ModItemDefinitionProvider: the layered
                // "modular_dreams:modular_tool" type). Writing the item definition here
                // as well would silently overwrite the layered one.
                for (ModularToolType tool : ModularToolType.values()) {
                        String itemName = "modular_" + tool.id;
                        ModelTemplates.FLAT_ITEM.create(
                                        ModularDreams.id(itemName),
                                        TextureMapping.layer0(texture(tool.headPart().id)),
                                        modelOutput);
                }

                // ---- molds ----
                // (the clay and terracotta mold block item definitions are hand-written
                // in src/main/resources too, pointing at the flat block models)

                // ---- guide book ----
                Identifier guideModelId = ModelTemplates.FLAT_ITEM.create(
                                ModularDreams.id("modular_guidebook"),
                                TextureMapping.layer0(texture("modular_guidebook")),
                                modelOutput);
                itemModelOutput.accept(item("modular_guidebook"), ItemModelUtils.plainModel(guideModelId));
        }

        private Item item(String name) {
                return ModItems.byName(name).orElseThrow(() -> new IllegalStateException("missing item " + name));
        }

        private Item blockItem(net.minecraft.world.level.block.Block block) {
                return block.asItem();
        }

        private Material texture(String name) {
                return new Material(ModularDreams.id("item/" + name));
        }

        private Material blockTexture(String name) {
                return new Material(ModularDreams.id("block/" + name));
        }

        @Override
        public String getName() {
                return "Modular Dreams Models";
        }
}
