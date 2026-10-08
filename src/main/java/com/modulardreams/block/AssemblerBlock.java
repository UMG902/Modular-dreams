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

/**
 * The Assembler station block (v2). Stateless: right-clicking opens the
 * assembly menu (handle + head -> tool, vanilla tool -> modular tool).
 */
public class AssemblerBlock extends Block {

    public AssemblerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(new AssemblerMenuProvider(pos));
        return InteractionResult.SUCCESS;
    }

    /** Menu provider carrying the station position to the client menu factory. */
    private record AssemblerMenuProvider(BlockPos pos) implements ExtendedMenuProvider<BlockPos> {

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.modular_dreams.assembler");
        }

        @Override
        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
                net.minecraft.world.entity.player.Inventory inventory, Player player) {
            return new com.modulardreams.menu.AssemblerMenu(containerId, inventory, pos);
        }

        @Override
        public BlockPos getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
            return pos;
        }
    }
}
