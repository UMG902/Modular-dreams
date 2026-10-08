package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import com.modulardreams.material.ModTraits;
import com.modulardreams.traits.DurabilityHooks;
import com.modulardreams.traits.TraitEngine;
import com.modulardreams.traits.TraitTuning;

/**
 * Damage-adjusting traits, applied at the head of the vanilla armor
 * pipeline ({@code actuallyHurt}):
 * <ul>
 *   <li>magma-cream lining "Heatproof": fire/lava damage reduced by 50%;</li>
 *   <li>bone "Piercing": 20% of the target's ARMOR reduction and 20% of the
 *       target's Protection reduction are ignored - two separate defense
 *       layers, each pierced by its own fraction;</li>
 *   <li>publishes the FINAL incoming damage (after reductions) for the
 *       diamond "Fortunate" armor durability save.</li>
 * </ul>
 *
 * <p>Also hosts the leather lining "Frostward" override: a player wearing a
 * leather lining simply cannot freeze ({@code canFreeze} -> false), the same
 * semantic vanilla leather armor uses - freezing damage never applies and
 * the freezing buildup never progresses. Powder Snow itself stays a pit you
 * can fall into.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float modular_dreams$adjustDamage(float damage, ServerLevel level, DamageSource source) {
        LivingEntity target = (LivingEntity) (Object) this;

        // Fortunate armor context: the FINAL damage this hit will deal after
        // the vanilla armor + resistance + Protection pipeline, before any
        // of our own adjustments
        if (target instanceof ServerPlayer) {
            DurabilityHooks.publishArmorContext(
                    (float) finalDamageFraction(target, level, source, damage) * damage, level.getGameTime());
        }

        // Heatproof: reduce fire and lava damage
        if (source.is(DamageTypeTags.IS_FIRE) && target instanceof ServerPlayer player
                && TraitEngine.hasCachedWornTrait(player, ModTraits.MAGMA_CREAM)) {
            damage *= (float) (1.0D - TraitTuning.HEATPROOF_REDUCTION);
        }

        // Piercing: ignore 20% of armor and 20% of Protection, flat
        if (source.getEntity() instanceof ServerPlayer attacker && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            // getWeaponItem() is null for e.g. player-ignited explosions
            ItemStack weapon = source.getWeaponItem();
            boolean pierce = weapon != null && weapon.getOrDefault(
                    com.modulardreams.component.ModDataComponents.MODULAR_DATA,
                    com.modulardreams.component.ModDataComponents.ModularData.EMPTY)
                    .hasModifier(ModTraits.BONE);
            if (pierce && damage > 0.0F) {
                damage = piercedDamage(target, level, source, damage);
            }
        }
        return damage;
    }

    /**
     * The pre-amplified damage so that, after vanilla applies its FULL armor
     * and Protection reductions, exactly 20% of each reduction is ignored:
     * <pre>
     * boosted = damage * (1 - 0.2*armorFrac) / (1 - armorFrac)
     *               * (1 - 0.2*protFrac)  / (1 - protFrac)
     * </pre>
     * Resistance (the potion effect) is a player-side buff, not equipment
     * defense - it is NOT pierced. The result is capped at 4x the input.
     */
    private static float piercedDamage(LivingEntity target, ServerLevel level, DamageSource source, float damage) {
        double armorFrac = armorReductionFraction(target, source, damage);
        double protFrac = protectionReductionFraction(target, level, source, damage);
        double out = damage;
        if (armorFrac > 0.005D && armorFrac < 0.995D) {
            out *= (1.0D - TraitTuning.PIERCING_ARMOR_FRACTION * armorFrac) / (1.0D - armorFrac);
        }
        if (protFrac > 0.005D && protFrac < 0.995D) {
            out *= (1.0D - TraitTuning.PIERCING_PROTECTION_FRACTION * protFrac) / (1.0D - protFrac);
        }
        return (float) Math.min(out, damage * 4.0D);
    }

    /** Fraction of the damage removed by the armor-value layer alone (0..1). */
    private static double armorReductionFraction(LivingEntity target, DamageSource source, float damage) {
        if (damage <= 0.0F) {
            return 0.0D;
        }
        float afterArmor = CombatRules.getDamageAfterAbsorb(target, damage, source,
                target.getArmorValue(), (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        return Math.max(0.0D, Math.min(0.999D, 1.0D - afterArmor / damage));
    }

    /** Fraction of the POST-ARMOR damage removed by Protection alone (0..1). */
    private static double protectionReductionFraction(LivingEntity target, ServerLevel level,
            DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS) || source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return 0.0D;
        }
        float afterArmor = CombatRules.getDamageAfterAbsorb(target, damage, source,
                target.getArmorValue(), (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        if (afterArmor <= 0.0F) {
            return 0.0D;
        }
        float protection = EnchantmentHelper.getDamageProtection(level, target, source);
        if (protection <= 0.0F) {
            return 0.0D;
        }
        float afterMagic = CombatRules.getDamageAfterMagicAbsorb(afterArmor, protection);
        return Math.max(0.0D, Math.min(0.999D, 1.0D - afterMagic / afterArmor));
    }

    /** The damage vanilla will actually apply (armor, then resistance, then Protection). */
    private static double finalDamageFraction(LivingEntity target, ServerLevel level,
            DamageSource source, float damage) {
        if (damage <= 0.0F) {
            return 0.0D;
        }
        double out = 1.0D - armorReductionFraction(target, source, damage);
        if (!source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            if (target.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE)
                    && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int amplifier = target.getEffect(net.minecraft.world.effect.MobEffects.RESISTANCE).getAmplifier();
                out *= Math.max(0.0F, (25.0F - (amplifier + 1) * 5.0F) / 25.0F);
            }
            if (out > 0.0D && !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
                out *= 1.0D - protectionReductionFraction(target, level, source, damage);
            }
        }
        return Math.max(0.0D, Math.min(1.0D, out));
    }

    /** Frostward: leather linings simply cannot freeze (vanilla leather semantics). */
    @Inject(method = "canFreeze", at = @At("HEAD"), cancellable = true)
    private void modular_dreams$frostward(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target instanceof ServerPlayer player
                && TraitEngine.hasCachedWornTrait(player, ModTraits.LEATHER)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void modular_dreams$clearDamageContext(ServerLevel level, DamageSource source, float damage,
            CallbackInfo ci) {
        DurabilityHooks.clearContext();
    }
}
