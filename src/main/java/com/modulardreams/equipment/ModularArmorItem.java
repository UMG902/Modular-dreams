package com.modulardreams.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.server.level.ServerLevel;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModTraits;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.stats.StatsEngine;

/**
 * A modular armor piece of v2: plating (outer, sets the armor slot and every
 * protection stat) + lining (inner, padding trait carrier). All stats live
 * in vanilla data components baked by the {@code StatsEngine} from its
 * {@link ModularData}; this class adds the modular tooltip and keeps stale
 * stacks refreshed.
 *
 * <p>An UNASSEMBLED piece is a blank shell: no components, not wearable,
 * no stats - exactly like the blank tool items. The assembler produces the
 * real thing.
 */
public class ModularArmorItem extends Item {

    public ModularArmorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        // armor from older builds silently adopts the current stat formulas
        StatsEngine.refreshIfStale(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            TooltipDisplay display,
            java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
        if (data == null || data.equipmentType().equals("none")) {
            return;
        }
        for (ModDataComponents.PartData part : data.parts()) {
            ModularMaterial material = ModMaterials.byId(part.material()).orElse(null);
            MutableComponent line = Component.literal("")
                    .append(Component.translatable("part.modular_dreams." + part.part())
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable(
                            material != null ? material.nameKey()
                                    : "material.modular_dreams." + part.material())
                            .withStyle(ChatFormatting.GRAY));
            tooltip.accept(line);
        }
        appendTraitLine(data, tooltip);
    }

    /**
     * One line with every trait the armor piece carries: its parts'
     * inherent material traits (gray) followed by traits applied through
     * the smithing table (aqua, reserved for future armor modifiers).
     */
    private void appendTraitLine(ModularData data, java.util.function.Consumer<Component> tooltip) {
        java.util.List<String> inherent = ModTraits.inherentTraits(data);
        java.util.List<String> applied = ModTraits.appliedTraits(data);
        if (inherent.isEmpty() && applied.isEmpty()) {
            return;
        }
        MutableComponent line = Component.translatable("traits.modular_dreams.label")
                .withStyle(ChatFormatting.DARK_GRAY);
        boolean first = true;
        for (String trait : inherent) {
            line = appendTrait(line, trait, first, ChatFormatting.GRAY);
            first = false;
        }
        for (String trait : applied) {
            line = appendTrait(line, trait, first, ChatFormatting.AQUA);
            first = false;
        }
        tooltip.accept(line);
    }

    private MutableComponent appendTrait(MutableComponent line, String trait, boolean first, ChatFormatting style) {
        if (!first) {
            line = line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
        }
        return line.append(Component.translatable(ModTraits.nameKey(trait)).withStyle(style));
    }
}
