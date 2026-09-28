package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.menu.StationLogic;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.MoldShape;
import com.modulardreams.part.PartType;

/**
 * A placeable mold block - the CLAY mold and the TERRACOTTA mold share this
 * class. Both use the flat cutting-board model (one pixel tall); right-clicking
 * a placed CLAY mold with a PART item presses that shape into it (the part is
 * only a template and is not consumed). The shape lives in the {@code shape}
 * BLOCK STATE PROPERTY: every shaped state points at a block model whose top
 * face is a cutout texture with the part's silhouette removed, so the mold
 * shows a real carved cavity (see {@code MoldShape}).
 * Terracotta molds reject shaping: they keep the shape they were baked with.
 *
 * <p>Breaking a mold drops its item carrying the {@code mold_part} shape
 * (and the wear of a used terracotta mold); cooking the clay mold item in a
 * furnace turns it into a terracotta mold of the same shape.
 */
public class MoldBlock extends Block implements EntityBlock {

        /** Which part the mold is shaped into (NONE = fresh). */
        public static final EnumProperty<MoldShape> SHAPE = EnumProperty.create("shape", MoldShape.class);

        /** Creates the correct block entity (clay or terracotta type). */
        private final java.util.function.BiFunction<BlockPos, BlockState, BlockEntity> entityFactory;

        /** One pixel tall, full 16x16 footprint - matches the cutting-board model. */
        private static final VoxelShape FLAT_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

        public MoldBlock(Properties properties,
                        java.util.function.BiFunction<BlockPos, BlockState, BlockEntity> entityFactory) {
                super(properties);
                this.entityFactory = entityFactory;
                this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, MoldShape.NONE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                builder.add(SHAPE);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                        CollisionContext context) {
                return FLAT_SHAPE;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                        CollisionContext context) {
                return FLAT_SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
                return this.entityFactory.apply(pos, state);
        }

        /**
         * Drops the mold item preserving the shaped part and the wear. The
         * loot table cannot know the block entity's shape, so the survival
         * drop is handled here instead of via loot (explosions fall back to
         * the plain loot drop).
         */
        @Override
        public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state,
                        BlockEntity be, ItemStack tool) {
                player.awardStat(Stats.BLOCK_MINED.get(this));
                player.causeFoodExhaustion(0.005F);
                ItemStack drop = new ItemStack(this.asItem());
                MoldShape shape = state.getValue(SHAPE);
                if (shape.part().isPresent()) {
                        drop.set(ModDataComponents.MOLD_PART, shape.part().get().id);
                }
                if (be instanceof MoldBlockEntity mold && mold.damage() > 0) {
                        drop.set(net.minecraft.core.component.DataComponents.DAMAGE, mold.damage());
                }
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5,
                                pos.getZ() + 0.5, drop));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                        BlockHitResult hit) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                        Player player, InteractionHand hand, BlockHitResult hit) {
                if (level.isClientSide()) {
                        return InteractionResult.SUCCESS;
                }
                // Only CLAY molds can be shaped by hand: a terracotta mold keeps
                // the shape it received when the clay was baked (shaping it again
                // would bypass the whole bake step and its 3-use wear system).
                if (this == ModBlocks.TERRACOTTA_MOLD) {
                        return InteractionResult.PASS;
                }
                if (state.getValue(SHAPE) != MoldShape.NONE) {
                        return InteractionResult.PASS; // already shaped
                }
                var identity = ModPartItems.resolve(stack);
                if (identity.isEmpty()) {
                        return InteractionResult.PASS;
                }
                PartType part = identity.get().part();
                ModularMaterial material = identity.get().material();
                if (!StationLogic.canMake(part, material)) {
                        return InteractionResult.PASS;
                }
                // the shape travels through the BLOCK STATE: flag 3 syncs it to
                // clients and the cutout model shows the carved cavity at once
                level.setBlock(pos, state.setValue(SHAPE, MoldShape.of(part)), 3);
                level.playSound(null, pos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
                return InteractionResult.SUCCESS_SERVER;
        }
}
