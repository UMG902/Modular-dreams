package com.modulardreams.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;

import com.modulardreams.ModularDreams;

/**
 * Menu types of the two station blocks. Uses Fabric's {@link ExtendedMenuType}
 * so the client menu factory receives the station {@link BlockPos}.
 */
public class ModMenuTypes {

        public static final ResourceKey<MenuType<?>> PART_BUILDER_KEY = key("part_builder");
        public static final ResourceKey<MenuType<?>> ASSEMBLY_TABLE_KEY = key("assembly_table");
        public static final ResourceKey<MenuType<?>> MELTING_UPGRADE_KEY = key("melting_upgrade");
        public static final ResourceKey<MenuType<?>> MELTING_FURNACE_KEY = key("melting_furnace");

        public static final MenuType<PartBuilderMenu> PART_BUILDER = register(PART_BUILDER_KEY,
                        (containerId, inventory, pos) -> new PartBuilderMenu(containerId, inventory,
                                        net.minecraft.world.inventory.ContainerLevelAccess.create(
                                                        inventory.player.level(), pos)));

        public static final MenuType<AssemblyTableMenu> ASSEMBLY_TABLE = register(ASSEMBLY_TABLE_KEY,
                        (containerId, inventory, pos) -> new AssemblyTableMenu(containerId, inventory, pos));

        public static final MenuType<MeltingUpgradeMenu> MELTING_UPGRADE = register(MELTING_UPGRADE_KEY,
                        (containerId, inventory, pos) -> new MeltingUpgradeMenu(containerId, inventory, pos));

        public static final MenuType<MeltingFurnaceMenu> MELTING_FURNACE = register(MELTING_FURNACE_KEY,
                        (containerId, inventory, pos) -> new MeltingFurnaceMenu(containerId, inventory, pos));

        private static <M extends AbstractContainerMenu> MenuType<M> register(
                        ResourceKey<MenuType<?>> key,
                        net.fabricmc.fabric.api.menu.v1.ExtendedMenuType.ExtendedFactory<M, BlockPos> factory) {
                ExtendedMenuType<M, BlockPos> type = new ExtendedMenuType<>(factory, BlockPos.STREAM_CODEC);
                return Registry.register(BuiltInRegistries.MENU, key, type);
        }

        private static ResourceKey<MenuType<?>> key(String name) {
                return ResourceKey.create(Registries.MENU, ModularDreams.id(name));
        }

        public static void initialize() {
                // static init
        }
}
