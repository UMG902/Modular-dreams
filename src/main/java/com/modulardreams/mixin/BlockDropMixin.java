package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.modulardreams.traits.DropHooks;

/**
 * Block-drop traits. The 6-arg {@code dropResources} is the ONLY drop path
 * that runs for player mining - the mixin opens the trait context around it
 * (vacuum = slime modifier, auto-smelt = blaze rod handle). Every dropped
 * stack then flows through {@code popResource}, where the context intercepts
 * it. Container breaks and explosions never see the context.
 */
@Mixin(Block.class)
public abstract class BlockDropMixin {

    private static final String DROP_RESOURCES_PLAYER =
            "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V";

    private static final String POP_RESOURCE =
            "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V";

    @Inject(method = DROP_RESOURCES_PLAYER, at = @At("HEAD"))
    private static void modular_dreams$openDropContext(BlockState state, Level level, BlockPos pos,
            BlockEntity blockEntity, Entity breaker, ItemStack tool, CallbackInfo ci) {
        if (breaker instanceof ServerPlayer player) {
            DropHooks.open(player, tool);
        }
    }

    @Inject(method = DROP_RESOURCES_PLAYER, at = @At("TAIL"))
    private static void modular_dreams$closeDropContext(BlockState state, Level level, BlockPos pos,
            BlockEntity blockEntity, Entity breaker, ItemStack tool, CallbackInfo ci) {
        DropHooks.close();
    }

    @Inject(method = POP_RESOURCE, at = @At("HEAD"), cancellable = true)
    private static void modular_dreams$vacuumOrSmelt(Level level, BlockPos pos, ItemStack stack, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel) || stack.isEmpty()) {
            return;
        }
        ItemStack replacement = DropHooks.intercept(serverLevel, stack);
        if (replacement == null) {
            ci.cancel(); // vacuumed straight into the inventory
        } else if (replacement != stack) {
            // auto-smelted: scatter the smelted result instead
            ci.cancel();
            DropHooks.scatter(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, replacement);
        }
    }
}
