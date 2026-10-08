package com.modulardreams;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modular Dreams v2 - fresh-start modular equipment overhaul.
 *
 * <p>Every tool is handle + head. Parts are shaped at the Part Picker
 * (five tiers), assembled at the Assembler (which also converts vanilla
 * tools). Rose gold joins iron and gold
 * at their station tier; netherite parts are shaped at the diamond station.
 * All assembled stats are baked into vanilla data components at assembly
 * time by the {@code StatsEngine}.
 */
public class ModularDreams implements ModInitializer {
    public static final String MOD_ID = "modular_dreams";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        com.modulardreams.component.ModDataComponents.initialize();
        com.modulardreams.material.ModMaterials.initialize();
        com.modulardreams.network.ModNetworking.initialize();
        com.modulardreams.equipment.ModItems.initialize();
        // All 58 part items (handle_wood, pickaxe_head_copper, ...). Must run
        // after ModMaterials and before anything that lists parts (creative
        // tab, Part Picker) - if this is skipped the creative inventory
        // crashes the client with "No part item for ..." on first open.
        com.modulardreams.equipment.ModPartItems.registerAll();
        // The 5 armor linings (leather_lining, wool_lining, ...): wearable
        // padding with no armor value, each carrying its material's trait.
        com.modulardreams.equipment.ModArmorItems.registerAll();
        // The 28 armor platings (helmet_plating_iron, chestplate_plating_gold,
        // ...): slot-specific outer-layer parts shaped at the Part Picker.
        com.modulardreams.equipment.ModPlatingItems.registerAll();
        com.modulardreams.block.ModBlocks.initialize();
        com.modulardreams.blockentity.ModBlockEntityTypes.initialize();
        // Captures the server's registry access when a server starts, so
        // StatsEngine can resolve data-pack registries (spear damage type,
        // netherite fire resistance) during assembly. Registered BEFORE the
        // recipes/self-test so the capture is in place when they run.
        com.modulardreams.registry.ModRegistryAccess.init();
        com.modulardreams.recipe.ModRecipes.initialize();
        // Runs every trait gameplay effect (wood regen, shadowed mining,
        // powered immunity, last stand, piglin favor, defiance, fortunate
        // saves, netherforged, blazing, windborne, linings, modifiers).
        com.modulardreams.traits.TraitEngine.initialize();
        com.modulardreams.menu.ModMenuTypes.initialize();
        // Makes the vanilla fletching table open the fletching menu
        // (2 potions + up to 64 arrows -> tipped arrows).
        com.modulardreams.block.FletchingTableHooks.initialize();
        com.modulardreams.registry.ModCreativeTabs.initialize();

        LOGGER.info("Modular Dreams v2 initialized. Dream in parts!");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
