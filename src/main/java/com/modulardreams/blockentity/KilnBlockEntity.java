package com.modulardreams.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.modulardreams.menu.KilnMenu;
import com.modulardreams.recipe.ModRecipes;

/**
 * Kiln block entity. Runs the vanilla furnace engine wholesale: burning,
 * cooking timers, XP tracking, hopper automation and lit-state handling all
 * come from {@link AbstractFurnaceBlockEntity}.
 *
 * <p>Two behavioural overrides on top of the engine:
 * <ul>
 *   <li>{@link #quickCheck} is swapped for the dynamic kiln filter
 *       ({@link ModRecipes#KILN_CHECK}): the kiln smelts whatever vanilla
 *       smelting covers minus everything the smoker or the blast furnace
 *       also claims - no recipe entries, modded smelting included for free.</li>
 *   <li>{@link #getSpeedMultiplier} returns {@code 2.0}, halving the cook
 *       time of every underlying smelting recipe (200t -&gt; 100t), matching
 *       the speed tiers of the smoker and blast furnace.</li>
 * </ul>
 */
public class KilnBlockEntity extends AbstractFurnaceBlockEntity {

    public KilnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.KILN, pos, state, RecipeType.SMELTING);
        this.quickCheck = ModRecipes.KILN_CHECK;
    }

    /** The kiln fires twice as fast as a regular furnace. */
    @Override
    protected float getSpeedMultiplier(ServerLevel level, ItemStack stack) {
        return 2.0F;
    }

    /**
     * Keep non-kilnable items out of the input slot entirely, so hoppers
     * don't jam on smoker food or ores. Players face the same rule through
     * the menu slot.
     */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == KilnMenu.INPUT_SLOT && !ModRecipes.isKilnIngredient(this.getLevel(), stack)) {
            return false;
        }
        return super.canPlaceItem(slot, stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.modular_dreams.kiln");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new KilnMenu(containerId, inventory, this, this.dataAccess);
    }
}
