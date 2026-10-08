package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.modulardreams.traits.TraitEngine;

/**
 * Feeds block-state changes into the trait engine's localized redstone/heat caches.
 * This turns repeated volume scans into point updates after the initial cache build.
 */
@Mixin(Level.class)
public abstract class TraitBlockChangeMixin {

    @Inject(method = "onBlockChanged", at = @At("HEAD"))
    private void modular_dreams$traitBlockChanged(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        if ((Object) this instanceof ServerLevel level) {
            TraitEngine.onBlockChanged(level, pos, oldState, newState);
        }
    }
}
