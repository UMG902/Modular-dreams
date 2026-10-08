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
 * Custom networking of Modular Dreams v2.
 *
 * The Part Picker computes its part options from code (not datapack
 * recipes), and 26.3 has no client-side recipe sync for custom menus - so
 * the server pushes the option list (result stack + material cost per
 * option) to the open menu's client through this dedicated S2C payload.
 * Selection itself rides the vanilla button-click packet, like the
 * stonecutter.
 */
public final class ModNetworking {

    private ModNetworking() {}

    // ------------------------------------------------------------------ payloads

    /** One selectable output of the Part Picker for the current input. */
    public record PartOption(ItemStack result, int cost) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PartOption> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, PartOption::result,
                ByteBufCodecs.VAR_INT, PartOption::cost,
                PartOption::new);
    }

    /**
     * S2C: the list of parts the Part Picker can shape from its current
     * input, in server order (the client's option indexes must match the
     * server's for the vanilla button-click selection to stay consistent).
     */
    public record PartPickerOptionsPayload(int containerId, List<PartOption> options)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<PartPickerOptionsPayload> TYPE = new CustomPacketPayload.Type<>(
                ModularDreams.id("part_picker_options"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PartPickerOptionsPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, PartPickerOptionsPayload::containerId,
                PartOption.STREAM_CODEC.apply(ByteBufCodecs.list()), PartPickerOptionsPayload::options,
                PartPickerOptionsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------ registration

    public static void initialize() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay()
                .register(PartPickerOptionsPayload.TYPE, PartPickerOptionsPayload.STREAM_CODEC);
    }

    /** Server helper: pushes the current option list of one open Part Picker menu. */
    public static void sendPartPickerOptions(ServerPlayer player, int containerId, List<PartOption> options) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                new PartPickerOptionsPayload(containerId, List.copyOf(options)));
    }
}
