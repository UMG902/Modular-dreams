package com.modulardreams.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.component.ModDataComponents.PartData;
import com.modulardreams.equipment.ModDataHooks;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.StatsEngine;

/**
 * Matching logic of the overhaul stations (step 1):
 *
 * - Part Builder    : converts material items into generic parts (cost in units)
 * - Assembly Table  : assembles a tool from its handle, binding and head -
 *                     one fixed slot per part, in assembly order
 *
 * Modifiers and repair return in a later overhaul step.
 */
public final class StationLogic {

        private StationLogic() {}

        // ------------------------------------------------------------------ material items

        private static final Map<Ingredient, ModularMaterial> BY_INGREDIENT = new HashMap<>();

        /** @return the material this stack can be shaped into, if any. */
        public static ModularMaterial materialOf(ItemStack stack) {
                if (BY_INGREDIENT.isEmpty()) {
                        for (ModularMaterial mat : ModMaterials.materials()) {
                                BY_INGREDIENT.put(mat.crafting(), mat);
                        }
                }
                for (var entry : BY_INGREDIENT.entrySet()) {
                        if (entry.getKey().test(stack)) {
                                return entry.getValue();
                        }
                }
                return null;
        }

        // ------------------------------------------------------------------ parts

        /** Builds one part of the given material (Part Builder result). */
        public static ItemStack makePart(PartType part, ModularMaterial material) {
                return new ItemStack(ModPartItems.get(part, material), 1);
        }

        /** @return whether this material can be shaped into this part type. */
        public static boolean canMake(PartType part, ModularMaterial material) {
                return part.allowedMaterials().contains(material);
        }

        // ------------------------------------------------------------------ assembly (Assembly Table)

        public record Assembly(ModularToolType type, PartType handlePart, ModularMaterial handleMaterial,
                        PartType bindingPart, ModularMaterial bindingMaterial,
                        PartType headPart, ModularMaterial headMaterial) {}

        /**
         * The Assembly Table has one fixed slot per part. GUI order (top to
         * bottom) is: slot 0 = head, slot 1 = binding, slot 2 = handle. A
         * valid assembly needs all three slots filled with the parts of ONE
         * tool type, each matching its slot position. The handle is the
         * UNIVERSAL handle (it fits every tool); the tool type is taken from
         * the head - taking it from the handle broke assembly completely
         * (the universal handle's toolId is "any").
         */
        public static Optional<Assembly> classifyAssembly(ItemStack handleStack, ItemStack headStack,
                        ItemStack bindingStack) {
                if (handleStack.isEmpty() || headStack.isEmpty() || bindingStack.isEmpty()) {
                        return Optional.empty();
                }
                Optional<ModPartItems.PartIdentity> handle = ModPartItems.resolve(handleStack);
                Optional<ModPartItems.PartIdentity> head = ModPartItems.resolve(headStack);
                Optional<ModPartItems.PartIdentity> binding = ModPartItems.resolve(bindingStack);
                if (handle.isEmpty() || head.isEmpty() || binding.isEmpty()) {
                        return Optional.empty(); // any foreign item disqualifies
                }
                // each slot must hold the part of its own role (heads and
                // bindings are tool-specific; the handle is the universal one)
                if (handle.get().part().role != PartType.Role.HANDLE
                                || head.get().part().role != PartType.Role.HEAD
                                || binding.get().part().role != PartType.Role.BINDING) {
                        return Optional.empty();
                }
                // the binding must belong to the SAME tool OR be the universal
                // binding - the sword and the spear keep their dedicated
                // bindings (the sword's is displayed as the Guard)
                PartType headPart = head.get().part();
                PartType bindingPart = binding.get().part();
                if (!bindingPart.toolId.equals(headPart.toolId) && !bindingPart.isUniversal()) {
                        return Optional.empty();
                }
                // the tool type comes from the head (the universal handle has
                // no tool of its own)
                Optional<ModularToolType> type = ModularToolType.byId(headPart.toolId);
                return type.map(t -> new Assembly(t,
                                handle.get().part(), handle.get().material(),
                                binding.get().part(), binding.get().material(),
                                head.get().part(), head.get().material()));
        }

        /** Assembles the modular tool for a valid part set. */
        public static ItemStack assemble(Assembly assembly) {
                List<PartData> parts = List.of(
                                new PartData(assembly.handlePart().id, assembly.handleMaterial().id()),
                                new PartData(assembly.bindingPart().id, assembly.bindingMaterial().id()),
                                new PartData(assembly.headPart().id, assembly.headMaterial().id()));
                ModularData data = new ModularData(assembly.type().id, parts, List.of());
                ItemStack result = new ItemStack(ModItems.byName("modular_" + assembly.type().id).orElseThrow());
                result.set(ModDataComponents.MODULAR_DATA, data);
                StatsEngine.bake(result, data);
                return result;
        }

        public static boolean hasModularData(ItemStack stack) {
                return ModDataHooks.dataOf(stack).isPresent();
        }
}
