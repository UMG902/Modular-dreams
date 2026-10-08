package com.modulardreams.menu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.block.PartPickerBlock;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.network.ModNetworking;

/**
 * Part Picker menu - a functional clone of the vanilla 26.3 stonecutter menu
 * (structure and click flow byte-verified against the real client jar by the
 * overhaul reference):
 *
 * - ONE input slot on the left, ONE result slot on the right.
 * - Putting a material in the input shows every part that material can be
 *   shaped into at THIS station tier, as clickable option buttons
 *   (stonecutter-style grid): the handle, one head per tool type, and the
 *   4 armor platings where the material has them.
 * - Clicking an option selects it locally (prediction) and notifies the
 *   server via the vanilla button-click packet; the server authoritatively
 *   fills the result slot, which reaches the client through normal slot sync.
 * - Taking the result consumes the part's material cost.
 *
 * Tier gating is cumulative: a tier-N station shapes every material up to
 * tier N+1 (wood picker: wood + stone; diamond picker: everything incl.
 * netherite).
 */
public class PartPickerMenu extends AbstractContainerMenu {

    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;

    private static final int INVENTORY_START = 2;
    private static final int USE_ROW_SLOT_START = 29;
    private static final int USE_ROW_SLOT_END = 38;

    /** UI slot coordinates - exactly the vanilla stonecutter layout. */
    public static final int INPUT_X = 20;
    public static final int INPUT_Y = 33;
    public static final int RESULT_X = 143;
    public static final int RESULT_Y = 33;

    /** One selectable output option (client display + payload entry). */
    public record PartOption(ItemStack result, int cost) {}

    private final ContainerLevelAccess access;
    private final net.minecraft.world.level.Level level;
    private final Player player;
    public final Container container;
    private final ResultContainer resultContainer = new ResultContainer();
    private final Slot inputSlot;
    private final Slot resultSlot;
    private final DataSlot selectedOptionIndex = DataSlot.standalone();
    private ItemStack lastInput = ItemStack.EMPTY;
    /** Server: options shapeable from the current input. Client: not used (options come from the payload). */
    private List<StationLogic.Option> serverOptions = List.of();
    private ModularMaterial serverMaterial;
    /** Client: the option list pushed by the server for the current input. */
    private List<PartOption> clientOptions = List.of();

