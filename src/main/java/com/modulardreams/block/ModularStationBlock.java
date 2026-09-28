package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import com.modulardreams.ModularDreams;

/**
 * A Tinkers'-style station block. Right-clicking opens its menu; the block
 * itself is stateless (all station state lives in the per-open menu, like the
 * vanilla crafting table).
 */
public class ModularStationBlock extends Block {

        private final String stationName;

        public ModularStationBlock(String stationName, Properties properties) {
                super(properties);
                this.stationName = stationName;
        }

        public String stationName() {
                return stationName;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                        BlockHitResult hitResult) {
                if (level.isClientSide()) {
                        return InteractionResult.SUCCESS;
                }
                player.openMenu(new StationMenuProvider(this.stationName, pos, titleKey()));
                return InteractionResult.SUCCESS;
        }

        private String titleKey() {
                return "container.modular_dreams." + stationName;
        }

        /** Menu provider carrying the station position to the client menu factory. */
        private record StationMenuProvider(String stationName, BlockPos pos, String titleKey)
                        implements ExtendedMenuProvider<BlockPos> {

                @Override
                public Component getDisplayName() {
                        return Component.translatable(titleKey);
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
                                net.minecraft.world.entity.player.Inventory inventory,
                                net.minecraft.world.entity.player.Player player) {
                        return ModularStationBlock.openMenu(stationName, containerId, inventory, pos);
                }

                @Override
                public BlockPos getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
                        return pos;
                }
        }

        /** Creates the right menu for a station name (server + client side). */
        public static net.minecraft.world.inventory.AbstractContainerMenu openMenu(String stationName,
                        int containerId, net.minecraft.world.entity.player.Inventory inventory, BlockPos pos) {
                var access = net.minecraft.world.inventory.ContainerLevelAccess.create(
                                inventory.player.level(), pos);
                if (com.modulardreams.block.ModBlocks.PART_BUILDER.stationName().equals(stationName)) {
                        return new com.modulardreams.menu.PartBuilderMenu(containerId, inventory, access);
                }
                // the assembly table is stateful: its parts live in the block entity
                net.minecraft.world.Container station = inventory.player.level().getBlockEntity(pos)
                                instanceof AssemblyTableBlockEntity blockEntity ? (net.minecraft.world.Container) blockEntity
                                                : null;
                return new com.modulardreams.menu.AssemblyTableMenu(containerId, inventory, access, station);
        }
}
