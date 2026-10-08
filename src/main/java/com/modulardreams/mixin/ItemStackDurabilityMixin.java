package com.modulardreams.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.stats.StatsEngine;
import com.modulardreams.traits.DurabilityHooks;
import com.modulardreams.traits.TraitEngine;
import com.modulardreams.traits.TraitTuning;

/**
 * The single durability choke point. EVERY durability damage of a
 * player-held or worn stack funnels through the 4-arg hurtAndBreak:
 * <ul>
 *   <li>copper "Powered" immunity window and the diamond "Fortunate" saves
 *       cancel the damage entirely (DurabilityHooks.shouldPrevent);</li>
 *   <li>netherite "Netherforged" near Nether heat takes 50% less;</li>
 *   <li>a modular item NEVER breaks: the damage that would destroy it is
 *       clamped to the last durability point and the item is re-baken
 *       BROKEN - it stops working (tools lose their combat stats and mine
 *       at hand speed, armor loses its protection) until repaired.</li>
 * </ul>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityMixin {

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), cancellable = true)
    private void modular_dreams$preventDurabilityDamage(int amount, ServerLevel level, ServerPlayer player,
            Consumer<ItemStack> onBreak, CallbackInfo ci) {
        if (amount <= 0 || player == null || level == null) {
            return;
        }
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.isDamageableItem()) {
            return;
        }
        // only OUR items get the trait behaviour and the broken clamp
        ModDataComponents.ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
        if (data == null || data.equipmentType().equals("none")) {
            return;
        }
        long gameTime = level.getGameTime();

        // netherite near heat: 50% less durability damage
        int effective = amount;
        if (data.unlocks() < ModDataComponents.ModularData.MAX_UNLOCKS // egg tools keep full use damage
                && com.modulardreams.material.ModTraits.dataHasTrait(
                        data, com.modulardreams.material.ModTraits.NETHERITE)
                && TraitEngine.isHeatEmpowered(player)) {
            effective = Math.max(1, Math.round(amount
                    * (1.0F - (float) TraitTuning.NETHER_HEAT_DURABILITY_REDUCTION)));
        }

        // the broken clamp: never actually break
        if (stack.getDamageValue() + effective >= stack.getMaxDamage()) {
            stack.setDamageValue(stack.getMaxDamage() - 1);
            StatsEngine.bake(stack, data); // re-bake into the BROKEN state
            ci.cancel();
            return;
        }

        if (DurabilityHooks.shouldPrevent(stack, player, gameTime)) {
            ci.cancel();
        }
    }
}