    public PartPickerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public PartPickerMenu(int containerId, Inventory playerInventory, net.minecraft.core.BlockPos pos) {
        this(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public PartPickerMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenuTypes.PART_PICKER, containerId);
        this.access = access;
        this.level = playerInventory.player.level();
        this.player = playerInventory.player;
        this.container = new MenuNotifyingContainer(this, 1);

        this.inputSlot = addSlot(new Slot(this.container, 0, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return StationLogic.materialOf(stack) != null;
            }
        });
        // Vanilla-exact: the result Slot carries container index 1; the
        // ResultContainer routes every access to its single entry.
        this.resultSlot = addSlot(new Slot(this.resultContainer, RESULT_SLOT, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                if (this.getItem().isEmpty()) {
                    return false;
                }
                // never hand out a part the input can no longer pay for
                int cost = PartPickerMenu.this.selectedCost();
                return cost > 0 && PartPickerMenu.this.inputCount() >= cost;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                PartPickerMenu.this.consumeInputForTake(player, stack);
                super.onTake(player, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 64;
            }
        });
        addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlot(this.selectedOptionIndex);
        this.slotsChanged(this.container);
    }

    // ------------------------------------------------------------------ option list

    /** @return whether an input item that some part accepts is present. */
    public boolean hasInputItem() {
        return this.inputSlot.hasItem();
    }

    /** @return the number of input items currently available for crafting. */
    public int inputCount() {
        ItemStack input = this.inputSlot.getItem();
        return input.isEmpty() ? 0 : input.getCount();
    }

    /** @return the current option list (client display). */
    public List<PartOption> options() {
        return this.clientOptions;
    }

    public int getNumOptions() {
        return this.clientOptions.size();
    }

    public PartOption getOption(int index) {
        return this.clientOptions.get(index);
    }

    public int getSelectedOptionIndex() {
        return this.selectedOptionIndex.get();
    }

    /** Client: applies the server-pushed option list for this menu. */
    public void applyClientOptions(List<PartOption> options) {
        this.clientOptions = List.copyOf(options);
    }

    /** Server: re-enumerates the options shapeable from the current input at this station's tier. */
    private void setupOptionList(ItemStack stack) {
        this.selectedOptionIndex.set(-1);
        this.resultContainer.setItem(0, ItemStack.EMPTY);
        this.serverOptions = List.of();
        this.serverMaterial = null;
        if (!stack.isEmpty() && !this.level.isClientSide()) {
            ModularMaterial material = StationLogic.materialOf(stack);
            int stationTier = access.evaluate((lvl, pos) -> lvl.getBlockState(pos).getBlock()
                    instanceof PartPickerBlock picker ? picker.stationTier() : 0).orElse(0);
            if (material != null && StationLogic.canShape(stationTier, material)) {
                this.serverOptions = StationLogic.optionsFor(material, stationTier);
                this.serverMaterial = material;
            }
        }
        pushClientOptions();
    }

    /** Server: pushes the current option list to the open client menu. */
    private void pushClientOptions() {
        if (this.level.isClientSide() || !(this.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        List<ModNetworking.PartOption> options = new ArrayList<>();
        for (StationLogic.Option option : this.serverOptions) {
            options.add(new ModNetworking.PartOption(option.prototype(), option.cost()));
        }
        ModNetworking.sendPartPickerOptions(serverPlayer, this.containerId, options);
    }

    /**
     * Vanilla-exact flow: the input item type drives the option list; only a
     * changed item type re-enumerates (so consuming input on take keeps the
     * selection).
     */
    @Override
    public void slotsChanged(Container container) {
        ItemStack current = this.inputSlot.getItem();
        if (!ItemStack.isSameItem(current, this.lastInput)) {
            this.lastInput = current.copy();
            this.setupOptionList(current);
        } else if (!this.level.isClientSide()
                && isValidServerOptionIndex(this.selectedOptionIndex.get())) {
            // same material but a different amount: the preview must be re-validated,
            // otherwise a part could be taken after pulling items out of the input
            this.setupResultSlot(this.selectedOptionIndex.get());
        }
        super.slotsChanged(container);
    }

    // ------------------------------------------------------------------ selection & output

    private boolean isValidClientOptionIndex(int index) {
        return index >= 0 && index < this.clientOptions.size();
    }

    private boolean isValidServerOptionIndex(int index) {
        return index >= 0 && index < this.serverOptions.size();
    }

    /** @return the cost of the selected option, or -1 if the selection is invalid. */
    public int selectedCost() {
        int index = this.selectedOptionIndex.get();
        if (this.level.isClientSide()) {
            return isValidClientOptionIndex(index) ? this.clientOptions.get(index).cost() : -1;
        }
        return isValidServerOptionIndex(index) ? this.serverOptions.get(index).cost() : -1;
    }

    /**
     * Vanilla-exact selection flow: applied to the LOCAL menu first (screen
     * prediction), then notified server-side via the button click packet.
     * The result preview itself is computed server-side only.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.selectedOptionIndex.get() == id) {
            return false;
        }
        if (this.level.isClientSide() ? isValidClientOptionIndex(id) : isValidServerOptionIndex(id)) {
            this.selectedOptionIndex.set(id);
            if (!this.level.isClientSide()) {
                this.setupResultSlot(id);
            }
        }
        return true;
    }

    /** Server: fills or clears the result preview for the selected option. */
    private void setupResultSlot(int index) {
        if (isValidServerOptionIndex(index)) {
            StationLogic.Option option = this.serverOptions.get(index);
            ItemStack input = this.inputSlot.getItem();
            if (input.getCount() >= option.cost()) {
                this.resultContainer.setItem(0, option.prototype().copy());
            } else {
                this.resultContainer.setItem(0, ItemStack.EMPTY);
            }
        } else {
            this.resultContainer.setItem(0, ItemStack.EMPTY);
        }
        this.broadcastChanges();
    }

    /**
     * Result slot take: consumes the part's material cost and refreshes the
     * preview. Runs on the client as well (prediction, like the vanilla
     * stonecutter's result slot): the client derives the cost from its pushed
     * option list, the server from its authoritative option list.
     */
    private void consumeInputForTake(Player player, ItemStack stack) {
        int index = this.selectedOptionIndex.get();
        int cost = -1;
        if (this.level.isClientSide()) {
            if (isValidClientOptionIndex(index)) {
                cost = this.clientOptions.get(index).cost();
            }
        } else if (isValidServerOptionIndex(index)) {
            cost = this.serverOptions.get(index).cost();
        }
        if (cost > 0) {
            ItemStack input = this.inputSlot.getItem();
            if (!input.isEmpty() && input.getCount() >= cost) {
                this.inputSlot.remove(cost);
            }
        }
        if (!this.level.isClientSide()) {
            this.setupResultSlot(index);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof PartPickerBlock
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
            } else if (index == INPUT_SLOT) {
                if (!this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= INVENTORY_START && index < USE_ROW_SLOT_START) {
                if (!this.moveItemStackTo(stack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)
                        && !this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_START, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_START, false)
                    && !this.moveItemStackTo(stack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
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
        return slot != this.resultSlot;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.container));
    }
}
