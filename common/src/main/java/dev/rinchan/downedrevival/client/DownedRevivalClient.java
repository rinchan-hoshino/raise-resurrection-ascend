package dev.rinchan.downedrevival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rinchan.downedrevival.DownedRevivalConfig;
import dev.rinchan.downedrevival.DownedRevivalGiveUpPacket;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class DownedRevivalClient {
    private static final KeyMapping GIVE_UP_KEY = new KeyMapping(
        "key.downed_revival.give_up",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        "key.categories.downed_revival"
    );
    private static int heldTicks;
    private static boolean sentForCurrentHold;

    private DownedRevivalClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DownedRevivalClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(DownedRevivalClient::onClientTick);
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
        if (!sentForCurrentHold && heldTicks >= DownedRevivalConfig.giveUpHoldTicks.get()) {
            PacketDistributor.sendToServer(new DownedRevivalGiveUpPacket());
            sentForCurrentHold = true;
        }
    }
}
