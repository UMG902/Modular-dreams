package com.modulardreams.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import com.modulardreams.blockentity.KilnBlockEntity;
import com.modulardreams.recipe.ModRecipes;

/**
 * Kiln menu: input / fuel / result, exactly the furnace layout. The input
 * slot only accepts kiln-eligible items (smeltable, claimed by neither the
 * smoker nor the blast furnace). The menu is opened from the kiln block
 * entity (server) or created with a dummy container that is immediately
 * sync-filled (client, with the client's synced recipe manager for slot
 * validation).
 */
public class KilnMenu extends AbstractContainerMenu {

    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int RESULT_SLOT = 2;

    /** Slot coordinates in the kiln GUI (mirrors the furnace layout). */
    public static final int INPUT_X = 56, INPUT_Y = 17;
    public static final int FUEL_X = 56, FUEL_Y = 53;
    public static final int RESULT_X = 116, RESULT_Y = 35;

    private static final int INVENTORY_START = 3;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;

    private final Container container;
    private final ContainerData data;
    private final Level level;

    /** Client: dummy container, filled by menu sync; client recipes validate the input slot. */
    public KilnMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(4),
                playerInventory.player.level());
    }

    /** Server: real kiln block entity. */
    public KilnMenu(int containerId, Inventory playerInventory, Container kilnContainer, ContainerData data) {
        this(containerId, playerInventory, kilnContainer, data,
                kilnContainer instanceof KilnBlockEntity kiln ? kiln.getLevel() : null);
    }

    private KilnMenu(int containerId, Inventory playerInventory, Container container,
            ContainerData data, Level level) {
        super(ModMenuTypes.KILN, containerId);
        this.container = container;
        this.data = data;
        this.level = level;
        addSlot(new Slot(container, INPUT_SLOT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return KilnMenu.this.isKilnIngredient(stack);
            }
        });
        addSlot(new Slot(container, FUEL_SLOT, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return KilnMenu.this.isFuel(stack);
            }
        });
        addSlot(new FurnaceResultSlot(playerInventory.player, container, RESULT_SLOT, RESULT_X, RESULT_Y));
        addStandardInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    /** Whether this stack may cook in a kiln (smelting minus smoker/blast furnace domains). */
    private boolean isKilnIngredient(ItemStack stack) {
        return ModRecipes.isKilnIngredient(this.level, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
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
                // kiln slots -> inventory
                if (!this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.isFuel(stack)) {
                if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)
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
            slot.onTake(player, stack);
        }
        return copy;
    }

    /** Whether the stack burns (mirror of the vanilla fuel check). */
    /**
     * Server: the block entity decides. Client: its container is a dummy that accepts
     * everything, so predict with the 26.3 fuel component (plus empty buckets, which a
     * furnace fuel slot accepts so the bucket can be taken back out).
     */
    private boolean isFuel(ItemStack stack) {
        if (this.container instanceof KilnBlockEntity) {
            return this.container.canPlaceItem(FUEL_SLOT, stack);
        }
        return stack.has(net.minecraft.core.component.DataComponents.COOKING_FUEL)
                || stack.is(net.minecraft.world.item.Items.BUCKET);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index != RESULT_SLOT;
    }

    /** @return whether the kiln is currently burning (flame icon on screen). */
    public boolean isLit() {
        return this.data.get(0) > 0;
    }

    public int getBurnProgress(int displayWidth) {
        int litTotalTime = this.data.get(1);
        if (litTotalTime == 0) {
            litTotalTime = 200;
        }
        int lit = this.data.get(0);
        return Math.max(0, Math.min(displayWidth, lit * displayWidth / litTotalTime));
    }

    public int getCookProgress(int displayWidth) {
        int cookingTotalTime = this.data.get(3);
        if (cookingTotalTime == 0) {
            cookingTotalTime = 1;
        }
        int cookingTimer = this.data.get(2);
        return Math.max(0, Math.min(displayWidth, cookingTimer * displayWidth / cookingTotalTime));
    }
}
