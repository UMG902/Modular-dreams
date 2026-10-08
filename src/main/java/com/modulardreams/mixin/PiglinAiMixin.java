package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;

import com.modulardreams.material.ModTraits;
import com.modulardreams.traits.TraitAggregator;

/**
 * Piglin hostility, driven by the trait spec:
 * <ul>
 *   <li>Gold "Piglin's Favor": ANY item with the gold trait in the player's
 *       inventory pacifies piglins, exactly like wearing gold armor;</li>
 *   <li>Rose gold "Defiant": piglins are ALWAYS hostile to a player
 *       carrying the rose-gold trait, even in full gold.</li>
 * </ul>
 * {@code PiglinAi.isWearingSafeArmor} is the single gate the piglin sensor
 * uses to decide who counts as gold-armored.
 */
@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {

    @Inject(method = "isWearingSafeArmor", at = @At("RETURN"), cancellable = true)
    private static void modular_dreams$traitDrivenPiglinPacification(LivingEntity entity,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player player)) {
            return;
        }
        // Defiant wins: piglins stay hostile
        if (TraitAggregator.hasTraitInInventory(player, ModTraits.ROSE_GOLD)) {
            cir.setReturnValue(Boolean.FALSE);
            return;
        }
        // Piglin's Favor: the trait counts as wearing gold
        if (!cir.getReturnValueZ() && TraitAggregator.hasTraitInInventory(player, ModTraits.GOLD)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
