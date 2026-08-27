package dev.rinchan.raiseresurrectionascend;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A key transition, never a client claim that the hold completed. */
public record RaiseResurrectionAscendGiveUpInputPacket(boolean pressed) implements CustomPacketPayload {
    public static final Type<RaiseResurrectionAscendGiveUpInputPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RaiseResurrectionAscend.MOD_ID, "give_up_input")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RaiseResurrectionAscendGiveUpInputPacket> CODEC = new StreamCodec<>() {
        @Override
        public RaiseResurrectionAscendGiveUpInputPacket decode(RegistryFriendlyByteBuf buffer) {
            return new RaiseResurrectionAscendGiveUpInputPacket(buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RaiseResurrectionAscendGiveUpInputPacket packet) {
            buffer.writeBoolean(packet.pressed());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
