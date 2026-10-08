package com.modulardreams.menu;

import java.util.Optional;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.block.AssemblerBlock;
import com.modulardreams.component.ModDataComponents;
import com.modulardreams.equipment.LiningItem;
import com.modulardreams.equipment.ModArmorItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.equipment.ModPlatingItems;
import com.modulardreams.part.PartType;

/**
 * Assembler menu (v2): assembles tools from handle + head, assembles armor
 * from lining + plating, and converts vanilla tools into their modular
 * equivalents.
 *
 * <p>Slots: 0 handle input (ALSO the lining input), 1 head input (ALSO the
 * plating input AND the vanilla conversion slot: a vanilla tool placed here
 * converts into its modular equivalent), 2 equipment output.
 *
 * <p>Conversion takes priority when a vanilla tool sits in the head slot;
 * the modular equivalent (head of the tool's material, wooden handle -
 * netherite tools convert with a netherite handle) appears in the tool
 * output. Otherwise a valid handle+head pair assembles into its tool, or a
 * valid lining+plating pair assembles into its armor piece.
 */
public class AssemblerMenu extends AbstractContainerMenu {

    public static final int HANDLE_SLOT = 0;
    public static final int HEAD_SLOT = 1;
    public static final int TOOL_RESULT_SLOT = 2;

    /** Slot coordinates in the assembler GUI. */
    public static final int HANDLE_X = 28, HANDLE_Y = 20;
    public static final int HEAD_X = 28, HEAD_Y = 44;
    public static final int TOOL_RESULT_X = 116, TOOL_RESULT_Y = 32;

    private static final int INVENTORY_START = 3;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;

    private final ContainerLevelAccess access;
    private final net.minecraft.world.level.Level playerLevel;
    private final Container inputs;
    private final ResultContainer toolResult = new ResultContainer();
    private final Slot toolResultSlot;
    private ItemStack lastHandle = ItemStack.EMPTY;
    private ItemStack lastHead = ItemStack.EMPTY;

    public AssemblerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public AssemblerMenu(int containerId, Inventory playerInventory, net.minecraft.core.BlockPos pos) {
        this(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public AssemblerMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenuTypes.ASSEMBLER, containerId);
        this.access = access;
        this.playerLevel = playerInventory.player.level();
        this.inputs = new MenuNotifyingContainer(this, 2);

        addSlot(new Slot(this.inputs, HANDLE_SLOT, HANDLE_X, HANDLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                boolean isHandle = ModPartItems.resolve(stack).map(p -> p.part() == PartType.HANDLE).orElse(false);
                boolean isLining = ModArmorItems.isLining(stack);
                return isHandle || isLining;
            }
        });
        addSlot(new Slot(this.inputs, HEAD_SLOT, HEAD_X, HEAD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                boolean isHead = ModPartItems.resolve(stack).map(p -> p.part().isHead()).orElse(false);
                boolean isPlating = ModPlatingItems.resolve(stack).isPresent();
                return isHead || isPlating || StationLogic.conversionOf(stack).isPresent();
            }
        });

        this.toolResultSlot = addSlot(new Slot(this.toolResult, TOOL_RESULT_SLOT, TOOL_RESULT_X, TOOL_RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !this.getItem().isEmpty();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                AssemblerMenu.this.consumeInputsForTake(player, stack);
                super.onTake(player, stack);
            }
        });

        addStandardInventorySlots(playerInventory, 8, 84);
        this.slotsChanged(this.inputs);
    }

    // ------------------------------------------------------------------ result computation

    @Override
    public void slotsChanged(Container container) {
        ItemStack handle = this.inputs.getItem(HANDLE_SLOT);
        ItemStack head = this.inputs.getItem(HEAD_SLOT);
        if (!ItemStack.isSameItemSameComponents(handle, this.lastHandle)
                || !ItemStack.isSameItemSameComponents(head, this.lastHead)) {
            this.lastHandle = handle.copy();
            this.lastHead = head.copy();
            if (!this.level().isClientSide()) {
                recomputeResult();
            }
        }
        super.slotsChanged(container);
    }

    private net.minecraft.world.level.Level level() {
        // the player's level is always available (ContainerLevelAccess.NULL has none)
        return this.access.evaluate((level, pos) -> level).orElse(this.playerLevel);
    }

    /** Server: recomputes the equipment output from the current inputs. */
    private void recomputeResult() {
        ItemStack head = this.inputs.getItem(HEAD_SLOT);
        ItemStack result = ItemStack.EMPTY;
        // a vanilla tool in the head slot converts (takes priority)
        Optional<StationLogic.Conversion> conv = StationLogic.conversionOf(head);
        if (conv.isPresent()) {
            result = StationLogic.convert(conv.get(), head);
        } else {
            var handle = ModPartItems.resolve(this.inputs.getItem(HANDLE_SLOT));
            var headPart = ModPartItems.resolve(head);
            if (handle.isPresent() && headPart.isPresent()
                    && handle.get().part() == PartType.HANDLE && headPart.get().part().isHead()) {
                result = StationLogic.assemble(
                        toolTypeOf(headPart.get().part()),
                        handle.get().material().id(),
                        headPart.get().material().id());
            } else {
                // armor: lining in slot 0 + plating in slot 1
                ItemStack liningStack = this.inputs.getItem(HANDLE_SLOT);
                var plating = ModPlatingItems.resolve(head);
                if (ModArmorItems.isLining(liningStack) && plating.isPresent()) {
                    result = StationLogic.assembleArmor(plating.get().plating(),
                            plating.get().material().id(),
                            ((LiningItem) liningStack.getItem()).materialId());
                }
            }
        }
        this.toolResult.setItem(0, result);
        this.broadcastChanges();
    }

    private static com.modulardreams.equipment.ModularToolType toolTypeOf(PartType headPart) {
        String toolId = headPart.id.substring(0, headPart.id.length() - "_head".length());
        return com.modulardreams.equipment.ModularToolType.byId(toolId).orElseThrow();
    }

    /** Equipment output take: consumes the assembly parts or the converted vanilla tool. */
    private void consumeInputsForTake(Player player, ItemStack takenEquipment) {
        ItemStack head = this.inputs.getItem(HEAD_SLOT);
        boolean armorTake = ModArmorItems.isPiece(takenEquipment);
        if (armorTake) {
            this.inputs.removeItem(HANDLE_SLOT, 1); // the lining
            this.inputs.removeItem(HEAD_SLOT, 1); // the plating
        } else if (StationLogic.conversionOf(head).isPresent()) {
            this.inputs.removeItem(HEAD_SLOT, 1);
        } else {
            this.inputs.removeItem(HANDLE_SLOT, 1);
            this.inputs.removeItem(HEAD_SLOT, 1);
        }
        if (!player.level().isClientSide()) {
            recomputeResult();
        }
    }

    // ------------------------------------------------------------------ standard plumbing

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof AssemblerBlock
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
            if (index == TOOL_RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, copy);
            } else if (index < INVENTORY_START) {
                // station inputs -> inventory
                if (!this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // inventory -> whichever station input accepts it
                if (!this.moveItemStackTo(stack, HANDLE_SLOT, HEAD_SLOT + 1, false)
                        && !this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_START, false)
                        && !this.moveItemStackTo(stack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
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

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot != this.toolResultSlot;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.inputs));
    }

    /** @return whether the tool output is ready for the modular tooltip hint. */
    public boolean hasModularResult() {
        return this.toolResultSlot.getItem().has(ModDataComponents.MODULAR_DATA);
    }
}
