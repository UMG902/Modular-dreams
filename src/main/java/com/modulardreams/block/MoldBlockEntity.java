package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity of BOTH mold blocks (clay and terracotta): remembers only the
 * wear of a used terracotta mold (clay molds ignore it, max 1 use).
 *
 * <p>The mold's SHAPE is no longer stored here - it lives in the
 * {@code shape} block state property (see {@code MoldBlock} / {@code MoldShape}),
 * which syncs to clients automatically and drives the carved cutout model.
 */
public class MoldBlockEntity extends BlockEntity {

        private int damage;

        public MoldBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
                super(type, pos, state);
        }

        /** Factory for the clay mold block entity type. */
        public static MoldBlockEntity forClay(BlockPos pos, BlockState state) {
                return new MoldBlockEntity(ModBlockEntities.CLAY_MOLD, pos, state);
        }

        /** Factory for the terracotta mold block entity type. */
        public static MoldBlockEntity forTerracotta(BlockPos pos, BlockState state) {
                return new MoldBlockEntity(ModBlockEntities.TERRACOTTA_MOLD, pos, state);
        }

        /** Wear of a terracotta mold (clay molds always stay at 0). */
        public int damage() {
                return this.damage;
        }

        public void setDamage(int damage) {
                this.damage = damage;
                setChanged();
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
                super.saveAdditional(output);
                output.putInt("Damage", this.damage);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
                super.loadAdditional(input);
                this.damage = input.getIntOr("Damage", 0);
        }
}
