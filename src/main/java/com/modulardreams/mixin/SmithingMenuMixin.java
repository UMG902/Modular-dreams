package com.modulardreams.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

import com.modulardreams.recipe.SmithingModifierRecipe;

/**
 * Lets a smithing table modifier consume its own amount of the addition
 * material - {@code count} (default 3) - when the player takes the result.
 * Every vanilla smithing recipe keeps consuming exactly 1.
 *
 * <p>The modifier recipe is captured at the head of {@code onTake}, while
 * all three input slots are still intact. {@code shrinkStackInSlot(2)} then
 * shrinks the addition stack by the recipe's {@code count} instead of 1;
 * slots 0 and 1 are untouched (the template in slot 0 is consumed by
 * vanilla's own template shrink). The capture is cleared at the tail of
 * {@code onTake} so it can never leak into a later interaction.
 *
 * <p>This runs on both the server and the (predicting) client: the client
 * side resolves the recipe against its synced recipe manager, so both sides
 * agree on the consumed amount.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

    @Shadow
    @Final
    private Level level;

    @Shadow
    protected abstract SmithingRecipeInput createRecipeInput();

    @Unique
    private SmithingModifierRecipe modular_dreams$capturedModifier;

    @Unique
    private int modular_dreams$capturedConsumption;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void modular_dreams$captureModifier(Player player, ItemStack stack, CallbackInfo ci) {
        this.modular_dreams$capturedModifier = null;
        this.modular_dreams$capturedConsumption = 1;
        try {
            if (this.level.recipeAccess() instanceof RecipeManager manager) {
                SmithingRecipeInput input = this.createRecipeInput();
                Optional<RecipeHolder<SmithingRecipe>> found = manager.getRecipeFor(
                        RecipeType.SMITHING, input, this.level);
                if (found.isPresent() && found.get().value() instanceof SmithingModifierRecipe modifier) {
                    this.modular_dreams$capturedModifier = modifier;
                    this.modular_dreams$capturedConsumption = modifier.additionCount();
                }
            }
        } catch (Exception e) {
            // never let a modifier lookup break a vanilla take
        }
    }

    @WrapOperation(
            method = "shrinkStackInSlot(I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void modular_dreams$wrapShrink(ItemStack stack, int amount, Operation<Void> original, int slotIndex) {
        if (slotIndex == SmithingMenu.ADDITIONAL_SLOT && this.modular_dreams$capturedModifier != null) {
            amount = this.modular_dreams$capturedConsumption;
        }
        original.call(stack, amount);
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void modular_dreams$releaseModifier(Player player, ItemStack stack, CallbackInfo ci) {
        this.modular_dreams$capturedModifier = null;
        this.modular_dreams$capturedConsumption = 1;
    }
}
