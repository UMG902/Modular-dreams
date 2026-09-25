package com.modulardreams.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.component.ModDataComponents.PartData;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.equipment.ModularArmorType;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.StatsEngine;

/**
 * Assembles modular tools and armor from their parts (Tinkers'-style).
 *
 * The recipe accepts ANY material combination: place one head, one handle and
 * (for some tools) one binding/guard in any arrangement to build a tool, or
 * the required amount of plates plus one lining to build an armor piece.
 */
public class AssemblyRecipe extends CustomRecipe {

        public static final AssemblyRecipe INSTANCE = new AssemblyRecipe();
        public static final MapCodec<AssemblyRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyRecipe> STREAM_CODEC = StreamCodec
                        .unit(INSTANCE);
        public static final RecipeSerializer<AssemblyRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

        private AssemblyRecipe() {
                super();
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
                return classify(input).isPresent();
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
                Optional<Assembly> assembly = classify(input);
                if (assembly.isEmpty()) {
                        return ItemStack.EMPTY;
                }
                Assembly a = assembly.get();
                List<PartData> parts = new ArrayList<>();
                for (var entry : a.parts().entrySet()) {
                        parts.add(new PartData(entry.getKey().id, entry.getValue().part().id));
                }
                ModularData data = new ModularData(a.typeId(), List.copyOf(parts), List.of());
                ItemStack result = new ItemStack(ModItems.byName("modular_" + a.typeId()).orElseThrow());
                result.set(ModDataComponents.MODULAR_DATA, data);
                StatsEngine.bake(result, data);
                return result;
        }

        // ------------------------------------------------------------------ matching logic

        private record Assembly(String typeId, Map<PartType, ModPartItems.PartIdentity> parts) {}

        private Optional<Assembly> classify(CraftingInput input) {
                List<ModPartItems.PartIdentity> identities = new ArrayList<>();
                for (int i = 0; i < input.size(); i++) {
                        ItemStack stack = input.getItem(i);
                        if (stack.isEmpty()) {
                                continue;
                        }
                        Optional<ModPartItems.PartIdentity> identity = ModPartItems.resolve(stack);
                        if (identity.isEmpty()) {
                                return Optional.empty(); // any foreign item disqualifies the grid
                        }
                        identities.add(identity.get());
                }
                if (identities.isEmpty()) {
                        return Optional.empty();
                }

                boolean hasArmorParts = identities.stream().anyMatch(p -> p.part().materialClass != PartType.MaterialClass.TOOL);
                if (hasArmorParts) {
                        return classifyArmor(identities);
                }
                return classifyTool(identities);
        }

        private Optional<Assembly> classifyTool(List<ModPartItems.PartIdentity> identities) {
                for (ModularToolType type : ModularToolType.values()) {
                        Map<PartType, ModPartItems.PartIdentity> parts = new HashMap<>();
                        boolean valid = true;
                        for (ModPartItems.PartIdentity identity : identities) {
                                PartType part = identity.part();
                                if (!type.parts.contains(part) || parts.containsKey(part)) {
                                        valid = false;
                                        break;
                                }
                                parts.put(part, identity);
                        }
                        if (valid && parts.size() == identities.size() && parts.keySet().equals(new HashSet<>(type.parts))) {
                                return Optional.of(new Assembly(type.id, parts));
                        }
                }
                return Optional.empty();
        }

        private Optional<Assembly> classifyArmor(List<ModPartItems.PartIdentity> identities) {
                Map<PartType, ModPartItems.PartIdentity> parts = new HashMap<>();
                int plates = 0;
                Optional<ModPartItems.PartIdentity> plate = Optional.empty();
                Optional<ModPartItems.PartIdentity> lining = Optional.empty();
                for (ModPartItems.PartIdentity identity : identities) {
                        if (identity.part() == PartType.PLATE) {
                                plates++;
                                if (plate.isEmpty()) {
                                        plate = Optional.of(identity);
                                } else if (!plate.get().material().id().equals(identity.material().id())) {
                                        return Optional.empty(); // all plates must share one material
                                }
                        } else if (identity.part() == PartType.LINING) {
                                if (lining.isPresent()) {
                                        return Optional.empty();
                                }
                                lining = Optional.of(identity);
                        } else {
                                return Optional.empty();
                        }
                }
                if (lining.isEmpty() || plate.isEmpty()) {
                        return Optional.empty();
                }
                parts.put(PartType.PLATE, plate.get());
                parts.put(PartType.LINING, lining.get());
                for (ModularArmorType type : ModularArmorType.values()) {
                        if (type.plateCount == plates) {
                                // turtle scute plates only fit the helmet (it is a shell, not sheet metal)
                                if (plate.get().material().id().equals("turtle_scute") && type != ModularArmorType.HELMET) {
                                        continue;
                                }
                                return Optional.of(new Assembly(type.id, parts));
                        }
                }
                return Optional.empty();
        }

        // ------------------------------------------------------------------ boilerplate

        @Override
        public RecipeSerializer<? extends CustomRecipe> getSerializer() {
                return SERIALIZER;
        }
}
