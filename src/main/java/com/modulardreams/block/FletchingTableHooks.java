package com.modulardreams.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import com.modulardreams.menu.FletchingMenu;

/**
 * Makes the vanilla fletching table functional: right-clicking it opens the
 * fletching menu (2 potions + up to 64 arrows &rarr; tipped arrows). Done
 * through Fabric's UseBlockCallback, which fires before block use - no mixin
 * into vanilla block behaviour needed.
 */
public final class FletchingTableHooks {

    private FletchingTableHooks() {}

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            // MAIN_HAND only: otherwise the interaction fires twice (once per hand)
            if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!level.getBlockState(pos).is(Blocks.FLETCHING_TABLE)) {
                return InteractionResult.PASS;
            }
            // vanilla rule: sneaking with something in hand places/uses that item
            // instead of using the block (otherwise nothing could be placed on it)
            if (player.isSecondaryUseActive()
                    && !(player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty())) {
                return InteractionResult.PASS;
            }
            // spectators never interact with blocks
            if (player.isSpectator()) {
                return InteractionResult.PASS;
            }
            // only the server opens menus; the client gets the success swing
            if (!level.isClientSide()) {
                player.openMenu(new FletchingMenuProvider(pos));
            }
            return InteractionResult.SUCCESS;
        });
    }

    /** Menu provider carrying the table position to the client menu factory. */
    private record FletchingMenuProvider(BlockPos pos) implements ExtendedMenuProvider<BlockPos> {

        @Override
        public Component getDisplayName() {
            return Blocks.FLETCHING_TABLE.getName();
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId,
                net.minecraft.world.entity.player.Inventory inventory, Player player) {
            return new FletchingMenu(containerId, inventory, pos);
        }

        @Override
        public BlockPos getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
            return pos;
        }
    }
}
