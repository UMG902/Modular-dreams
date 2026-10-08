package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemInstance;

import com.modulardreams.traits.LuckHooks;

/**
 * Luck (the lapis modifier) rides the vanilla enchantment lookups: block
 * loot asks the TOOL for its Fortune level, mob loot asks the killer for
 * its Looting level. Both lookups gain the lapis modifier's level here, so
 * every vanilla drop formula behaves exactly as if the tool carried the
 * real enchantment - without writing an enchantment onto the item.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "getItemEnchantmentLevel", at = @At("RETURN"), cancellable = true)
    private static void modular_dreams$fortuneFromLapis(Holder<Enchantment> enchantment, ItemInstance piece,
            CallbackInfoReturnable<Integer> cir) {
        if (enchantment.is(Enchantments.FORTUNE) && piece instanceof ItemStack stack) {
            int bonus = LuckHooks.fortuneBonus(stack);
            if (bonus > 0) {
                cir.setReturnValue(cir.getReturnValueI() + bonus);
            }
        }
    }

    @Inject(method = "getEnchantmentLevel", at = @At("RETURN"), cancellable = true)
    private static void modular_dreams$lootingFromLapis(Holder<Enchantment> enchantment, LivingEntity entity,
            CallbackInfoReturnable<Integer> cir) {
        if (enchantment.is(Enchantments.LOOTING)) {
            int bonus = LuckHooks.lootingBonus(entity);
            if (bonus > 0) {
                cir.setReturnValue(cir.getReturnValueI() + bonus);
            }
        }
    }
}
