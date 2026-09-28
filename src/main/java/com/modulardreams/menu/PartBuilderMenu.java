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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import com.modulardreams.network.ModNetworking;
import com.modulardreams.recipe.ModRecipeTypes;
import com.modulardreams.recipe.PartBuilderRecipe;

/**
 * Part Builder menu - a functional clone of the vanilla 26.3 stonecutter menu
 * (structure and click flow byte-verified against the real client jar):
 *
 * - ONE input slot on the left, ONE result slot on the right.
 * - Putting a material in the input shows EVERY part recipe that accepts it as
 *   clickable option buttons (stonecutter-style grid, scrollable).
 * - Clicking an option selects it locally (prediction) and notifies the server
 *   via the vanilla button-click packet; the server authoritatively fills the
 *   result slot, which reaches the client through normal slot sync.
 * - Taking the result consumes the recipe's material cost.
 *
 * Differences from the vanilla stonecutter, by design: recipes are of the
 * separate {@code modular_dreams:part_builder} type (so the stonecutter can
 * never craft parts and vice versa), and recipes can cost more than one input
 * item per craft (the menu enforces the cost, the buttons gray out when the
 * input stack is too small).
 *
 * Because 26.3 no longer syncs custom recipe types to the client, the server
 * pushes the matching option list through the custom
 * {@link ModNetworking.PartBuilderOptionsPayload} whenever the input changes.
 */
public class PartBuilderMenu extends AbstractContainerMenu {

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

        /** One selectable output option (client display model + payload entry). */
        public record PartOption(ItemStack result, int cost) {}

        private final ContainerLevelAccess access;
        private final net.minecraft.world.level.Level level;
        private final Player player;
        public final Container container;
        private final ResultContainer resultContainer = new ResultContainer();
        private final Slot inputSlot;
        private final Slot resultSlot;
        private final DataSlot selectedRecipeIndex = DataSlot.standalone();
        private ItemStack lastInput = ItemStack.EMPTY;
        /** Server: matching recipes. Client: not used (options come from the payload). */
        private List<RecipeHolder<PartBuilderRecipe>> recipesForInput = List.of();
        /** Client: the option list pushed by the server for the current input. */
        private List<PartOption> clientOptions = List.of();

        public PartBuilderMenu(int containerId, Inventory playerInventory) {
                this(containerId, playerInventory, ContainerLevelAccess.NULL);
        }

