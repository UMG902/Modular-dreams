package com.modulardreams.equipment;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.modulardreams.material.ModTraits;

/**
 * An armor plating part of Modular Dreams v2. Like the linings, a plating
 * carries its material's trait (the trait IS the material's name) - shown
 * as a gray tooltip line. The plating is the OUTER layer of a future armor
 * piece; protection arrives when plating + lining are assembled.
 */
public class PlatingItem extends Item {

    private final String materialId;

    public PlatingItem(Properties properties, String materialId) {
        super(properties);
        this.materialId = materialId;
    }

    /** The plating material's id - which is by definition also its trait id. */
    public String materialId() {
        return this.materialId;
    }

    /** Whether the stack is a plating item. */
    public static boolean isPlating(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PlatingItem;
    }

    /** One "Traits: <name>" line in the inherent (gray) style - the trait IS the plating. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        MutableComponent line = Component.translatable("traits.modular_dreams.label")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable(ModTraits.nameKey(this.materialId))
                        .withStyle(ChatFormatting.GRAY));
        tooltip.accept(line);
    }
}
