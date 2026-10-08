package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import com.modulardreams.traits.TraitEngine;

/**
 * Feeds block-state changes into the trait engine's localized redstone/heat caches.
 * This turns repeated volume scans into point updates after the initial cache build.
 *
 * Targets ServerLevel.updatePOIOnBlockStateChange, which Level.setBlock calls on every
 * block state change. ServerLevel overrides this method, so the hook must live on
 * ServerLevel; injecting into Level would not fire on the server.
 */
@Mixin(ServerLevel.class)
public abstract class TraitBlockChangeMixin {

    @Inject(method = "updatePOIOnBlockStateChange", at = @At("HEAD"))
    private void modular_dreams$traitBlockChanged(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        TraitEngine.onBlockChanged((ServerLevel) (Object) this, pos, oldState, newState);
    }
}
