package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.modulardreams.ModularDreams;
import com.modulardreams.equipment.MoldBlockItem;

/**
 * The Melting Upgrade. Placed UNDERNEATH a furnace; a mold goes into it and
 * metal ingots in the furnace's input slot melt into the mold's shape.
 *
 * <p>Placing it under a vanilla furnace (or placing a furnace on top of it)
 * quietly transforms that furnace into the {@link MeltingFurnaceBlock} - a
 * block that looks identical but burns the melting fuel table and casts parts
 * from the mold stored here.
 *
 * <p>Right-clicking with a mold inserts it (replacing any mold inside); any
 * other right-click opens the upgrade's own GUI: the MOLD slot at the bottom
 * and the slot for the finished part at the top. Breaking the upgrade drops
 * its contents and reverts the furnace above to a plain furnace.
 */
public class MeltingUpgradeBlock extends Block implements EntityBlock {

        public MeltingUpgradeBlock(Properties properties) {
                super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
                return new MeltingUpgradeBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                        BlockEntityType<T> type) {
                if (level.isClientSide()) {
                        return null;
                }
                return createTickerHelper(type, ModBlockEntities.MELTING_UPGRADE,
                                MeltingUpgradeBlockEntity::serverTick);
        }

        /** Any empty-handed (or non-mold) right-click opens the upgrade's GUI. */
        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                        BlockHitResult hit) {
                if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MeltingUpgradeBlockEntity upgrade) {
                        player.openMenu(upgrade);
                }
                return InteractionResult.SUCCESS;
        }

        /** Right-click with a mold inserts it; any other item opens the GUI. */
        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                        Player player, InteractionHand hand, BlockHitResult hit) {
                if (level.getBlockEntity(pos) instanceof MeltingUpgradeBlockEntity upgrade) {
                        if (MoldBlockItem.isMold(stack)) {
                                if (!level.isClientSide()) {
                                        ItemStack previous = upgrade.getMold();
                                        ItemStack inserted = stack.copyWithCount(1);
                                        if (stack.getCount() > 1) {
                                                stack.shrink(1);
                                        } else {
                                                player.setItemInHand(hand, previous); // swap molds
                                        }
                                        upgrade.setMold(inserted);
                                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS,
                                                        0.5F, 1.2F);
                                }
                                return InteractionResult.SUCCESS;
                        }
                        if (!level.isClientSide()) {
                                player.openMenu(upgrade);
                        }
                        return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
        }

        @SuppressWarnings("unchecked")
        private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
                        BlockEntityType<A> given, BlockEntityType<E> expected,
                        BlockEntityTicker<? super E> ticker) {
                return given == expected ? (BlockEntityTicker<A>) ticker : null;
        }

        static {
                ModularDreams.LOGGER.debug("MeltingUpgradeBlock loaded");
        }
}
