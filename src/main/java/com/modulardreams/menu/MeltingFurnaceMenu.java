package com.modulardreams.menu;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.modulardreams.block.ModBlocks;
import com.modulardreams.block.MeltingFurnaceBlockEntity;
import com.modulardreams.block.MeltingUpgradeBlockEntity;

/**
 * Menu of the MELTING FURNACE. Looks like a vanilla furnace menu (input,
 * fuel) but has NO output slot: the finished part goes to the melting
 * upgrade's own GUI. The screen draws the mold that is inserted into the
 * Melting Upgrade below at the vanilla output slot position (nothing when
 * there is no mold) so the setup can be read at a glance.
 *
 * <p>Fuel slot only accepts the melting fuel table (coal/charcoal, gunpowder,
 * blaze powder, fire charges).
 */
public class MeltingFurnaceMenu extends AbstractContainerMenu {

        public static final int INPUT_SLOT = 0;
        public static final int FUEL_SLOT = 1;

        private static final int INVENTORY_START = 2;
        private static final int USE_ROW_SLOT_START = 29;
        private static final int USE_ROW_SLOT_END = 38;

        /** Slot coordinates copied from the vanilla furnace GUI. */
        public static final int INPUT_X = 56;
        public static final int INPUT_Y = 17;
        public static final int FUEL_X = 56;
        public static final int FUEL_Y = 53;
        /** Where the mold preview is drawn: exactly the vanilla furnace
         * RESULT slot position (116, 35 - verified from the vanilla bytecode),
         * so the mold sits inside the baked output cell like a real output. */
        public static final int MOLD_PREVIEW_X = 116;
        public static final int MOLD_PREVIEW_Y = 35;

        private final Container furnaceContainer;
        private final ContainerData furnaceData;
        private final Supplier<ItemStack> moldPreview;

        /** Server-side constructor used by the block entity's menu provider. */
        public MeltingFurnaceMenu(int containerId, Inventory playerInventory, Container furnaceContainer,
                        ContainerData data, Supplier<ItemStack> moldPreview) {
                super(ModMenuTypes.MELTING_FURNACE, containerId);
                this.furnaceContainer = furnaceContainer;
                this.furnaceData = data;
                this.moldPreview = moldPreview;

                this.addSlot(new Slot(furnaceContainer, INPUT_SLOT, INPUT_X, INPUT_Y));
                this.addSlot(new Slot(furnaceContainer, FUEL_SLOT, FUEL_X, FUEL_Y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                                return MeltingFurnaceBlockEntity.fuelOf(stack) != null;
                        }
                });
                this.addPlayerInventory(playerInventory);
                this.addDataSlots(data);
        }

        /** Client-side constructor: resolves the block entity from the opened position. */
        public MeltingFurnaceMenu(int containerId, Inventory playerInventory, BlockPos pos) {
                this(containerId, playerInventory,
                                resolveContainer(playerInventory.player.level(), pos),
                                new SimpleContainerData(4),
                                moldPreviewOf(playerInventory.player.level(), pos));
        }

        private static Container resolveContainer(Level level, BlockPos pos) {
                return level.getBlockEntity(pos) instanceof MeltingFurnaceBlockEntity furnace
                                ? furnace
                                : new SimpleContainer(MeltingFurnaceBlockEntity.CONTAINER_SIZE);
        }

        /** The mold inserted into the Melting Upgrade below (client copy is kept in sync). */
        private static Supplier<ItemStack> moldPreviewOf(Level level, BlockPos pos) {
                return level.getBlockEntity(pos.below()) instanceof MeltingUpgradeBlockEntity upgrade
                                ? upgrade::getMold
                                : () -> ItemStack.EMPTY;
        }

        private void addPlayerInventory(Inventory playerInventory) {
                for (int row = 0; row < 3; row++) {
                        for (int col = 0; col < 9; col++) {
                                addSlot(new Slot(playerInventory, 9 + col + row * 9, 8 + col * 18, 84 + row * 18));
                        }
                }
                for (int col = 0; col < 9; col++) {
                        addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
                }
        }

        // ------------------------------------------------------------------ furnace-style getters (screen)

        public boolean isLit() {
                return this.furnaceData.get(0) > 0;
        }

        public float getLitProgress() {
                int total = this.furnaceData.get(1);
                return total == 0 ? 0.0F : (float) this.furnaceData.get(0) / total;
        }

        public float getBurnProgress() {
                int total = this.furnaceData.get(3);
                return total == 0 ? 0.0F : (float) this.furnaceData.get(2) / total;
        }

        /** @return the mold currently inserted into the Melting Upgrade below (may be empty). */
        public ItemStack getMoldPreview() {
                return this.moldPreview.get();
        }

        // ------------------------------------------------------------------ standard menu plumbing

        @Override
        public boolean stillValid(Player player) {
                return this.furnaceContainer.stillValid(player);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
                ItemStack copy = ItemStack.EMPTY;
                Slot slot = this.slots.get(index);
                if (slot.hasItem()) {
                        ItemStack stack = slot.getItem();
                        copy = stack.copy();
                        if (index < INVENTORY_START) {
                                if (!this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_END, true)) {
                                        return ItemStack.EMPTY;
                                }
                        } else {
                                // fuel first (mayPlace filters non-fuels), then the melt input
                                if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)
                                                && !this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)
                                                && !this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_START,
                                                                false)) {
                                        return ItemStack.EMPTY;
                                }
                        }
                        if (stack.isEmpty()) {
                                slot.setByPlayer(ItemStack.EMPTY);
                        } else {
                                slot.setChanged();
                        }
                        if (stack.getCount() == copy.getCount()) {
                                return ItemStack.EMPTY;
                        }
                        slot.onTake(player, stack);
                }
                return copy;
        }
}
