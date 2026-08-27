package dev.rinchan.raiseresurrectionascend;

import net.minecraft.server.level.ServerPlayer;

/** Required loader networking boundary for the authoritative S2C state. */
@FunctionalInterface
public interface DownedStateSynchronizer {
    void send(ServerPlayer player, RaiseResurrectionAscendStatePacket packet);
}
