package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The MELTING FURNACE. When a Melting Upgrade is placed directly underneath a
 * vanilla furnace (or a furnace is placed on top of a Melting Upgrade) the
 * furnace quietly becomes this block: it uses the furnace's own block models,
 * so from the outside it looks exactly the same, but its GUI replaces the
 * output slot with the mold of the upgrade and it burns the special melting
 * fuel table (see {@link MeltingFurnaceBlockEntity}).
 *
 * <p>Breaking the Melting Upgrade reverts this block into a plain furnace;
 * breaking the furnace itself drops a furnace plus its contents.
 */
public class MeltingFurnaceBlock extends AbstractFurnaceBlock implements EntityBlock {

        public MeltingFurnaceBlock(Properties properties) {
                super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
                return new MeltingFurnaceBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                        BlockEntityType<T> type) {
                if (level.isClientSide()) {
                        return null;
                }
                return type == ModBlockEntities.MELTING_FURNACE
                                ? (BlockEntityTicker<T>) (BlockEntityTicker<MeltingFurnaceBlockEntity>)
                                                MeltingFurnaceBlockEntity::serverTick
                                : null;
        }

        @Override
        protected void openContainer(Level level, BlockPos pos, Player player) {
                if (!level.isClientSide()
                                && level.getBlockEntity(pos) instanceof MeltingFurnaceBlockEntity furnace) {
                        player.openMenu(furnace);
                }
        }

        /**
         * Turns a vanilla furnace sitting directly above a Melting Upgrade into
         * the melting furnace, carrying input and fuel over (any vanilla
         * smelting output still in the furnace spills out). Called every tick
         * by the upgrade below - so either placement order ends up
         * transformed.
         *
         * <p>Uses {@code UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS} so the vanilla
         * furnace does not spill its contents while they are being carried over.
         */
        public static void transformFromFurnace(Level level, BlockPos furnacePos) {
                BlockState oldState = level.getBlockState(furnacePos);
                if (!oldState.is(Blocks.FURNACE)) {
                        return;
                }
                NonNullList<ItemStack> carried = NonNullList.withSize(2, ItemStack.EMPTY);
                if (level.getBlockEntity(furnacePos) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
                        for (int i = 0; i < 2; i++) {
                                carried.set(i, furnace.getItem(i).copy());
                        }
                        // a vanilla furnace keeps its result in slot 2; the melting
                        // furnace has no output slot, so spill it out
                        ItemStack oldOutput = furnace.getItem(2);
                        if (!oldOutput.isEmpty()) {
                                net.minecraft.world.Containers.dropItemStack(level, furnacePos.getX() + 0.5,
                                                furnacePos.getY() + 0.5, furnacePos.getZ() + 0.5, oldOutput);
                        }
                }
                BlockState newState = ModBlocks.MELTING_FURNACE.defaultBlockState()
                                .setValue(AbstractFurnaceBlock.FACING, oldState.getValue(AbstractFurnaceBlock.FACING))
                                .setValue(AbstractFurnaceBlock.LIT, false);
                level.setBlock(furnacePos, newState, Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
                if (level.getBlockEntity(furnacePos) instanceof MeltingFurnaceBlockEntity melting) {
                        for (int i = 0; i < 2; i++) {
                                melting.setItem(i, carried.get(i));
                        }
                }
        }

        /** Reverts the melting furnace back into a plain vanilla furnace (contents carried over). */
        public static void revertToFurnace(Level level, BlockPos furnacePos) {
                BlockState oldState = level.getBlockState(furnacePos);
                if (!(oldState.getBlock() instanceof MeltingFurnaceBlock)) {
                        return;
                }
                NonNullList<ItemStack> carried = NonNullList.withSize(2, ItemStack.EMPTY);
                if (level.getBlockEntity(furnacePos) instanceof MeltingFurnaceBlockEntity melting) {
                        for (int i = 0; i < 2; i++) {
                                carried.set(i, melting.getItem(i).copy());
                        }
                }
                BlockState newState = Blocks.FURNACE.defaultBlockState()
                                .setValue(AbstractFurnaceBlock.FACING, oldState.getValue(AbstractFurnaceBlock.FACING));
                level.setBlock(furnacePos, newState, Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
                if (level.getBlockEntity(furnacePos) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
                        for (int i = 0; i < 2; i++) {
                                furnace.setItem(i, carried.get(i));
                        }
                }
        }

        static {
                com.modulardreams.ModularDreams.LOGGER.debug("MeltingFurnaceBlock loaded");
        }
}
