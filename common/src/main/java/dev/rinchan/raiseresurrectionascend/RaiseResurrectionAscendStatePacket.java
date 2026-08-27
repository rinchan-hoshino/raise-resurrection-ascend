package dev.rinchan.raiseresurrectionascend;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RaiseResurrectionAscendStatePacket(boolean downed, float recoveryThreshold) implements CustomPacketPayload {
    public static final Type<RaiseResurrectionAscendStatePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RaiseResurrectionAscend.MOD_ID, "state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RaiseResurrectionAscendStatePacket> CODEC = new StreamCodec<>() {
        @Override
        public RaiseResurrectionAscendStatePacket decode(RegistryFriendlyByteBuf buffer) {
            return new RaiseResurrectionAscendStatePacket(buffer.readBoolean(), buffer.readFloat());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RaiseResurrectionAscendStatePacket packet) {
            buffer.writeBoolean(packet.downed());
            buffer.writeFloat(packet.recoveryThreshold());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
