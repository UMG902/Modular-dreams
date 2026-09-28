package com.modulardreams.block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.modulardreams.ModularDreams;
import com.modulardreams.equipment.MoldBlockItem;

/**
 * The station blocks of Modular Dreams (overhaul step 1):
 *
 * - Part Builder    : crafts tool parts from material items (stonecutter-like)
 * - Assembly Table  : assembles tools from handle + binding + head
 * - Clay Mold       : placeable flat block, right-click a part onto it to shape it
 * - Terracotta Mold : the baked mold, also a placeable flat block
 * - Melting Upgrade : placed under a furnace; melts metals into shaped molds
 */
public class ModBlocks {

        private static final Map<String, Block> BY_NAME = new LinkedHashMap<>();

        public static final ModularStationBlock PART_BUILDER = registerStation("part_builder");
        public static final AssemblyTableBlock ASSEMBLY_TABLE = registerAssemblyStation("assembly_table");
        public static final MoldBlock CLAY_MOLD = registerMold("clay_mold", MoldBlockEntity::forClay,
                        props -> props.mapColor(MapColor.CLAY).strength(0.6F)
                                        .sound(SoundType.GRAVEL).noOcclusion());
        /** Terracotta mold: same flat block as the clay mold, drops its own item (3 uses). */
        public static final MoldBlock TERRACOTTA_MOLD = registerMold("terracotta_mold", MoldBlockEntity::forTerracotta,
                        props -> props.mapColor(MapColor.TERRACOTTA_WHITE).strength(1.25F)
                                        .requiresCorrectToolForDrops().sound(SoundType.DECORATED_POT).noOcclusion());
        public static final MeltingUpgradeBlock MELTING_UPGRADE = register("melting_upgrade",
                        props -> new MeltingUpgradeBlock(props.mapColor(MapColor.METAL).strength(3.5F)
                                        .requiresCorrectToolForDrops().sound(SoundType.METAL)));
        /** The melting furnace has NO item form: it only exists by transforming a furnace. */
        public static final MeltingFurnaceBlock MELTING_FURNACE = registerBlockOnly("melting_furnace",
                        props -> new MeltingFurnaceBlock(props));

        private static ModularStationBlock registerStation(String name) {
                return register(name, props -> new ModularStationBlock(name,
                                props.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
        }

        /** The assembly table is a stateful station (its parts live in a block entity). */
        private static AssemblyTableBlock registerAssemblyStation(String name) {
                return register(name, props -> new AssemblyTableBlock(name,
                                props.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
        }

        /**
         * Registers one of the mold blocks with a {@link MoldBlockItem} (carries
         * the mold tooltip; terracotta gets 3 uses = durability 3).
         */
        private static MoldBlock registerMold(String name,
                        java.util.function.BiFunction<BlockPos, BlockState, BlockEntity> entityFactory,
                        java.util.function.UnaryOperator<BlockBehaviour.Properties> properties) {
                ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModularDreams.id(name));
                MoldBlock block = new MoldBlock(properties.apply(BlockBehaviour.Properties.of()).setId(key),
                                entityFactory);
                Registry.register(BuiltInRegistries.BLOCK, key, block);
                BY_NAME.put(name, block);

                ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ModularDreams.id(name));
                Item.Properties itemProps = new Item.Properties().setId(itemKey);
                if (name.equals("terracotta_mold")) {
                        itemProps = itemProps.durability(3);
                }
                Registry.register(BuiltInRegistries.ITEM, itemKey, new MoldBlockItem(block, itemProps));
                return block;
        }

        /** Registers a block WITHOUT a BlockItem (the melting furnace has no item form). */
        private static <B extends Block> B registerBlockOnly(String name,
                        java.util.function.Function<BlockBehaviour.Properties, B> factory) {
                ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModularDreams.id(name));
                B block = factory.apply(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).setId(key));
                Registry.register(BuiltInRegistries.BLOCK, key, block);
                BY_NAME.put(name, block);
                return block;
        }

        private static <B extends Block> B register(String name,
                        java.util.function.Function<BlockBehaviour.Properties, B> factory) {
                ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModularDreams.id(name));
                B block = factory.apply(BlockBehaviour.Properties.of().setId(key));
                Registry.register(BuiltInRegistries.BLOCK, key, block);
                BY_NAME.put(name, block);

                ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ModularDreams.id(name));
                BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey));
                Registry.register(BuiltInRegistries.ITEM, itemKey, item);
                return block;
        }

        public static Optional<Block> byName(String name) {
                return Optional.ofNullable(BY_NAME.get(name));
        }

        public static void initialize() {
                com.modulardreams.block.ModBlockEntities.initialize();
                // static init
        }
}
