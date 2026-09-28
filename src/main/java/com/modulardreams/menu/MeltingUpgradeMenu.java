package com.modulardreams.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.block.MeltingUpgradeBlockEntity;
import com.modulardreams.equipment.MoldBlockItem;

/**
 * Menu of the MELTING UPGRADE's own GUI. Two machine slots:
 *
 * <ul>
 *   <li>slot 0 = MOLD (bottom): accepts clay and terracotta molds; the
 *       melting furnace above reads the mold from here;</li>
 *   <li>slot 1 = OUTPUT (top): the finished metal part cast by the furnace;
 *       take-only.</li>
 * </ul>
 *
 * <p>A mold can also be inserted without opening this menu by right-clicking
 * the block with a mold in hand.
 */
public class MeltingUpgradeMenu extends AbstractContainerMenu {

        public static final int MOLD_SLOT = 0;
        public static final int OUTPUT_SLOT = 1;

        private static final int INVENTORY_START = 2;
        private static final int USE_ROW_SLOT_START = 29;
        private static final int USE_ROW_SLOT_END = 38;

        /** GUI slot coordinates (match textures/gui/melting_upgrade.png). */
        public static final int MOLD_X = 79;
        public static final int MOLD_Y = 54;
        public static final int OUTPUT_X = 79;
        public static final int OUTPUT_Y = 18;

        private final Container upgradeContainer;

        /** Server-side constructor used by the block entity's menu provider. */
        public MeltingUpgradeMenu(int containerId, Inventory playerInventory, Container upgradeContainer) {
                super(ModMenuTypes.MELTING_UPGRADE, containerId);
                this.upgradeContainer = upgradeContainer;

                this.addSlot(new Slot(upgradeContainer, MOLD_SLOT, MOLD_X, MOLD_Y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                                return MoldBlockItem.isMold(stack);
                        }
                });
                this.addSlot(new Slot(upgradeContainer, OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                                return false;
                        }
                });
                this.addPlayerInventory(playerInventory);
        }

        /** Client-side constructor: resolves the block entity from the opened position. */
        public MeltingUpgradeMenu(int containerId, Inventory playerInventory, net.minecraft.core.BlockPos pos) {
                this(containerId, playerInventory, resolveContainer(playerInventory.player.level(), pos));
        }

        private static Container resolveContainer(net.minecraft.world.level.Level level,
                        net.minecraft.core.BlockPos pos) {
                return level.getBlockEntity(pos) instanceof MeltingUpgradeBlockEntity upgrade
                                ? upgrade
                                : new SimpleContainer(MeltingUpgradeBlockEntity.CONTAINER_SIZE);
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

        @Override
        public boolean stillValid(Player player) {
                return this.upgradeContainer.stillValid(player);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
                ItemStack copy = ItemStack.EMPTY;
                Slot slot = this.slots.get(index);
                if (slot.hasItem()) {
                        ItemStack stack = slot.getItem();
                        copy = stack.copy();
                        if (index < INVENTORY_START) {
                                // mold or output -> player inventory
                                if (!this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_END, true)) {
                                        return ItemStack.EMPTY;
                                }
                                slot.onQuickCraft(stack, copy);
                        } else {
                                // player inventory -> mold slot only (mayPlace filters non-molds)
                                if (!this.moveItemStackTo(stack, MOLD_SLOT, MOLD_SLOT + 1, false)
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
