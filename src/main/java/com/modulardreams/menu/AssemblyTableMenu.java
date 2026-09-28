package com.modulardreams.menu;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.block.AssemblyTableBlockEntity;
import com.modulardreams.block.ModBlocks;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.menu.StationLogic.Assembly;
import com.modulardreams.part.PartType;

/**
 * Assembly Table menu: THREE input slots on the left and ONE result slot on
 * the right. The slots are fixed by role and read top to bottom as
 * slot 0 = head, slot 1 = binding, slot 2 = handle (the assembly ORDER is
 * still handle -> head -> binding; the binding simply renders on top of the
 * head, with the handle as the lowest layer). A tool is assembled when all
 * three hold the matching parts of the SAME tool type, each in its own slot.
 *
 * <p>The input slots are backed by the block's {@link AssemblyTableBlockEntity}:
 * parts STAY in the table when the menu closes.
 */
public class AssemblyTableMenu extends AbstractContainerMenu {

        public static final int HEAD_SLOT = 0;
        public static final int BINDING_SLOT = 1;
        public static final int HANDLE_SLOT = 2;
        public static final int RESULT_SLOT = 3;

        private static final int INVENTORY_START = 4;
        private static final int USE_ROW_SLOT_START = 31;
        private static final int USE_ROW_SLOT_END = 40;

        /** UI slot coordinates: three input squares in a vertical column on the
         * left (x=44, y=18/42/66 - top to bottom: head, binding, handle), the
         * big result square on the right (item centered at 134,42). */
        public static final int[] INPUT_X = { 44, 44, 44 };
        public static final int[] INPUT_Y = { 18, 42, 66 };
        public static final int RESULT_X = 134;
        public static final int RESULT_Y = 42;

        private final ContainerLevelAccess access;
        public final Container container;
        /** True when the menu owns a transient container (no block entity behind it). */
        private final boolean ownsContainer;
        private final Container resultContainer = new SimpleContainer(1);
        private Assembly currentResult;

        /** Server-side constructor: opened by the block with its persistent container. */
        public AssemblyTableMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access,
                        Container stationContainer) {
                super(ModMenuTypes.ASSEMBLY_TABLE, containerId);
                this.access = access;
                if (stationContainer != null) {
                        this.container = stationContainer;
                        this.ownsContainer = false;
                        if (stationContainer instanceof AssemblyTableBlockEntity blockEntity) {
                                blockEntity.changeListener = this::recomputeResult;
                        }
                } else {
                        // notifying container: re-evaluates the output whenever a slot changes
                        this.container = new MenuNotifyingContainer(this, 3);
                        this.ownsContainer = true;
                }

                for (int i = 0; i < 3; i++) {
                        final int slotIndex = i;
                        addSlot(new Slot(this.container, i, INPUT_X[i], INPUT_Y[i]) {
                                @Override
                                public boolean mayPlace(ItemStack stack) {
                                        // each slot accepts only its own role:
                                        // 0 = head, 1 = binding, 2 = handle
                                        PartType.Role role = switch (slotIndex) {
                                                case HEAD_SLOT -> PartType.Role.HEAD;
                                                case BINDING_SLOT -> PartType.Role.BINDING;
                                                default -> PartType.Role.HANDLE;
                                        };
                                        return ModPartItems.resolve(stack)
                                                        .map(identity -> identity.part().role == role)
                                                        .orElse(false);
                                }
                        });
                }
                addSlot(new Slot(this.resultContainer, 0, RESULT_X, RESULT_Y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                                return false;
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                                return currentResult != null;
                        }

                        @Override
                        public void onTake(Player player, ItemStack stack) {
                                consumeInputs();
                                super.onTake(player, stack);
                        }

                        @Override
                        public int getMaxStackSize() {
                                return 64;
                        }
                });
                addPlayerInventory(playerInventory);
        }

        /** Client-side constructor: resolves the block entity from the opened position. */
        public AssemblyTableMenu(int containerId, Inventory playerInventory, BlockPos pos) {
                this(containerId, playerInventory,
                                ContainerLevelAccess.create(playerInventory.player.level(), pos),
                                playerInventory.player.level().getBlockEntity(pos)
                                                instanceof AssemblyTableBlockEntity blockEntity
                                                                ? (Container) blockEntity
                                                                : null);
        }

        /** Fallback for non-block contexts (creative preview, tests). */
        public AssemblyTableMenu(int containerId, Inventory playerInventory) {
                this(containerId, playerInventory, ContainerLevelAccess.NULL, null);
        }

        private void addPlayerInventory(Inventory playerInventory) {
                // vanilla-standard machine-GUI layout (same rows as the furnace)
                for (int row = 0; row < 3; row++) {
                        for (int col = 0; col < 9; col++) {
                                addSlot(new Slot(playerInventory, 9 + col + row * 9, 8 + col * 18, 84 + row * 18));
                        }
                }
                for (int col = 0; col < 9; col++) {
                        addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
                }
        }

        private void recomputeResult() {
                Optional<Assembly> result = StationLogic.classifyAssembly(
                                this.container.getItem(HANDLE_SLOT),
                                this.container.getItem(HEAD_SLOT),
                                this.container.getItem(BINDING_SLOT));
                this.currentResult = result.orElse(null);
                this.resultContainer.setItem(0, result.map(StationLogic::assemble).orElse(ItemStack.EMPTY));
                this.broadcastChanges();
        }

        private void consumeInputs() {
                if (this.currentResult == null) {
                        return;
                }
                // every part is consumed (parts are single items)
                for (int i = 0; i < 3; i++) {
                        ItemStack stack = this.container.getItem(i);
                        if (!stack.isEmpty()) {
                                stack.shrink(1);
                                this.container.setItem(i, stack);
                        }
                }
        }

        @Override
        public void slotsChanged(Container container) {
                recomputeResult();
                super.slotsChanged(container);
        }

        @Override
        public boolean stillValid(Player player) {
                return this.access.evaluate((level, pos) -> level.getBlockState(pos).is(ModBlocks.ASSEMBLY_TABLE)
                                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0)
                                .orElse(true);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
                ItemStack copy = ItemStack.EMPTY;
                Slot slot = this.slots.get(index);
                if (slot.hasItem()) {
                        ItemStack stack = slot.getItem();
                        copy = stack.copy();
                        if (index == RESULT_SLOT) {
                                if (!this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_END, true)) {
                                        return ItemStack.EMPTY;
                                }
                                slot.onQuickCraft(stack, copy);
                        } else if (index < INVENTORY_START) {
                                if (!this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_END, true)) {
                                        return ItemStack.EMPTY;
                                }
                        } else if (index >= INVENTORY_START && index < USE_ROW_SLOT_START) {
                                if (!this.moveItemStackTo(stack, 0, 3, false)
                                                && !this.moveItemStackTo(stack, USE_ROW_SLOT_START, USE_ROW_SLOT_END,
                                                                false)) {
                                        return ItemStack.EMPTY;
                                }
                        } else if (!this.moveItemStackTo(stack, 0, 3, false)
                                        && !this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_START, false)) {
                                return ItemStack.EMPTY;
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

        @Override
        public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
                return slot.container != this.resultContainer;
        }

        @Override
        public void removed(Player player) {
                // the input parts live in the table - they STAY there when the GUI
                // closes; only the transient preview result is returned/dropped
                if (this.ownsContainer) {
                        clearContainer(player, this.container);
                } else if (this.container instanceof AssemblyTableBlockEntity blockEntity) {
                        blockEntity.changeListener = null;
                }
                clearContainer(player, this.resultContainer);
                super.removed(player);
        }
}
