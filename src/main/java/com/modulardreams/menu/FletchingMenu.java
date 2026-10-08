package com.modulardreams.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;

/**
 * Fletching menu: the vanilla fletching table made functional.
 *
 * <p>Rule of thumb: <b>every potion coats up to 32 arrows</b>, so
 * <b>2 potions + a full stack of 64 arrows &rarr; 64 tipped arrows</b> in one
 * take (1 potion + 32 arrows &rarr; 32 tipped arrows works too). Both potion
 * slots must hold the same potion; drinkable, splash and lingering potions
 * all work.
 *
 * <p>Slots: 0 potion A, 1 potion B, 2 arrows input, 3 tipped arrow output.
 */
public class FletchingMenu extends AbstractContainerMenu {

    public static final int POTION_A_SLOT = 0;
    public static final int POTION_B_SLOT = 1;
    public static final int ARROWS_SLOT = 2;
    public static final int RESULT_SLOT = 3;

    /** Arrows coated per single potion. Two potions cover a full stack of 64. */
    public static final int ARROWS_PER_POTION = 32;

    /** Slot coordinates in the fletching GUI. */
    public static final int POTION_A_X = 35, POTION_A_Y = 17;
    public static final int POTION_B_X = 35, POTION_B_Y = 53;
    public static final int ARROWS_X = 61, ARROWS_Y = 35;
    public static final int RESULT_X = 118, RESULT_Y = 35;

    private static final int INVENTORY_START = 4;
    private static final int USE_ROW_SLOT_START = 31;
    private static final int USE_ROW_SLOT_END = 40;

    private final Container inputs = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            FletchingMenu.this.slotsChanged(this);
        }
    };
    private final Container result = new SimpleContainer(1);
    private final ContainerLevelAccess access;

    public FletchingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public FletchingMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public FletchingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenuTypes.FLETCHING, containerId);
        this.access = access;

        addSlot(new Slot(this.inputs, POTION_A_SLOT, POTION_A_X, POTION_A_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isPotion(stack);
            }
        });
        addSlot(new Slot(this.inputs, POTION_B_SLOT, POTION_B_X, POTION_B_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isPotion(stack);
            }
        });
        addSlot(new Slot(this.inputs, ARROWS_SLOT, ARROWS_X, ARROWS_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isPlainArrow(stack);
            }
        });
        // Container index is 0 (the result container holds one stack). RESULT_SLOT
        // is the MENU slot index (3) and must not be used here: it caused
        // ArrayIndexOutOfBounds on the client when the menu contents were synced.
        addSlot(new Slot(this.result, 0, RESULT_X, RESULT_Y) {
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
                FletchingMenu.this.consumeInputsForTake(stack.getCount());
                super.onTake(player, stack);
            }
        });

        addStandardInventorySlots(playerInventory, 8, 84);
        this.slotsChanged(this.inputs);
    }

    private static boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    private static boolean isPlainArrow(ItemStack stack) {
        return stack.is(Items.ARROW) && !stack.has(DataComponents.POTION_CONTENTS);
    }

    // ------------------------------------------------------------------ result computation

    @Override
    public void slotsChanged(Container container) {
        // Always recompute: the result depends on the arrow COUNT, which the old
        // item/component comparison ignored (pulling arrows out left a stale,
        // larger result -> duplicated tipped arrows).
        recomputeResult();
        super.slotsChanged(container);
    }

    /** Recomputes the tipped arrow preview from the current inputs. */
    private void recomputeResult() {
        PotionContents contents = activePotion();
        int arrows = this.inputs.getItem(ARROWS_SLOT).getCount();
        int potions = 0;
        if (contents != null) {
            potions = 1;
            if (!this.inputs.getItem(POTION_B_SLOT).isEmpty()) {
                potions = 2;
            }
        }
        ItemStack result = ItemStack.EMPTY;
        if (contents != null && arrows > 0) {
            int coated = Math.min(arrows, potions * ARROWS_PER_POTION);
            result = new ItemStack(Items.TIPPED_ARROW, coated);
            result.set(DataComponents.POTION_CONTENTS, contents);
        }
        this.result.setItem(0, result);
        this.broadcastChanges();
    }

    /**
     * The potion that does the coating: potion A first, potion B only if it
     * is the same potion (mixed potions are rejected - predictable results
     * beat surprises). An empty A slot falls back to B alone, so a taken
     * first batch does not strand the second potion.
     */
    private PotionContents activePotion() {
        ItemStack aStack = this.inputs.getItem(POTION_A_SLOT);
        ItemStack bStack = this.inputs.getItem(POTION_B_SLOT);
        if (aStack.isEmpty()) {
            return bStack.isEmpty() ? null : coatingOf(bStack);
        }
        PotionContents a = coatingOf(aStack);
        if (a == null) {
            return null;
        }
        if (!bStack.isEmpty()) {
            PotionContents bContents = coatingOf(bStack);
            if (bContents == null || !bContents.equals(a)) {
                return null; // two different potions: no output
            }
        }
        return a;
    }

    /** Valid coating from a potion stack, or null (empty/water-like potions). */
    private static PotionContents coatingOf(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.equals(PotionContents.EMPTY)) {
            return null;
        }
        // effectless bases (water, mundane, thick, awkward) are not a coating
        if (contents.customEffects().isEmpty() && (contents.is(Potions.WATER) || contents.is(Potions.MUNDANE)
                || contents.is(Potions.THICK) || contents.is(Potions.AWKWARD))) {
            return null;
        }
        return contents;
    }

    /** Take: consumes the arrows and as many potions as were actually used. */
    private void consumeInputsForTake(int taken) {
        if (taken <= 0) {
            return;
        }
        this.inputs.removeItem(ARROWS_SLOT, taken);
        int potionsToConsume = (taken + ARROWS_PER_POTION - 1) / ARROWS_PER_POTION;
        // consume from whichever potion slots hold a potion (slot A alone empty
        // used to consume nothing at all -> free arrows from slot B)
        for (int slot = POTION_A_SLOT; slot <= POTION_B_SLOT && potionsToConsume > 0; slot++) {
            if (!this.inputs.getItem(slot).isEmpty()) {
                this.inputs.removeItem(slot, 1);
                potionsToConsume--;
            }
        }
        recomputeResult();
    }

    // ------------------------------------------------------------------ standard plumbing

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> level.getBlockState(pos).is(Blocks.FLETCHING_TABLE)
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
            } else if (isPotion(stack)) {
                if (!this.moveItemStackTo(stack, POTION_A_SLOT, POTION_B_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, ARROWS_SLOT, ARROWS_SLOT + 1, false)
                    && !this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_START, false)
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
            // for the result slot the taken amount is what was moved out of it - after
            // moveItemStackTo the remaining stack is empty or only the remainder
            slot.onTake(player, index == RESULT_SLOT
                    ? copy.copyWithCount(copy.getCount() - stack.getCount()) : stack);
        }
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index != RESULT_SLOT;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.inputs));
    }
}
