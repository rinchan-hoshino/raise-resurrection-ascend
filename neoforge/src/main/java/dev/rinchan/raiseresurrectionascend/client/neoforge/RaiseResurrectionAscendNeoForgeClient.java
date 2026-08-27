package dev.rinchan.raiseresurrectionascend.client.neoforge;

import dev.rinchan.raiseresurrectionascend.client.RaiseResurrectionAscendClient;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

public final class RaiseResurrectionAscendNeoForgeClient {
    private static final KeyMapping.Category GIVE_UP_CATEGORY = new KeyMapping.Category(
        RaiseResurrectionAscendClient.GIVE_UP_CATEGORY_ID
    );
    private static final KeyMapping GIVE_UP_KEY = RaiseResurrectionAscendClient.createGiveUpKey(GIVE_UP_CATEGORY);

    private RaiseResurrectionAscendNeoForgeClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RaiseResurrectionAscendNeoForgeClient::onRegisterKeyMappings);
        RaiseResurrectionAscendClient.initializeGiveUp(GIVE_UP_KEY, ClientPacketDistributor::sendToServer);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendNeoForgeClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendNeoForgeClient::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendNeoForgeClient::onLoggingOut);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(GIVE_UP_CATEGORY);
        event.register(GIVE_UP_KEY);
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        RaiseResurrectionAscendClient.resetState();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RaiseResurrectionAscendClient.resetState();
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        RaiseResurrectionAscendClient.clientTick();
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        RaiseResurrectionAscendClient.renderHud(event.getGuiGraphics());
    }
}
