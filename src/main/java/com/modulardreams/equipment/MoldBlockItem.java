package com.modulardreams.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.modulardreams.block.MoldBlock;
import com.modulardreams.component.ModDataComponents;
import com.modulardreams.part.MoldShape;
import com.modulardreams.part.PartType;

/**
 * The item form of the clay and the terracotta mold blocks. Both carry the
 * optional {@code mold_part} component: the part shape they were shaped
 * into. When placed, the shape moves onto the block's {@code shape} state
 * property so the carved cutout model shows immediately.
 *
 * <p>Terracotta molds survive three uses in the Melting Upgrade (max
 * durability 3, one durability point per use); clay molds break after one.
 */
public class MoldBlockItem extends BlockItem {

        public MoldBlockItem(Block block, Properties properties) {
                super(block, properties);
        }

        @Override
        protected BlockState getPlacementState(BlockPlaceContext context) {
                BlockState state = super.getPlacementState(context);
                if (state != null && state.hasProperty(MoldBlock.SHAPE)) {
                        var part = partOf(context.getItemInHand());
                        if (part.isPresent()) {
                                state = state.setValue(MoldBlock.SHAPE, MoldShape.of(part.get()));
                        }
                }
                return state;
        }

        /** @return the part shape of this mold, or empty for an unshaped mold. */
        public static java.util.Optional<PartType> partOf(ItemStack stack) {
                String id = stack.get(ModDataComponents.MOLD_PART);
                return id == null ? java.util.Optional.empty() : PartType.byId(id);
        }

        /** @return a copy of this mold shaped into the given part. */
        public static ItemStack shaped(ItemStack mold, PartType part) {
                ItemStack copy = mold.copy();
                copy.set(ModDataComponents.MOLD_PART, part.id);
                return copy;
        }

        /** @return whether this stack is any mold item (clay or terracotta). */
        public static boolean isMold(ItemStack stack) {
                return !stack.isEmpty() && stack.getItem() instanceof MoldBlockItem;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                        java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
                java.util.Optional<PartType> part = partOf(stack);
                if (part.isPresent()) {
                        tooltip.accept(Component.translatable("tooltip.modular_dreams.mold_shape",
                                        Component.translatable(part.get().translationKey()))
                                        .withStyle(ChatFormatting.GRAY));
                } else {
                        tooltip.accept(Component.translatable("tooltip.modular_dreams.mold_unshaped")
                                        .withStyle(ChatFormatting.DARK_GRAY));
                }
                if (stack.getMaxDamage() > 0) {
                        int usesLeft = Math.max(1, stack.getMaxDamage() - stack.getDamageValue());
                        tooltip.accept(Component.translatable("tooltip.modular_dreams.mold_uses", usesLeft)
                                        .withStyle(ChatFormatting.BLUE));
                }
        }
}
