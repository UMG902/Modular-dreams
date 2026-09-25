package com.modulardreams.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Datagen entrypoint of Modular Dreams. Generates models, language files,
 * recipes, tags and advancements into src/main/generated.
 */
public class ModDataGenerator implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();

		pack.addProvider(ModLangProvider::new);
		pack.addProvider(ModRecipeProvider::new);
		pack.addProvider(ModTagProvider::new);
		pack.addProvider(ModAdvancementProvider::new);
		pack.addProvider(ModItemDefinitionProvider::new);
		pack.addProvider(ModModelProvider::new);
	}
}
