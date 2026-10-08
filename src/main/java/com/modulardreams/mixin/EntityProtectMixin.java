package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import com.modulardreams.component.ModDataComponents;

/**
 * The two destroy paths that do NOT go through damage - both closed for a
 * tool that carries the DRAGON EGG:
 * <ul>
 *   <li>{@code kill()} - /kill and the kill command remove entities without
 *       hurting them first; the egg tool simply refuses.</li>
 *   <li>{@code onBelowWorld()} - falling out of the world discards items
 *       with no damage call; the egg tool is instead returned to the world
 *       spawn, because an unreachable item would lose the dragon egg
 *       forever.</li>
 * </ul>
 */
@Mixin(Entity.class)
public abstract class EntityProtectMixin {

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void modular_dreams$surviveKill(CallbackInfo ci) {
        if ((Object) this instanceof ItemEntity item && isEggTool(item)) {
            ci.cancel();
        }
    }

    @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
    private void modular_dreams$voidReturnsTheEgg(CallbackInfo ci) {
        if ((Object) this instanceof ItemEntity item && isEggTool(item)) {
            if (item.level() instanceof ServerLevel level) {
                // 26.3: world spawn lives in the server's RespawnData
                var respawn = level.getServer().getRespawnData();
                ServerLevel spawnLevel = respawn.dimension() == null ? null
                        : level.getServer().getLevel(respawn.dimension());
                if (spawnLevel == null) {
                    spawnLevel = level.getServer().overworld();
                }
                if (spawnLevel != null) {
                    var spawn = respawn.pos();
                    item.teleportTo(spawnLevel,
                            spawn.getX() + 0.5D,
                            Math.min(spawn.getY() + 1.0D, spawnLevel.getMaxY() - 16),
                            spawn.getZ() + 0.5D, java.util.Set.of(), 0.0F, 0.0F, false);
                    item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                }
            }
            ci.cancel();
        }
    }

    private static boolean isEggTool(ItemEntity item) {
        ModDataComponents.ModularData data = item.getItem().get(ModDataComponents.MODULAR_DATA);
        return data != null && data.hasDragonEgg();
    }
}
