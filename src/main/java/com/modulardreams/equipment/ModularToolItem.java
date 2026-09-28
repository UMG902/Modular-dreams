package com.modulardreams.equipment;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.impl.item.ItemExtensions;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.ModTraits;
import com.modulardreams.stats.StatsEngine;

/**
 * A modular tool or weapon. All stats live in data components; this class only
 * adds the trait hooks (Fiery ignition, Dense/reinforced durability reduction)
 * and the modular tooltip.
 */
public class ModularToolItem extends Item {

        public ModularToolItem(Properties properties) {
                super(properties);
                // route durability damage through our trait-aware handler (obsidian, reinforced...)
                ((ItemExtensions) this).fabric_setCustomDamageHandler(this::hurtAndBreakWithTraits);
        }

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
                // tools from older builds silently adopt the current stat formulas
                StatsEngine.refreshIfStale(stack);
        }

        protected int hurtAndBreakWithTraits(ItemStack stack, int amount, LivingEntity entity,
                        net.minecraft.world.entity.EquipmentSlot slot, Runnable breakCallback) {
                ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
                if (data == null) {
                        return amount;
                }
                // legacy stacks may miss the DAMAGE component (see ensureDamageComponent)
                StatsEngine.ensureDamageComponent(stack);
                float reduction = StatsEngine.durabilityDamageReduction(stack, data);
                return Math.max(1, Math.round(amount * (1.0F - reduction)));
        }

        @Override
        public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
                ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
                if (data != null && !target.level().isClientSide()) {
                        float seconds = StatsEngine.fierySeconds(stack, data);
                        if (seconds > 0.0F && !target.fireImmune()) {
                                target.igniteForTicks(Math.round(seconds * 20.0F));
                        }
                }
                super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context,
                        net.minecraft.world.item.component.TooltipDisplay display,
                        java.util.function.Consumer<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
                if (data == null || data.equipmentType().equals("none")) {
                        return;
                }
                ModDataHooks.appendDurabilityTooltip(stack, tooltip);
                tooltip.accept(Component.translatable("tooltip.modular_dreams.parts").withStyle(ChatFormatting.DARK_GRAY));
                for (var part : data.parts()) {
                        ModularToolType type = ModularToolType.byId(data.equipmentType()).orElse(null);
                        PartType partType = resolvePartType(type, part.part());
                        if (partType == null) {
                                continue;
                        }
                        MutableComponent line = Component.literal("")
                                        .append(Component.translatable(partType.translationKey()).withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                                        .append(Component.translatable("material.modular_dreams." + part.material())
                                                        .withStyle(ChatFormatting.GRAY));
                        tooltip.accept(line);
                        ModDataHooks.material(part.material()).ifPresent(mat -> {
                                List<ModTraits> traits = mat.traits();
                                if (!traits.isEmpty()) {
                                        MutableComponent traitLine = Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY);
                                        for (int i = 0; i < traits.size(); i++) {
                                                if (i > 0) {
                                                        traitLine.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                                                }
                                                traitLine.append(Component.translatable(traits.get(i).nameKey())
                                                                .withStyle(ChatFormatting.BLUE));
                                        }
                                        tooltip.accept(traitLine);
                                }
                        });
                }
                ModDataHooks.appendModifierTooltip(data, tooltip);
        }

        private static PartType resolvePartType(ModularToolType type, String partId) {
                if (type == null) {
                        return null;
                }
                for (PartType partType : type.parts()) {
                        if (partType.id.equals(partId)) {
                                return partType;
                        }
                }
                return null;
        }
}
