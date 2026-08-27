package dev.rinchan.raiseresurrectionascend.client.neoforge;

import dev.rinchan.raiseresurrectionascend.client.RaiseResurrectionAscendClient;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class RaiseResurrectionAscendNeoForgeClient {
    private RaiseResurrectionAscendNeoForgeClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendNeoForgeClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendNeoForgeClient::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendNeoForgeClient::onLoggingOut);
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        RaiseResurrectionAscendClient.resetState();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RaiseResurrectionAscendClient.resetState();
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        RaiseResurrectionAscendClient.tick();
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        RaiseResurrectionAscendClient.renderHud(event.getGuiGraphics());
    }
}
