package dev.rinchan.raiseresurrectionascend;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RaiseResurrectionAscendGiveUpPacket(boolean pressed) implements CustomPacketPayload {
    public static final Type<RaiseResurrectionAscendGiveUpPacket> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(RaiseResurrectionAscend.MOD_ID, "give_up")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RaiseResurrectionAscendGiveUpPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        RaiseResurrectionAscendGiveUpPacket::pressed,
        RaiseResurrectionAscendGiveUpPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