        public PartBuilderMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
                super(ModMenuTypes.PART_BUILDER, containerId);
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
                                return !this.getItem().isEmpty();
                        }

                        @Override
                        public void onTake(Player player, ItemStack stack) {
                                PartBuilderMenu.this.consumeInputForTake(player, stack);
                                super.onTake(player, stack);
                        }

                        @Override
                        public int getMaxStackSize() {
                                return 64;
                        }
                });
                addStandardInventorySlots(playerInventory, 8, 84);
                this.addDataSlot(this.selectedRecipeIndex);
                this.slotsChanged(this.container);
        }

        // ------------------------------------------------------------------ recipe list

        /** @return whether an input item that some recipe accepts is present. */
        public boolean hasInputItem() {
                return this.inputSlot.hasItem();
        }

        /** @return the number of input items currently available for crafting. */
        public int inputCount() {
                ItemStack input = this.inputSlot.getItem();
                return input.isEmpty() ? 0 : input.getCount();
        }

        /** @return the current option list (client display model). */
        public List<PartOption> options() {
                return this.clientOptions;
        }

        public int getNumOptions() {
                return this.clientOptions.size();
        }

        public PartOption getOption(int index) {
                return this.clientOptions.get(index);
        }

        public int getSelectedRecipeIndex() {
                return this.selectedRecipeIndex.get();
        }

        /** Client: applies the server-pushed option list for this menu. */
        public void applyClientOptions(List<PartOption> options) {
                this.clientOptions = List.copyOf(options);
        }

        /** Server: re-enumerates the recipes matching the current input. */
        private void setupRecipeList(ItemStack stack) {
                this.selectedRecipeIndex.set(-1);
                this.resultSlot.set(ItemStack.EMPTY);
                if (stack.isEmpty()) {
                        this.recipesForInput = List.of();
                } else if (!this.level.isClientSide()
                                && this.level.recipeAccess() instanceof RecipeManager recipeManager) {
                        List<RecipeHolder<PartBuilderRecipe>> list = new ArrayList<>();
                        for (RecipeHolder<PartBuilderRecipe> holder : ModRecipeTypes.allPartRecipes(recipeManager)) {
                                if (holder.value().matches(new SingleRecipeInput(stack), this.level)) {
                                        list.add(holder);
                                }
                        }
                        this.recipesForInput = list;
                }
                pushClientOptions();
        }

        /** Server: pushes the current recipes to the open client menu. */
        private void pushClientOptions() {
                if (this.level.isClientSide() || !(this.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
                        return;
                }
                List<ModNetworking.PartOption> options = new ArrayList<>();
                for (RecipeHolder<PartBuilderRecipe> holder : this.recipesForInput) {
                        ItemStack result = holder.value().assemble(new SingleRecipeInput(this.inputSlot.getItem()));
                        options.add(new ModNetworking.PartOption(result, holder.value().cost()));
                }
                ModNetworking.sendPartBuilderOptions(serverPlayer, this.containerId, options);
        }

        /**
         * Vanilla-exact flow: the input item type drives the recipe list; only
         * a changed item type re-enumerates (so consuming input on take keeps
         * the selection).
         */
        @Override
        public void slotsChanged(Container container) {
                ItemStack current = this.inputSlot.getItem();
                if (!ItemStack.isSameItem(current, this.lastInput)) {
                        this.lastInput = current.copy();
                        this.setupRecipeList(current);
                }
                super.slotsChanged(container);
        }

        // ------------------------------------------------------------------ selection & output

        private boolean isValidRecipeIndex(int index) {
                return index >= 0 && index < this.clientOptions.size();
        }

        private boolean isValidServerRecipeIndex(int index) {
                return index >= 0 && index < this.recipesForInput.size();
        }

        /** @return the cost of the selected recipe, or -1 if the selection is invalid. */
        public int selectedCost() {
                int index = this.selectedRecipeIndex.get();
                if (index >= 0 && index < this.clientOptions.size()) {
                        return this.clientOptions.get(index).cost();
                }
                return -1;
        }

        /**
         * Vanilla-exact selection flow: applied to the LOCAL menu first (screen
         * prediction), then notified server-side via the button click packet.
         * The result preview itself is computed server-side only.
         */
        @Override
        public boolean clickMenuButton(Player player, int id) {
                if (this.selectedRecipeIndex.get() == id) {
                        return false;
                }
                if (this.level.isClientSide() ? isValidRecipeIndex(id) : isValidServerRecipeIndex(id)) {
                        this.selectedRecipeIndex.set(id);
                        if (!this.level.isClientSide()) {
                                this.setupResultSlot(id);
                        }
                }
                return true;
        }

        /** Server: fills or clears the result preview for the selected recipe. */
        private void setupResultSlot(int index) {
                if (!this.recipesForInput.isEmpty() && isValidServerRecipeIndex(index)) {
                        PartBuilderRecipe recipe = this.recipesForInput.get(index).value();
                        ItemStack input = this.inputSlot.getItem();
                        if (input.getCount() >= recipe.cost()) {
                                this.resultContainer.setItem(0, recipe.assemble(new SingleRecipeInput(input)));
                        } else {
                                this.resultContainer.setItem(0, ItemStack.EMPTY);
                        }
                } else {
                        this.resultContainer.setItem(0, ItemStack.EMPTY);
                }
                this.broadcastChanges();
        }

        /**
         * Result slot take: consumes the recipe's material cost and refreshes the
         * preview. Runs on the client as well (prediction, like the vanilla
         * stonecutter's result slot): the client derives the cost from its pushed
         * option list, the server from its authoritative recipe list.
         */
        private void consumeInputForTake(Player player, ItemStack stack) {
                int index = this.selectedRecipeIndex.get();
                int cost = -1;
                if (this.level.isClientSide()) {
                        if (index >= 0 && index < this.clientOptions.size()) {
                                cost = this.clientOptions.get(index).cost();
                        }
                } else if (isValidServerRecipeIndex(index)) {
                        cost = this.recipesForInput.get(index).value().cost();
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
                return this.access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof
                                com.modulardreams.block.ModularStationBlock
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
                                                && !this.moveItemStackTo(stack, INVENTORY_START, USE_ROW_SLOT_START,
                                                                false)) {
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

        public boolean matchesBlock() {
                return this.access.evaluate((level, pos) -> level.getBlockState(pos)
                                .is(com.modulardreams.block.ModBlocks.PART_BUILDER)).orElse(true);
        }
}
