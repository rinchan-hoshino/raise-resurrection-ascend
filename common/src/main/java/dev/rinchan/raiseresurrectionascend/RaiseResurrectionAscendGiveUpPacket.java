package dev.rinchan.raiseresurrectionascend;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RaiseResurrectionAscendGiveUpPacket() implements CustomPacketPayload {
    public static final Type<RaiseResurrectionAscendGiveUpPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RaiseResurrectionAscend.MOD_ID, "give_up"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RaiseResurrectionAscendGiveUpPacket> CODEC = StreamCodec.unit(new RaiseResurrectionAscendGiveUpPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
