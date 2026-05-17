package dev.rinchan.downedrevival;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DownedRevivalGiveUpPacket() implements CustomPacketPayload {
    public static final Type<DownedRevivalGiveUpPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DownedRevival.MOD_ID, "give_up"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DownedRevivalGiveUpPacket> CODEC = StreamCodec.unit(new DownedRevivalGiveUpPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
