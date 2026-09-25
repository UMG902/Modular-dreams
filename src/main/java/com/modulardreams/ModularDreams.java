package com.modulardreams;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modular Dreams - a Tinkers'-style modular equipment system built strictly on
 * vanilla Minecraft tools, weapons, armor and resources.
 *
 * Everything is data-component driven: assembled gear stores its part materials
 * and modifier levels in a custom {@code MODULAR_DATA} component, and all actual
 * gameplay stats (durability, mining speed, attack damage, armor, enchantability,
 * repair items, ...) are baked into vanilla data components at assembly time.
 */
public class ModularDreams implements ModInitializer {
        public static final String MOD_ID = "modular_dreams";

        public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

        @Override
        public void onInitialize() {
                com.modulardreams.component.ModDataComponents.initialize();
                com.modulardreams.part.PartType.initialize();
                com.modulardreams.material.ModMaterials.initialize();
                com.modulardreams.modifier.Modifier.initialize();
                com.modulardreams.registry.ModRegistryAccess.init();
                com.modulardreams.equipment.ModItems.initialize();
                com.modulardreams.recipe.ModRecipeSerializers.initialize();
                com.modulardreams.registry.ModCreativeTabs.initialize();

                LOGGER.info("Modular Dreams initialized. Assemble something wonderful!");
        }

        public static Identifier id(String path) {
                return Identifier.fromNamespaceAndPath(MOD_ID, path);
        }
}
