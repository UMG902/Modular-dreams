package com.modulardreams.equipment;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.modifier.Modifier;

/**
 * Shared tooltip helpers for modular items.
 */
public final class ModDataHooks {

        private ModDataHooks() {}

        public static Optional<ModularMaterial> material(String id) {
                return ModMaterials.byId(id);
        }

        public static void appendModifierTooltip(ModularData data, Consumer<Component> tooltip) {
                if (!data.modifiers().isEmpty()) {
                        tooltip.accept(Component.translatable("tooltip.modular_dreams.modifiers")
                                        .withStyle(ChatFormatting.DARK_GRAY));
                        for (var entry : data.modifiers()) {
                                Optional<Modifier> modifier = Modifier.byId(entry.id());
                                if (modifier.isPresent()) {
                                        tooltip.accept(Component.literal(" ")
                                                        .append(Component.translatable(modifier.get().nameKey())
                                                                        .withStyle(ChatFormatting.LIGHT_PURPLE))
                                                        .append(Component.literal(" " + romanOrLevel(entry.level()))
                                                                        .withStyle(ChatFormatting.DARK_PURPLE)));
                                }
                        }
                }
        }

        /** Reads the modular data of a stack, if any. */
        public static Optional<ModularData> dataOf(ItemStack stack) {
                return Optional.ofNullable(stack.get(ModDataComponents.MODULAR_DATA))
                                .filter(d -> !d.equipmentType().equals("none"));
        }

        private static String romanOrLevel(int level) {
                return "I".repeat(Math.max(0, Math.min(level, 5)));
        }
}
