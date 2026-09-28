package com.modulardreams.network;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.ModularDreams;

/**
 * Custom networking of Modular Dreams.
 *
 * In 26.3 the vanilla recipe sync only covers stonecutter recipes; custom
 * recipe types never reach the client. The Part Builder therefore receives its
 * list of possible outputs through this dedicated S2C payload: whenever the
 * server menu (re)computes the part recipes matching the current input, it
 * pushes the option list (result stack + material cost per option) to the open
 * menu's client. Selection itself rides the vanilla button-click packet, like
 * the stonecutter.
 */
public final class ModNetworking {

        private ModNetworking() {}

        // ------------------------------------------------------------------ payloads

        /** One selectable output of the Part Builder for the current input. */
        public record PartOption(ItemStack result, int cost) {
                public static final StreamCodec<RegistryFriendlyByteBuf, PartOption> STREAM_CODEC = StreamCodec.composite(
                                ItemStack.STREAM_CODEC, PartOption::result,
                                ByteBufCodecs.VAR_INT, PartOption::cost,
                                PartOption::new);
        }

        /**
         * S2C: the list of part recipes matching the Part Builder's current
         * input, in server order (the client's option indexes must match the
         * server's for the vanilla button-click selection to stay consistent).
         */
        public record PartBuilderOptionsPayload(int containerId, List<PartOption> options)
                        implements CustomPacketPayload {

                public static final CustomPacketPayload.Type<PartBuilderOptionsPayload> TYPE = new CustomPacketPayload.Type<>(
                                ModularDreams.id("part_builder_options"));

                public static final StreamCodec<RegistryFriendlyByteBuf, PartBuilderOptionsPayload> STREAM_CODEC = StreamCodec.composite(
                                ByteBufCodecs.VAR_INT, PartBuilderOptionsPayload::containerId,
                                PartOption.STREAM_CODEC.apply(ByteBufCodecs.list()), PartBuilderOptionsPayload::options,
                                PartBuilderOptionsPayload::new);

                @Override
                public Type<? extends CustomPacketPayload> type() {
                        return TYPE;
                }
        }

        // ------------------------------------------------------------------ registration

        public static void initialize() {
                net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay()
                                .register(PartBuilderOptionsPayload.TYPE, PartBuilderOptionsPayload.STREAM_CODEC);
        }

        /** Server helper: pushes the current option list of one open Part Builder menu. */
        public static void sendPartBuilderOptions(ServerPlayer player, int containerId,
                        List<PartOption> options) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                                new PartBuilderOptionsPayload(containerId, List.copyOf(options)));
        }
}
