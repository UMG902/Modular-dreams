package com.modulardreams.block;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.equipment.MoldBlockItem;
import com.modulardreams.menu.MeltingUpgradeMenu;
import com.modulardreams.part.PartType;

/**
 * Block entity of the Melting Upgrade - the mold holder that goes UNDERNEATH
 * a furnace. It now has its OWN GUI (two slots):
 *
 * <ul>
 *   <li><b>bottom slot</b>: the mold. Insert it by right-clicking the block
 *       with a mold in hand, or open the GUI empty-handed and drop the mold
 *       into the slot.</li>
 *   <li><b>top slot</b>: the finished metal part - the melting furnace above
 *       casts into the mold and the part appears here.</li>
 * </ul>
 *
 * <p>Other jobs:
 * <ul>
 *   <li>each tick, turn a vanilla furnace sitting directly above into the
 *       MELTING FURNACE (either placement order ends up transformed);</li>
 *   <li>apply mold wear when the melting furnace completes a cast: clay molds
 *       are consumed after one use, terracotta molds lose one durability
 *       point per use (max durability 3) and break on the third.</li>
 * </ul>
 */
public class MeltingUpgradeBlockEntity extends BlockEntity
                implements Container, ExtendedMenuProvider<BlockPos> {

        public static final int MOLD_SLOT = 0;
        public static final int OUTPUT_SLOT = 1;
        public static final int CONTAINER_SIZE = 2;

        private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

        public MeltingUpgradeBlockEntity(BlockPos pos, BlockState state) {
                super(ModBlockEntities.MELTING_UPGRADE, pos, state);
        }

        // ------------------------------------------------------------------ mold access (furnace + block)

        public ItemStack getMold() {
                return this.items.get(MOLD_SLOT);
        }

        /** Inserts a mold (server side, from the block's use handler). */
        public void setMold(ItemStack stack) {
                this.items.set(MOLD_SLOT, stack.copy());
                setChanged();
                syncToClients();
        }

        /** Removes and returns the inserted mold. */
        public ItemStack takeMold() {
                ItemStack taken = this.items.get(MOLD_SLOT);
                this.items.set(MOLD_SLOT, ItemStack.EMPTY);
                setChanged();
                syncToClients();
                return taken;
        }

        // ------------------------------------------------------------------ part output (filled by the furnace)

        /** @return whether the finished part would fit into the output slot. */
        public boolean outputAccepts(ItemStack result) {
                ItemStack output = this.items.get(OUTPUT_SLOT);
                return output.isEmpty()
                                || (ItemStack.isSameItemSameComponents(output, result)
                                                && output.getCount() + result.getCount() <= output
                                                                .getMaxStackSize());
        }

        /** Places a finished part into the output slot (server side, from the furnace). */
        public void insertOutput(ItemStack result) {
                if (result.isEmpty()) {
                        return;
                }
                ItemStack output = this.items.get(OUTPUT_SLOT);
                if (output.isEmpty()) {
                        this.items.set(OUTPUT_SLOT, result.copy());
                } else {
                        output.grow(result.getCount());
                        this.items.set(OUTPUT_SLOT, output);
                }
                setChanged();
                syncToClients();
        }

        /**
         * Applies one cast of mold wear (called by the melting furnace when a
         * melt completes). Clay molds break after ONE cast; terracotta molds
         * lose one durability point per cast and break on the third.
         */
        public void wearMoldOnce(Level level) {
                Item moldItem = this.items.get(MOLD_SLOT).getItem();
                if (moldItem == ModBlocks.CLAY_MOLD.asItem()) {
                        this.items.set(MOLD_SLOT, ItemStack.EMPTY);
                        level.playSound(null, this.worldPosition,
                                        net.minecraft.sounds.SoundEvents.GRAVEL_BREAK,
                                        net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.8F);
                } else if (moldItem == ModBlocks.TERRACOTTA_MOLD.asItem()) {
                        int damage = this.items.get(MOLD_SLOT).getOrDefault(DataComponents.DAMAGE, 0) + 1;
                        if (damage >= this.items.get(MOLD_SLOT).getMaxDamage()) {
                                this.items.set(MOLD_SLOT, ItemStack.EMPTY);
                                level.playSound(null, this.worldPosition,
                                                net.minecraft.sounds.SoundEvents.DECORATED_POT_BREAK,
                                                net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 0.9F);
                        } else {
                                this.items.get(MOLD_SLOT).set(DataComponents.DAMAGE, damage);
                        }
                } else {
                        return;
                }
                setChanged();
                syncToClients();
        }

        /** @return the part this mold prints (empty when no shaped mold is inserted). */
        public Optional<PartType> moldPart() {
                String partId = this.items.get(MOLD_SLOT).get(ModDataComponents.MOLD_PART);
                return partId == null ? Optional.empty() : PartType.byId(partId);
        }

        // ------------------------------------------------------------------ ticking (furnace transform)

        /**
         * Per-tick upgrade check: whenever a vanilla furnace sits directly
         * above, quietly turn it into the melting furnace. This makes the
         * transformation work in both placement orders (upgrade under an
         * existing furnace, or a furnace placed onto an existing upgrade).
         */
        public static void serverTick(Level level, BlockPos pos, BlockState state, MeltingUpgradeBlockEntity be) {
                MeltingFurnaceBlock.transformFromFurnace(level, pos.above());
        }

        /**
         * Called (server side) right before this block entity is discarded by a
         * block change: drop the stored mold and the finished part, and revert
         * the furnace above to a plain vanilla furnace.
         */
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
                        MeltingFurnaceBlock.revertToFurnace(this.level, pos.above());
                }
        }

        // ------------------------------------------------------------------ menu (the upgrade's own GUI)

        @Override
        public Component getDisplayName() {
                return Component.translatable("container.modular_dreams.melting_upgrade");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
                        Player player) {
                return new MeltingUpgradeMenu(containerId, inventory, (Container) this);
        }

        @Override
        public BlockPos getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
                return this.worldPosition;
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
                ItemStack removed = net.minecraft.world.ContainerHelper.removeItem(this.items, slot, amount);
                if (!removed.isEmpty()) {
                        setChanged();
                        syncToClients();
                }
                return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
                return net.minecraft.world.ContainerHelper.takeItem(this.items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
                this.items.set(slot, stack);
                if (stack.getCount() > this.getMaxStackSize()) {
                        stack.setCount(this.getMaxStackSize());
                }
                setChanged();
                syncToClients();
        }

        @Override
        public boolean stillValid(Player player) {
                return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
                this.items.clear();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
                return switch (slot) {
                        case MOLD_SLOT -> MoldBlockItem.isMold(stack);
                        default -> false; // output slot: finished parts only
                };
        }

        // ------------------------------------------------------------------ client sync

        private void syncToClients() {
                if (this.level != null && !this.level.isClientSide()) {
                        this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
        }

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
                net.minecraft.world.ContainerHelper.saveAllItems(output, this.items, true);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
                super.loadAdditional(input);
                this.items.clear();
                net.minecraft.world.ContainerHelper.loadAllItems(input, this.items);
        }
}
