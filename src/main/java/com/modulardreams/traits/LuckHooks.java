package com.modulardreams.traits;

import net.minecraft.world.item.ItemStack;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.material.ModTraits;
import com.modulardreams.ModularDreams;

import net.minecraft.world.entity.LivingEntity;

/**
 * Luck (the lapis modifier), implemented exactly like Fortune and Looting
 * per the user's spec: the block-loot enchantment lookup and the mob-loot
 * enchantment lookup each add the modifier's level for lapis-trait tools.
 * The mixins in {@code EnchantmentHelperMixin} call into these hooks.
 */
public final class LuckHooks {

    private LuckHooks() {}

    /** Effective Fortune level added for a TOOL the block loot was computed with. */
    public static int fortuneBonus(ItemStack tool) {
        if (tool == null || tool.isEmpty()) {
            return 0;
        }
        return lapisLevel(tool);
    }

    /** Effective Looting level added for the entity whose kill loot is computed. */
    public static int lootingBonus(LivingEntity killer) {
        if (killer == null) {
            return 0;
        }
        return lapisLevel(killer.getMainHandItem());
    }

    private static int lapisLevel(ItemStack stack) {
        ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
        if (data == null || !data.hasModifier(ModTraits.LAPIS)) {
            return 0;
        }
        // traits have no levels - lapis is Fortune I / Looting I, once
        return TraitTuning.LUCK_LEVEL;
    }
}
