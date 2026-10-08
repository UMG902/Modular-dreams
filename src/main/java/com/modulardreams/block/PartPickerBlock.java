package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import com.modulardreams.ModularDreams;

/**
 * The Part Picker station block (v2). Stateless: right-clicking opens the
 * part menu, all state lives in the per-open menu (like the vanilla
 * crafting table). The block's tier decides which materials it can shape.
 */
public class PartPickerBlock extends Block {

    private final int stationTier;

    public PartPickerBlock(int stationTier, Properties properties) {
        super(properties);
        this.stationTier = stationTier;
    }

    public int stationTier() {
        return stationTier;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(new PickerMenuProvider(pos));
        return InteractionResult.SUCCESS;
    }

    /** Menu provider carrying the station position to the client menu factory. */
    private record PickerMenuProvider(BlockPos pos) implements ExtendedMenuProvider<BlockPos> {

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.modular_dreams.part_picker");
        }

        @Override
        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
                net.minecraft.world.entity.player.Inventory inventory, Player player) {
            return new com.modulardreams.menu.PartPickerMenu(containerId, inventory, pos);
        }

        @Override
        public BlockPos getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
            return pos;
        }
    }
}
