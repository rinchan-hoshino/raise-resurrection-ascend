package dev.rinchan.raiseresurrectionascend.client;

import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendGiveUpInputPacket;

@FunctionalInterface
public interface GiveUpInputSender {
    void send(RaiseResurrectionAscendGiveUpInputPacket packet);
}
