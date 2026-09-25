package com.modulardreams.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import com.modulardreams.ModularDreams;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModularArmorType;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;

/**
 * Generates all item models and client item definitions.
 *
 * - Part items: simple flat models with their own texture.
 * - Modular tools: two-layer models (neutral base + colored head overlay);
 *   one model per head material, selected at runtime via the ITEM_MODEL
 *   component that StatsEngine bakes onto every assembled item.
 * - Modular armor: same trick with plate overlays (on-body rendering reuses
 *   vanilla equipment assets).
 */
public class ModModelProvider extends FabricModelProvider {

	protected ModModelProvider(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
		// no blocks in this mod
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		var modelOutput = generators.modelOutput;
		var itemModelOutput = generators.itemModelOutput;

		// ---- part items ----
		for (PartType part : PartType.values()) {
			for (ModularMaterial material : part.allowedMaterials()) {
				String name = material.id() + "_" + part.id;
				Identifier modelId = ModelTemplates.FLAT_ITEM.create(
						ModularDreams.id(name),
						TextureMapping.layer0(texture(name)),
						modelOutput);
				itemModelOutput.accept(ModPartItems.get(part, material), ItemModelUtils.plainModel(modelId));
			}
		}

		// ---- modular tools (2 layers: base + head overlay per material) ----
		for (ModularToolType tool : ModularToolType.values()) {
			String itemName = "modular_" + tool.id;
			// default (unassembled) model: base layer only
			Identifier baseModelId = ModelTemplates.FLAT_ITEM.create(
					ModularDreams.id(itemName),
					TextureMapping.layer0(texture(itemName + "_base")),
					modelOutput);
			itemModelOutput.accept(item(itemName), ItemModelUtils.plainModel(baseModelId));

			for (ModularMaterial material : ModMaterials.toolMaterials()) {
				Identifier layeredId = ModelTemplates.TWO_LAYERED_ITEM.create(
						ModularDreams.id(itemName + "_" + material.id()),
						TextureMapping.layered(texture(itemName + "_base"), texture(itemName + "_overlay_" + material.id())),
						modelOutput);
			}
		}

		// ---- modular armor (2 layers: lining base + plate overlay per material) ----
		for (ModularArmorType armor : ModularArmorType.values()) {
			String itemName = "modular_" + armor.id;
			Identifier baseModelId = ModelTemplates.FLAT_ITEM.create(
					ModularDreams.id(itemName),
					TextureMapping.layer0(texture(itemName + "_base")),
					modelOutput);
			itemModelOutput.accept(item(itemName), ItemModelUtils.plainModel(baseModelId));

			for (ModularMaterial material : ModMaterials.plateMaterials()) {
				Identifier layeredId = ModelTemplates.TWO_LAYERED_ITEM.create(
						ModularDreams.id(itemName + "_" + material.id()),
						TextureMapping.layered(texture(itemName + "_base"), texture(itemName + "_overlay_" + material.id())),
						modelOutput);
			}
		}

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

	private Material texture(String name) {
		return new Material(ModularDreams.id("item/" + name));
	}

	@Override
	public String getName() {
		return "Modular Dreams Models";
	}
}
