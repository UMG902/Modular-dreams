package com.modulardreams.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;

import com.modulardreams.block.ModBlocks;

/** Both station blocks drop themselves when broken. */
public class ModLootProvider extends FabricBlockLootSubProvider {

        protected ModLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
                super(output, registriesFuture);
        }

        @Override
        public void generate() {
                dropSelf(ModBlocks.PART_BUILDER);
                dropSelf(ModBlocks.ASSEMBLY_TABLE);
                dropSelf(ModBlocks.CLAY_MOLD);
                dropSelf(ModBlocks.TERRACOTTA_MOLD);
                dropSelf(ModBlocks.MELTING_UPGRADE);
                // the melting furnace has no item form - it drops a plain furnace
                // (contents are dropped separately by the block's onRemove)
                dropOther(ModBlocks.MELTING_FURNACE, net.minecraft.world.item.Items.FURNACE);
        }

        @Override
        public String getName() {
                return "Modular Dreams Block Loot";
        }
}
