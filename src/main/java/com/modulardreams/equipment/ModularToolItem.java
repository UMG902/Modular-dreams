package com.modulardreams.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModTraits;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.StatsEngine;
import com.modulardreams.traits.TraitEngine;

/**
 * A modular tool or weapon of v2. All stats live in vanilla data components
 * baked by the {@code StatsEngine} from its {@link ModularData}; this class
 * adds the modular tooltip and keeps stale stacks refreshed.
 */
public class ModularToolItem extends Item {

    public ModularToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        // tools from older builds silently adopt the current stat formulas
        StatsEngine.refreshIfStale(stack);
    }

    /**
     * Breeze rod "Windborne": right-clicking a tool with a breeze rod
     * handle fires a small wind charge gust at the player's feet, launching
     * them upward (a mace combo). Everything else passes through untouched.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
        // the spear keeps its vanilla charge/stab use (handled by Item#use), so the
        // breeze gust only replaces right-click on the other tools
        if (data != null && !"spear".equals(data.equipmentType())
                && ModTraits.BREEZE_ROD.equals(data.materialOf(PartType.HANDLE.id).orElse(""))) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                TraitEngine.tryWindborne(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }
        // vanilla behaviour: spear kinetic charge, blocking, consumables, ...
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display,
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
                    .append(Component.translatable("material.modular_dreams." + part.material())
                            .withStyle(ChatFormatting.GRAY));
            tooltip.accept(line);
        }
        appendTraitLine(data, tooltip);
    }

    /**
     * One line with every trait the tool carries: its parts' inherent
     * material traits (gray) followed by the traits applied through the
     * smithing table (aqua).
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
