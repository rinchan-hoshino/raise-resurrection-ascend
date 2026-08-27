package dev.rinchan.raiseresurrectionascend.client;

import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class RaiseResurrectionAscendClient {
    private static boolean downed;
    private static float recoveryThreshold;
    private static boolean appliedForcedPose;

    private RaiseResurrectionAscendClient() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendClient::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendClient::onLoggingOut);
    }

    public static void applyState(RaiseResurrectionAscendStatePacket packet) {
        downed = packet.downed();
        recoveryThreshold = packet.recoveryThreshold();
        applyLocalPose();
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        resetState();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetState();
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            resetState();
            return;
        }
        applyLocalPose();
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!downed || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        int centerX = event.getGuiGraphics().guiWidth() / 2;
        int centerY = event.getGuiGraphics().guiHeight() / 2 + 32;
        Component recovery = Component.translatable(
            "hud.raise_resurrection_ascend.recovery",
            formatHealth(recoveryThreshold)
        );
        int width = minecraft.font.width(recovery) + 12;
        event.getGuiGraphics().fill(centerX - width / 2, centerY - 5, centerX + width / 2, centerY + 13, 0xA0000000);
        event.getGuiGraphics().drawCenteredString(minecraft.font, recovery, centerX, centerY, 0xFFFFFF);
    }

    private static String formatHealth(float health) {
        if (health == Math.rint(health)) {
            return Integer.toString(Math.round(health));
        }
        return String.format(Locale.ROOT, "%.1f", health);
    }

    private static void applyLocalPose() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (downed) {
            if (minecraft.player.getForcedPose() != Pose.SWIMMING) {
                minecraft.player.setForcedPose(Pose.SWIMMING);
            }
            minecraft.player.setPose(Pose.SWIMMING);
            minecraft.player.setSprinting(false);
            appliedForcedPose = true;
        } else if (appliedForcedPose) {
            if (minecraft.player.getForcedPose() == Pose.SWIMMING) {
                minecraft.player.setForcedPose(null);
            }
            appliedForcedPose = false;
        }
    }

    private static void resetState() {
        downed = false;
        recoveryThreshold = 0.0F;
        applyLocalPose();
        appliedForcedPose = false;
    }
}
