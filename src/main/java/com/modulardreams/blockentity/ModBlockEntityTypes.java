package com.modulardreams.blockentity;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.modulardreams.ModularDreams;
import com.modulardreams.block.ModBlocks;

/**
 * Block entity types of Modular Dreams v2.
 */
public final class ModBlockEntityTypes {

    public static final BlockEntityType<KilnBlockEntity> KILN = register("kiln",
            new BlockEntityType<>(KilnBlockEntity::new, Set.of(ModBlocks.KILN)));

    private static <T extends BlockEntityType<?>> T register(String name, T type) {
        ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                ModularDreams.id(name));
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type);
    }

    public static void initialize() {
        // static init
    }
}
