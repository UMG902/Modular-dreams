package com.modulardreams.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.modulardreams.ModularDreams;

/**
 * Block entity types of Modular Dreams (overhaul step 1). The clay and the
 * terracotta mold share one entity class but are registered as two types
 * (one per valid block), so each block always creates the right one.
 */
public class ModBlockEntities {

        public static final BlockEntityType<MoldBlockEntity> CLAY_MOLD = register(
                        "clay_mold", new BlockEntityType<>(MoldBlockEntity::forClay,
                                        java.util.Set.of(ModBlocks.CLAY_MOLD)));
        public static final BlockEntityType<MoldBlockEntity> TERRACOTTA_MOLD = register(
                        "terracotta_mold", new BlockEntityType<>(MoldBlockEntity::forTerracotta,
                                        java.util.Set.of(ModBlocks.TERRACOTTA_MOLD)));
        public static final BlockEntityType<AssemblyTableBlockEntity> ASSEMBLY_TABLE = register(
                        "assembly_table", new BlockEntityType<>(AssemblyTableBlockEntity::new,
                                        java.util.Set.of(ModBlocks.ASSEMBLY_TABLE)));
        public static final BlockEntityType<MeltingUpgradeBlockEntity> MELTING_UPGRADE = register(
                        "melting_upgrade", new BlockEntityType<>(MeltingUpgradeBlockEntity::new,
                                        java.util.Set.of(ModBlocks.MELTING_UPGRADE)));
        public static final BlockEntityType<MeltingFurnaceBlockEntity> MELTING_FURNACE = register(
                        "melting_furnace", new BlockEntityType<>(MeltingFurnaceBlockEntity::new,
                                        java.util.Set.of(ModBlocks.MELTING_FURNACE)));

        private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
                ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                                ModularDreams.id(name));
                return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type);
        }

        public static void initialize() {
                // static init
        }
}
