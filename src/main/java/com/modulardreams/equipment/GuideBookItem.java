package com.modulardreams.equipment;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The in-game guide book ("Materials & You").
 *
 * Right-clicking opens the mod's own guide book GUI - a Tinkers'-style custom
 * screen with chapter tabs and a two-page spread whose content is built live
 * from the material / part / modifier registries (so it can never go stale and
 * every page is complete). The vanilla written-book UI is no longer used.
 *
 * Opening happens through the {@link GuideBookOpener} client hook: the item
 * code runs in the common source set, the actual screen lives on the client.
 */
public class GuideBookItem extends Item {

        /** Registered by the client initializer; null on dedicated servers. */
        public static volatile GuideBookOpener opener;

        public GuideBookItem(Properties properties) {
                super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
                if (level.isClientSide() && opener != null) {
                        opener.open(player, hand);
                }
                return InteractionResult.SUCCESS;
        }
}
