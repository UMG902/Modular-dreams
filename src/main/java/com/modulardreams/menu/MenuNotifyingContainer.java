package com.modulardreams.menu;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * A {@link SimpleContainer} that tells its menu about every change, exactly
 * like vanilla's TransientCraftingContainer (the crafting-grid container).
 *
 * A plain SimpleContainer NEVER calls {@code menu.slotsChanged} - vanilla menus
 * that use one only work because their slots are driven externally. Our station
 * menus evaluate their outputs inside {@code slotsChanged}, so with a plain
 * SimpleContainer the evaluation never ran.
 */
public class MenuNotifyingContainer extends SimpleContainer {

    private final AbstractContainerMenu menu;

    public MenuNotifyingContainer(AbstractContainerMenu menu, int size) {
        super(size);
        this.menu = menu;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        super.setItem(index, stack);
        this.menu.slotsChanged(this);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack removed = super.removeItem(index, count);
        this.menu.slotsChanged(this);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack removed = super.removeItemNoUpdate(index);
        this.menu.slotsChanged(this);
        return removed;
    }
}
