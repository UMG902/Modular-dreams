package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import com.modulardreams.traits.TraitEngine;

/**
 * Wool lining "Silent": every game event the player causes (walking,
 * breaking blocks, placing, projectiles, ...) is dropped at the server's
 * single game-event entry point. Sculk sensors and wardens perceive the
 * world through these events, so a silent player is undetectable to them.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelGameEventMixin {

    @Inject(method = "gameEvent", at = @At("HEAD"), cancellable = true)
    private void modular_dreams$silentPlayer(Holder<GameEvent> event, Vec3 position,
            GameEvent.Context context, CallbackInfo ci) {
        // the wool flag is cached once per tick: no armor rescan per footstep
        if (context.sourceEntity() instanceof ServerPlayer player
                && TraitEngine.wearsWool(player)) {
            ci.cancel();
        }
    }
}
