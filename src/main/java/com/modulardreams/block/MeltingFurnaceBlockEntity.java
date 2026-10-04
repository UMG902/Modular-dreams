package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.menu.MeltingFurnaceMenu;
import com.modulardreams.menu.StationLogic;
import com.modulardreams.part.PartType;

/**
 * Block entity of the MELTING FURNACE - the furnace-shaped machine a vanilla
 * furnace turns into when a Melting Upgrade sits directly underneath it.
 *
 * <p>It behaves like a furnace with three differences:
 * <ul>
 *   <li>the only accepted fuels are coal/charcoal (4 smelts at 10 s each),
 *       gunpowder (6 smelts), blaze powder (8 smelts) and fire charges
 *       (16 smelts at double speed = 5 s per smelt);</li>
 *   <li>it does not smelt vanilla recipes: with a MOLD inserted into the
 *       melting upgrade below, copper/iron/gold ingots in the input slot melt
 *       into the part printed on the mold (one ingot per part);</li>
 *   <li>without a mold nothing melts - ingots simply wait.</li>
 * </ul>
 *
 * <p>There is NO output slot: the finished part goes straight into the
 * OUTPUT slot of the melting upgrade's own GUI (open the upgrade to collect
 * it). Clay molds are consumed after one cast; terracotta molds lose one of
 * their three durability points per cast and stay in the melting upgrade
 * until they break.
 */
public class MeltingFurnaceBlockEntity extends BlockEntity implements Container, ExtendedMenuProvider<BlockPos> {

        public static final int INPUT_SLOT = 0;
        public static final int FUEL_SLOT = 1;
        public static final int CONTAINER_SIZE = 2;

        /** A fuel of the melting furnace: total burn ticks + ticks per smelt. */
        public record Fuel(int burnTicks, int cookTicks) {}

        /** Coal/charcoal: 40 s burn, 4 items at 10 s (200 ticks). */
        public static final Fuel FUEL_COAL = new Fuel(800, 200);
        /** Gunpowder: 60 s burn, 6 items at 10 s. */
        public static final Fuel FUEL_GUNPOWDER = new Fuel(1200, 200);
        /** Blaze powder: 80 s burn, 8 items at 10 s. */
        public static final Fuel FUEL_BLAZE_POWDER = new Fuel(1600, 200);
        /** Fire charge: 80 s burn, 16 items at DOUBLE speed (5 s = 100 ticks). */
        public static final Fuel FUEL_FIRE_CHARGE = new Fuel(1600, 100);

        private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        private int litRemaining;
        private int litTotal;
        private int cookProgress;
        private int cookDuration = 200;

        public MeltingFurnaceBlockEntity(BlockPos pos, BlockState state) {
                super(ModBlockEntities.MELTING_FURNACE, pos, state);
        }

        // ------------------------------------------------------------------ menu provider

        @Override
        public Component getDisplayName() {
                // looks (and reads) like a furnace - the mold preview is the only difference
                return Component.translatable("container.furnace");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new MeltingFurnaceMenu(containerId, inventory, (Container) this, this.dataAccess(),
                                this::moldPreview);
        }

