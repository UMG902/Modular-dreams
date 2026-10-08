package com.modulardreams.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;

import com.modulardreams.ModularDreams;

/**
 * Menu types of the v2 station blocks. Uses Fabric's {@link ExtendedMenuType}
 * so the client menu factory receives the station {@link BlockPos}.
 */
public class ModMenuTypes {

    public static final MenuType<PartPickerMenu> PART_PICKER = register("part_picker",
            (containerId, inventory, pos) -> new PartPickerMenu(containerId, inventory, pos));

    public static final MenuType<AssemblerMenu> ASSEMBLER = register("assembler",
            (containerId, inventory, pos) -> new AssemblerMenu(containerId, inventory, pos));

    public static final MenuType<KilnMenu> KILN = register("kiln",
            (containerId, inventory, pos) -> new KilnMenu(containerId, inventory));

    public static final MenuType<FletchingMenu> FLETCHING = register("fletching",
            (containerId, inventory, pos) -> new FletchingMenu(containerId, inventory, pos));

    private static <M extends AbstractContainerMenu> MenuType<M> register(String name,
            ExtendedMenuType.ExtendedFactory<M, BlockPos> factory) {
        ExtendedMenuType<M, BlockPos> type = new ExtendedMenuType<>(factory, BlockPos.STREAM_CODEC);
        ResourceKey<MenuType<?>> key = ResourceKey.create(Registries.MENU, ModularDreams.id(name));
        return Registry.register(BuiltInRegistries.MENU, key, type);
    }

    public static void initialize() {
        // static init
    }
}
