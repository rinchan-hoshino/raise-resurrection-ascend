package dev.rinchan.raiseresurrectionascend;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RaiseResurrectionAscendStatePacket(
        boolean downed,
        float recoveryThreshold,
        Component deathMessage) implements CustomPacketPayload {
    public static final Type<RaiseResurrectionAscendStatePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RaiseResurrectionAscend.MOD_ID, "state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RaiseResurrectionAscendStatePacket> CODEC = new StreamCodec<>() {
        @Override
        public RaiseResurrectionAscendStatePacket decode(RegistryFriendlyByteBuf buffer) {
            return new RaiseResurrectionAscendStatePacket(
                buffer.readBoolean(),
                buffer.readFloat(),
                ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RaiseResurrectionAscendStatePacket packet) {
            buffer.writeBoolean(packet.downed());
            buffer.writeFloat(packet.recoveryThreshold());
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, packet.deathMessage());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