        @Override
        public BlockPos getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
                return this.worldPosition;
        }

        /** Live view of the furnace state for the menu's synced data slots. */
        public ContainerData dataAccess() {
                return new ContainerData() {
                        @Override
                        public int get(int index) {
                                return switch (index) {
                                        case 0 -> MeltingFurnaceBlockEntity.this.litRemaining;
                                        case 1 -> MeltingFurnaceBlockEntity.this.litTotal;
                                        case 2 -> MeltingFurnaceBlockEntity.this.cookProgress;
                                        case 3 -> MeltingFurnaceBlockEntity.this.cookDuration;
                                        default -> 0;
                                };
                        }

                        @Override
                        public void set(int index, int value) {
                                // values live server-side only; no client writes expected
                        }

                        @Override
                        public int getCount() {
                                return 4;
                        }
                };
        }

        /** The mold inserted into the Melting Upgrade below (empty when none). */
        private ItemStack moldPreview() {
                MeltingUpgradeBlockEntity upgrade = getUpgrade();
                return upgrade == null ? ItemStack.EMPTY : upgrade.getMold();
        }

        // ------------------------------------------------------------------ fuel

        /** @return the melting-furnace fuel value of this stack, or null if it is not accepted. */
        public static Fuel fuelOf(ItemStack stack) {
                if (stack.isEmpty()) {
                        return null;
                }
                Item item = stack.getItem();
                if (item == Items.COAL || item == Items.CHARCOAL) {
                        return FUEL_COAL;
                }
                if (item == Items.GUNPOWDER) {
                        return FUEL_GUNPOWDER;
                }
                if (item == Items.BLAZE_POWDER) {
                        return FUEL_BLAZE_POWDER;
                }
                if (item == Items.FIRE_CHARGE) {
                        return FUEL_FIRE_CHARGE;
                }
                return null;
        }

        // ------------------------------------------------------------------ melting

        /** @return the melting upgrade directly below, if there is one. */
        public MeltingUpgradeBlockEntity getUpgrade() {
                return this.level != null && this.level.getBlockEntity(this.worldPosition.below())
                                instanceof MeltingUpgradeBlockEntity upgrade ? upgrade : null;
        }

        /** @return the part result of one melt, or null if the current setup cannot melt. */
        public ItemStack currentResult() {
                MeltingUpgradeBlockEntity upgrade = getUpgrade();
                if (upgrade == null || this.items.get(INPUT_SLOT).isEmpty()) {
                        return null;
                }
                String partId = upgrade.getMold().get(ModDataComponents.MOLD_PART);
                ModularMaterial metal = metalOf(this.items.get(INPUT_SLOT));
                if (partId == null || metal == null) {
                        return null;
                }
                PartType part = PartType.byId(partId).orElse(null);
                if (part == null || !StationLogic.canMake(part, metal)) {
                        return null;
                }
                return StationLogic.makePart(part, metal);
        }

        /** @return whether the melting upgrade below can take one more part. */
        private boolean upgradeOutputAccepts(ItemStack result) {
                MeltingUpgradeBlockEntity upgrade = getUpgrade();
                return upgrade != null && upgrade.outputAccepts(result);
        }

        private static ModularMaterial metalOf(ItemStack stack) {
                if (stack.is(Items.COPPER_INGOT)) {
                        return ModMaterials.getOrThrow("copper");
                }
                if (stack.is(Items.IRON_INGOT)) {
                        return ModMaterials.getOrThrow("iron");
                }
                if (stack.is(Items.GOLD_INGOT)) {
                        return ModMaterials.getOrThrow("gold");
                }
                // build 21: diamond melts and casts like the metals
                if (stack.is(Items.DIAMOND)) {
                        return ModMaterials.getOrThrow("diamond");
                }
                return null;
        }

        // ------------------------------------------------------------------ ticking

        public static void serverTick(Level level, BlockPos pos, BlockState state, MeltingFurnaceBlockEntity be) {
                boolean wasLit = be.litRemaining > 0;
                boolean changed = false;

                if (be.litRemaining > 0) {
                        be.litRemaining--;
                        changed = true;
                }

                ItemStack result = be.currentResult();
                boolean canMelt = result != null && be.upgradeOutputAccepts(result);

                // light a fresh fuel unit only when there is work to do
                if (be.litRemaining <= 0 && canMelt) {
                        Fuel fuel = fuelOf(be.items.get(FUEL_SLOT));
                        if (fuel != null) {
                                be.litRemaining = fuel.burnTicks();
                                be.litTotal = fuel.burnTicks();
                                be.cookDuration = fuel.cookTicks();
                                be.items.set(FUEL_SLOT, shrinkOne(be.items.get(FUEL_SLOT)));
                                changed = true;
                        }
                }

                if (be.litRemaining > 0) {
                        be.cookProgress++;
                        if (be.cookProgress >= be.cookDuration) {
                                be.cookProgress = 0;
                                if (canMelt) {
                                        be.items.set(INPUT_SLOT, shrinkOne(be.items.get(INPUT_SLOT)));
                                        // the finished part lands in the melting upgrade's output slot
                                        be.getUpgrade().insertOutput(result);
                                        be.getUpgrade().wearMoldOnce(level);
                                }
                        }
                        changed = true;
                } else if (be.cookProgress > 0) {
                        be.cookProgress = Math.max(0, be.cookProgress - 2);
                        changed = true;
                }

                if (wasLit != (be.litRemaining > 0)) {
                        changed = true;
                        level.setBlock(pos, state.setValue(AbstractFurnaceBlock.LIT, be.litRemaining > 0), 3);
                }
                if (changed) {
                        be.setChanged();
                }
        }

        private static ItemStack shrinkOne(ItemStack stack) {
                stack.shrink(1);
                return stack.isEmpty() ? ItemStack.EMPTY : stack;
        }

        // ------------------------------------------------------------------ container

        @Override
        public int getContainerSize() {
                return CONTAINER_SIZE;
        }

        @Override
        public boolean isEmpty() {
                for (ItemStack stack : this.items) {
                        if (!stack.isEmpty()) {
                                return false;
                        }
                }
                return true;
        }

        @Override
        public ItemStack getItem(int slot) {
                return this.items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
                return net.minecraft.world.ContainerHelper.removeItem(this.items, slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
                return net.minecraft.world.ContainerHelper.takeItem(this.items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
                this.items.set(slot, stack);
                if (stack.getCount() > this.getMaxStackSize()) {
                        stack.setCount(this.getMaxStackSize());
                }
                this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
                return net.minecraft.world.Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
                this.items.clear();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
                return switch (slot) {
                        case INPUT_SLOT -> true;
                        case FUEL_SLOT -> fuelOf(stack) != null;
                        default -> false;
                };
        }

        // ------------------------------------------------------------------ save / load

        @Override
        protected void saveAdditional(ValueOutput output) {
                super.saveAdditional(output);
                net.minecraft.world.ContainerHelper.saveAllItems(output, this.items);
                output.putInt("LitRemaining", this.litRemaining);
                output.putInt("LitTotal", this.litTotal);
                output.putInt("CookProgress", this.cookProgress);
                output.putInt("CookDuration", this.cookDuration);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
                super.loadAdditional(input);
                this.items.clear();
                net.minecraft.world.ContainerHelper.loadAllItems(input, this.items);
                this.litRemaining = input.getIntOr("LitRemaining", 0);
                this.litTotal = input.getIntOr("LitTotal", 0);
                this.cookProgress = input.getIntOr("CookProgress", 0);
                this.cookDuration = input.getIntOr("CookDuration", 200);
        }
}
