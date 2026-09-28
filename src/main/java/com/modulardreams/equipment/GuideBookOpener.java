package com.modulardreams.equipment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;

/**
 * Client-side hook that opens the custom guide book GUI.
 *
 * The guide book is a full custom screen (Tinkers'-style: chapter tabs,
 * two-page spread, item icons), which only exists on the client. The item code
 * in the main source set cannot reference client classes, so the client
 * initializer registers an implementation of this interface at startup and the
 * item invokes it from {@code use()} on the client side.
 */
public interface GuideBookOpener {

        /** Opens the guide book GUI on the client for the given held stack. */
        void open(Player player, InteractionHand hand);
}
