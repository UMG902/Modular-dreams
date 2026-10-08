package com.modulardreams.equipment;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.modulardreams.material.ModTraits;

/**
 * A wearable armor lining of Modular Dreams v2. One lining item per lining
 * material - it is the padding layer of the future armor pieces, but it is
 * ALSO wearable on its own: right-click (or any armor slot of the vanilla
 * inventory, thanks to {@code LivingEntityMixin}) puts it on.
 *
 * <p>A lining has NO armor value on purpose - it protects nothing and takes
 * no damage. Its only gameplay identity is its TRAIT, which by definition is
 * its own material name (a leather lining carries the {@code leather} trait,
 * a slime lining the {@code slime} trait, and so on). Trait effects arrive
 * with the traits milestone.
 *
 * <p>Equip mechanics in 26.3: the vanilla {@code EQUIPPABLE} component holds
 * exactly one slot per item, so this item carries it with the asset + equip
 * sound (slot HEAD as the carrier) while {@code LivingEntityMixin} opens
 * every humanoid armor slot for it. The worn body layer comes from the
 * equipment asset referenced by the component.
 */
public class LiningItem extends Item {

    /** The humanoid armor slots, in right-click equip priority order. */
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private final String materialId;

    public LiningItem(Properties properties, String materialId) {
        super(properties);
        this.materialId = materialId;
    }

    /** The lining material's id - which is by definition also its trait id. */
    public String materialId() {
        return this.materialId;
    }

    /** Whether the stack is a lining item. */
    public static boolean isLining(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof LiningItem;
    }

    /**
     * Right-click equips the lining into the first EMPTY armor slot (head
     * first). Occupied slots are never swapped out - nothing the player is
     * wearing can be replaced by accident, and a fully armored player simply
     * fails the interaction.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).isEmpty()) {
                player.setItemSlot(slot, stack.split(1));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }

    /** One "Traits: <name>" line in the inherent (gray) style - the trait IS the lining. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        MutableComponent line = Component.translatable("traits.modular_dreams.label")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable(ModTraits.nameKey(this.materialId))
                        .withStyle(ChatFormatting.GRAY));
        tooltip.accept(line);
    }
}
