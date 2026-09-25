package com.modulardreams.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import com.modulardreams.ModularDreams;
import com.modulardreams.equipment.ModularArmorType;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;

/**
 * Generates the per-material client item definitions under assets/modular_dreams/items/
 * (e.g. items/modular_pickaxe_iron.json) that the ITEM_MODEL component points to.
 * The definition and its 2-layer model share the same id by design.
 */
public class ModItemDefinitionProvider extends FabricCodecDataProvider<ClientItem> {

	protected ModItemDefinitionProvider(FabricPackOutput output,
			CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture, PackOutput.Target.RESOURCE_PACK, "items", ClientItem.CODEC);
	}

	@Override
	protected void configure(java.util.function.BiConsumer<Identifier, ClientItem> consumer,
			HolderLookup.Provider provider) {
		for (ModularToolType tool : ModularToolType.values()) {
			for (ModularMaterial material : ModMaterials.toolMaterials()) {
				Identifier id = ModularDreams.id("modular_" + tool.id + "_" + material.id());
				consumer.accept(id, new ClientItem(ItemModelUtils.plainModel(id), ClientItem.Properties.DEFAULT));
			}
		}
		for (ModularArmorType armor : ModularArmorType.values()) {
			for (ModularMaterial material : ModMaterials.plateMaterials()) {
				Identifier id = ModularDreams.id("modular_" + armor.id + "_" + material.id());
				consumer.accept(id, new ClientItem(ItemModelUtils.plainModel(id), ClientItem.Properties.DEFAULT));
			}
		}
	}

	@Override
	public String getName() {
		return "Modular Dreams Item Definitions";
	}
}
