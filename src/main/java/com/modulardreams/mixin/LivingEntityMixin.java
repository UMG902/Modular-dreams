package com.modulardreams.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.equipment.LiningItem;

/**
 * Armor linings fit EVERY humanoid armor slot (head, chest, legs, feet).
 *
 * <p>26.3 vanilla equipping is strict: the {@code EQUIPPABLE} component
 * holds exactly one slot and {@code LivingEntity#isEquippableInSlot} only
 * accepts a stack when that slot matches the one being filled. One item can
 * therefore never natively be wearable in all four armor slots - but a
 * lining is the padding layer of ANY future armor piece, so one lining item
 * per material must be wearable everywhere.
 *
 * <p>This injection returns true for a lining in any of the four humanoid
 * armor slots, which is the single gate behind the inventory's armor slots
 * ({@code ArmorSlot#mayPlace}). Everything else - hands, body, saddle, or a
 * non-lining stack - falls through to the vanilla decision untouched.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "isEquippableInSlot", at = @At("HEAD"), cancellable = true)
    private void modular_dreams$liningFitsAnyArmorSlot(ItemStack stack, EquipmentSlot slot,
            CallbackInfoReturnable<Boolean> cir) {
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && stack.getItem() instanceof LiningItem) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
