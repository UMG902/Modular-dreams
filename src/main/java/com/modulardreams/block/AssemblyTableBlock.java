package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.modulardreams.menu.AssemblyTableMenu;

/**
 * The ASSEMBLY TABLE - unlike the Part Builder it is a STATEFUL station: the
 * three part slots live in an {@link AssemblyTableBlockEntity} inside the
 * block, so inserted parts remain in the table when the GUI is closed.
 */
public class AssemblyTableBlock extends ModularStationBlock implements EntityBlock {

        public AssemblyTableBlock(String stationName, Properties properties) {
                super(stationName, properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
                return new AssemblyTableBlockEntity(pos, state);
        }
}
