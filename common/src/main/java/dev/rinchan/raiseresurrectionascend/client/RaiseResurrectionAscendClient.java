package dev.rinchan.raiseresurrectionascend.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendConfig;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendGiveUpPacket;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class RaiseResurrectionAscendClient {
    private static final KeyMapping GIVE_UP_KEY = new KeyMapping(
        "key.raise_resurrection_ascend.give_up",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        "key.categories.raise_resurrection_ascend"
    );
    private static int heldTicks;
    private static boolean sentForCurrentHold;

    private RaiseResurrectionAscendClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RaiseResurrectionAscendClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendClient::onClientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GIVE_UP_KEY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (!GIVE_UP_KEY.isDown()) {
            heldTicks = 0;
            sentForCurrentHold = false;
            return;
        }
        heldTicks++;
        if (!sentForCurrentHold && heldTicks >= RaiseResurrectionAscendConfig.giveUpHoldTicks.get()) {
            PacketDistributor.sendToServer(new RaiseResurrectionAscendGiveUpPacket());
            sentForCurrentHold = true;
        }
    }
}
