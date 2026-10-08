package com.modulardreams.block;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import com.modulardreams.ModularDreams;

/**
 * The station blocks of Modular Dreams v2:
 *
 * - Part Picker (wood / stone / copper / iron / diamond): the part-crafting
 *   station. Each tier shapes its own material plus the next tier's material.
 * - Assembler: assembles tools from handle + head, converts vanilla tools,
 */
public class ModBlocks {

    private static final Map<String, Block> BY_NAME = new LinkedHashMap<>();

    public static final PartPickerBlock PART_PICKER_WOOD = registerPicker("part_picker_wood", 1,
            props -> props.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD));
    public static final PartPickerBlock PART_PICKER_STONE = registerPicker("part_picker_stone", 2,
            props -> props.mapColor(MapColor.STONE).strength(3.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));
    public static final PartPickerBlock PART_PICKER_COPPER = registerPicker("part_picker_copper", 3,
            props -> props.mapColor(MapColor.COLOR_ORANGE).strength(3.5F).requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER));
    public static final PartPickerBlock PART_PICKER_IRON = registerPicker("part_picker_iron", 4,
            props -> props.mapColor(MapColor.METAL).strength(4.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));
    public static final PartPickerBlock PART_PICKER_DIAMOND = registerPicker("part_picker_diamond", 5,
            props -> props.mapColor(MapColor.DIAMOND).strength(5.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));
    public static final AssemblerBlock ASSEMBLER = register("assembler",
            props -> props.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD),
            AssemblerBlock::new);
    public static final KilnBlock KILN = register("kiln",
            props -> props.mapColor(net.minecraft.world.level.material.MapColor.TERRACOTTA_ORANGE)
                    .strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT) ? 13 : 0),
            KilnBlock::new);

    private static PartPickerBlock registerPicker(String name, int stationTier,
            java.util.function.UnaryOperator<BlockBehaviour.Properties> properties) {
        return register(name, properties,
                props -> new PartPickerBlock(stationTier, props));
    }

    private static <B extends Block> B register(String name,
            java.util.function.UnaryOperator<BlockBehaviour.Properties> properties,
            java.util.function.Function<BlockBehaviour.Properties, B> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModularDreams.id(name));
        B block = factory.apply(properties.apply(BlockBehaviour.Properties.of()).setId(key));
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        BY_NAME.put(name, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ModularDreams.id(name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }

    public static void initialize() {
        // static init
    }

    public static Block byName(String name) {
        return BY_NAME.get(name);
    }
}
