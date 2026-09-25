package com.modulardreams.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.equipment.ModDataHooks;
import com.modulardreams.modifier.Modifier;
import com.modulardreams.stats.StatsEngine;

/**
 * Applies Tinkers'-style modifiers: combine an assembled item with the
 * modifier's cost items in a crafting grid.
 *
 * Modifier recipes never collide with the repair recipe, because every cost
 * requires 2+ items (cost items double as repair materials in some cases).
 */
public class ModifierRecipe extends CustomRecipe {

        public static final ModifierRecipe INSTANCE = new ModifierRecipe();
        public static final MapCodec<ModifierRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, ModifierRecipe> STREAM_CODEC = StreamCodec
                        .unit(INSTANCE);
        public static final RecipeSerializer<ModifierRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        private ModifierRecipe() {
                super();
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
                return findApplication(input).isPresent();
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
                Optional<Application> application = findApplication(input);
                if (application.isEmpty()) {
                        return ItemStack.EMPTY;
                }
                Application app = application.get();
                ItemStack result = app.stack().copy();
                ModularData newData = app.data().withModifier(app.modifier().id(), app.newLevel());
                result.set(ModDataComponents.MODULAR_DATA, newData);
                StatsEngine.bake(result, newData);
                return result;
        }

        private record Application(ItemStack stack, ModularData data, Modifier modifier, int newLevel) {}

        private Optional<Application> findApplication(CraftingInput input) {
                ItemStack modularStack = null;
                Item costItem = null;
                int costCount = 0;
                for (int i = 0; i < input.size(); i++) {
                        ItemStack stack = input.getItem(i);
                        if (stack.isEmpty()) {
                                continue;
                        }
                        if (ModDataHooks.dataOf(stack).isPresent()) {
                                if (modularStack != null) {
                                        return Optional.empty(); // two modular items
                                }
                                modularStack = stack;
                        } else {
                                if (costItem != null && costItem != stack.getItem()) {
                                        return Optional.empty(); // mixed ingredients
                                }
                                costItem = stack.getItem();
                                costCount++;
                        }
                }
                if (modularStack == null || costItem == null) {
                        return Optional.empty();
                }
                ModularData data = ModDataHooks.dataOf(modularStack).orElseThrow();
                boolean tool = com.modulardreams.equipment.ModularToolType.byId(data.equipmentType()).isPresent();

                Optional<Modifier> modifier = Modifier.byCostItem(costItem);
                if (modifier.isEmpty()) {
                        return Optional.empty();
                }
                Modifier m = modifier.get();
                if (costCount != m.costPerLevel()) {
                        return Optional.empty();
                }
                // category check
                if (!m.category().matches(tool)) {
                        return Optional.empty();
                }
                int current = data.levelOf(m.id());
                if (current >= m.maxLevel()) {
                        return Optional.empty();
                }
                return Optional.of(new Application(modularStack, data, m, current + 1));
        }

        @Override
        public RecipeSerializer<? extends CustomRecipe> getSerializer() {
                return SERIALIZER;
        }
}
