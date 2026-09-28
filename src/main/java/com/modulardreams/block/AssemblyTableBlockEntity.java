package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity of the ASSEMBLY TABLE - the three input slots (head, binding,
 * handle) live IN the block, so placed parts stay in the table when the GUI
 * is closed (and survive a save/load). Breaking the block spills the parts on
 * the ground.
 *
 * <p>The open menu registers itself as the {@link #changeListener} so the
 * result preview recomputes whenever a slot changes.
 */
public class AssemblyTableBlockEntity extends BlockEntity implements Container {

        public static final int CONTAINER_SIZE = 3;
        public static final int HEAD_SLOT = 0;
        public static final int BINDING_SLOT = 1;
        public static final int HANDLE_SLOT = 2;

        private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

        /** Set by the open menu; recomputes the assembly result on every change. */
        public Runnable changeListener;

        public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
                super(ModBlockEntities.ASSEMBLY_TABLE, pos, state);
        }

        /** Notifies the open menu (if any) that the contents changed. */
        private void contentsChanged() {
                setChanged();
                if (this.changeListener != null) {
                        this.changeListener.run();
                }
        }

        // ------------------------------------------------------------------ block removal

        /** Breaking the table spills the stored parts on the ground. */
        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
                super.preRemoveSideEffects(pos, state);
                if (this.level != null && !this.level.isClientSide()) {
                        for (ItemStack stack : this.items) {
                                if (!stack.isEmpty()) {
                                        net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(),
                                                        pos.getY(), pos.getZ(), stack);
                                }
                        }
                        this.items.clear();
                }
        }

        // ------------------------------------------------------------------ container

        @Override
        public int getContainerSize() {
                return CONTAINER_SIZE;
        }

        @Override
        public boolean isEmpty() {
                for (ItemStack stack : this.items) {
                        if (!stack.isEmpty()) {
                                return false;
                        }
                }
                return true;
        }

        @Override
        public ItemStack getItem(int slot) {
                return this.items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
                ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
                if (!removed.isEmpty()) {
                        contentsChanged();
                }
                return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
                ItemStack taken = ContainerHelper.takeItem(this.items, slot);
                if (!taken.isEmpty()) {
                        contentsChanged();
                }
                return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
                this.items.set(slot, stack);
                if (stack.getCount() > this.getMaxStackSize()) {
                        stack.setCount(this.getMaxStackSize());
                }
                contentsChanged();
        }

        @Override
        public boolean stillValid(Player player) {
                return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
                this.items.clear();
                contentsChanged();
        }

        // ------------------------------------------------------------------ client sync

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
                return saveCustomOnly(provider);
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
                return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
        }

        // ------------------------------------------------------------------ save / load

        @Override
        protected void saveAdditional(ValueOutput output) {
                super.saveAdditional(output);
                ContainerHelper.saveAllItems(output, this.items, true);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
                super.loadAdditional(input);
                this.items.clear();
                ContainerHelper.loadAllItems(input, this.items);
        }
}
