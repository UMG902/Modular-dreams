package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.modulardreams.traits.TraitEngine;

/**
 * Records the raw fall distance when vanilla works out fall damage. The
 * damage that reaches the ALLOW_DAMAGE event has already been scaled by the
 * caller's multiplier (hay bales) and FALL_DAMAGE_MULTIPLIER, so the bounce
 * reads the distance here instead of inverting the damage.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFallMixin {

    @Inject(method = "calculateFallDamage", at = @At("HEAD"))
    private void modular_dreams$recordFallDistance(double fallDistance, float multiplier,
            CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && !player.level().isClientSide()) {
            TraitEngine.recordFallDistance(player, fallDistance);
        }
    }
}
