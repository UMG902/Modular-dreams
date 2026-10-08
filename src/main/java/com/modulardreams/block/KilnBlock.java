package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;

import com.modulardreams.blockentity.KilnBlockEntity;
import com.modulardreams.blockentity.ModBlockEntityTypes;
import com.modulardreams.recipe.ModRecipes;

/**
 * The Kiln (v2): the third furnace. Looks and behaves like a furnace, but
 * smelts anything that is not for the smoker or the blast furnace - every
 * input with a smelting recipe and no smoking/blasting recipe, modded
 * recipes included (see {@link ModRecipes#KILN_CHECK}). Cooks at twice the
 * furnace speed via the block entity's speed multiplier (200t -&gt; 100t).
 */
public class KilnBlock extends AbstractFurnaceBlock {

    public KilnBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KilnBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createFurnaceTicker(level, type, ModBlockEntityTypes.KILN);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof KilnBlockEntity kiln) {
            player.openMenu(kiln);
        }
    }
}
