package dev.rinchan.raiseresurrectionascend.client.fabric;

import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import dev.rinchan.raiseresurrectionascend.client.RaiseResurrectionAscendClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class RaiseResurrectionAscendFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(RaiseResurrectionAscendClient.GIVE_UP_CATEGORY_ID);
        KeyMapping giveUpKey = KeyMappingHelper.registerKeyMapping(RaiseResurrectionAscendClient.createGiveUpKey(category));
        RaiseResurrectionAscendClient.initializeGiveUp(giveUpKey, ClientPlayNetworking::send);
        ClientPlayNetworking.registerGlobalReceiver(
            RaiseResurrectionAscendStatePacket.TYPE,
            (packet, context) -> RaiseResurrectionAscendClient.applyState(packet)
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> RaiseResurrectionAscendClient.clientTick());
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("raise_resurrection_ascend", "downed_state"),
            (graphics, tickCounter) -> RaiseResurrectionAscendClient.renderHud(graphics)
        );
        ClientPlayConnectionEvents.INIT.register((handler, client) ->
            RaiseResurrectionAscendClient.resetState()
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            RaiseResurrectionAscendClient.resetState()
        );
    }
}
