package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.material.ModTraits;
import com.modulardreams.traits.TraitAggregator;

/**
 * Item-entity protection:
 * <ul>
 *   <li>every stack carrying the NETHERITE trait is immune to fire and lava
 *       as a dropped item (assembled netherite items already carry the
 *       vanilla {@code DamageResistant} component - this also covers the
 *       loose netherite parts);</li>
 *   <li>a tool that carries the DRAGON EGG (the third unlock) is completely
 *       indestructible as an item entity: fire, lava, cactus, explosions,
 *       everything.</li>
 * </ul>
 * The void path and /kill are handled by {@link EntityProtectMixin}.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void modular_dreams$protectItems(ServerLevel level, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.getItem().isEmpty()) {
            return;
        }
        // the dragon egg tool is indestructible, period
        ModDataComponents.ModularData data = self.getItem().get(ModDataComponents.MODULAR_DATA);
        if (data != null && data.hasDragonEgg()) {
            cir.setReturnValue(Boolean.FALSE);
            return;
        }
        // netherite items do not burn
        if (source.is(DamageTypeTags.IS_FIRE) && TraitAggregator.stackHasTrait(self.getItem(), ModTraits.NETHERITE)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
