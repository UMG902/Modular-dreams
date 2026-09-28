package com.modulardreams.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import com.modulardreams.ModularDreams;
import com.modulardreams.block.ModBlocks;

/** Block tags for the station blocks (mineable with an axe like vanilla tables). */
public class ModBlockTagProvider extends FabricTagsProvider<Block> {

        protected ModBlockTagProvider(FabricPackOutput output,
                        CompletableFuture<HolderLookup.Provider> registriesFuture) {
                super(output, Registries.BLOCK, registriesFuture);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
                this.builder(BlockTags.MINEABLE_WITH_AXE)
                                .add(ResourceKey.create(Registries.BLOCK, ModularDreams.id("part_builder")))
                                .add(ResourceKey.create(Registries.BLOCK, ModularDreams.id("assembly_table")));
                this.builder(BlockTags.MINEABLE_WITH_PICKAXE)
                                .add(ResourceKey.create(Registries.BLOCK, ModularDreams.id("melting_upgrade")))
                                .add(ResourceKey.create(Registries.BLOCK, ModularDreams.id("melting_furnace")))
                                .add(ResourceKey.create(Registries.BLOCK, ModularDreams.id("terracotta_mold")));
                this.builder(BlockTags.MINEABLE_WITH_SHOVEL)
                                .add(ResourceKey.create(Registries.BLOCK, ModularDreams.id("clay_mold")));
        }

        @Override
        public String getName() {
                return "Modular Dreams Block Tags";
        }
}
