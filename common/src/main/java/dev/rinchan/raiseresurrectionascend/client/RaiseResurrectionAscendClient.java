package dev.rinchan.raiseresurrectionascend.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendConfig;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendGiveUpPacket;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
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
    private static final int PROGRESS_BAR_WIDTH = 128;
    private static final int PROGRESS_BAR_HEIGHT = 4;
    private static int heldTicks;
    private static boolean downed;
    private static boolean sentForCurrentHold;
    private static boolean appliedForcedPose;

    private RaiseResurrectionAscendClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RaiseResurrectionAscendClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RaiseResurrectionAscendClient::onRenderGui);
    }

    public static void applyState(RaiseResurrectionAscendStatePacket packet) {
        downed = packet.downed();
        heldTicks = 0;
        sentForCurrentHold = false;
        applyLocalPose();
    }

    static void setHeldTicksForScreenshot(int ticks) {
        heldTicks = ticks;
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GIVE_UP_KEY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            downed = false;
            heldTicks = 0;
            sentForCurrentHold = false;
            appliedForcedPose = false;
            return;
        }

        applyLocalPose();
        if (!downed) {
            heldTicks = 0;
            sentForCurrentHold = false;
            return;
        }
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

    private static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!downed || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        int centerX = event.getGuiGraphics().guiWidth() / 2;
        int baseY = event.getGuiGraphics().guiHeight() / 2 + 32;
        Component recovery = Component.translatable("hud.raise_resurrection_ascend.recovery");
        Component giveUp = Component.translatable(
            "hud.raise_resurrection_ascend.give_up",
            GIVE_UP_KEY.getTranslatedKeyMessage()
        );

        int width = Math.max(
            PROGRESS_BAR_WIDTH,
            Math.max(minecraft.font.width(recovery), minecraft.font.width(giveUp))
        ) + 12;
        int barLeft = centerX - PROGRESS_BAR_WIDTH / 2;
        int barTop = baseY + 24;
        int progressWidth = Math.round(PROGRESS_BAR_WIDTH * GiveUpHoldProgress.ratio(
            heldTicks,
            RaiseResurrectionAscendConfig.giveUpHoldTicks.get()
        ));

        event.getGuiGraphics().fill(centerX - width / 2, baseY - 5, centerX + width / 2, barTop + 9, 0xA0000000);
        event.getGuiGraphics().drawCenteredString(minecraft.font, recovery, centerX, baseY, 0xFFFFFF);
        event.getGuiGraphics().drawCenteredString(minecraft.font, giveUp, centerX, baseY + 12, 0xB8B8B8);
        event.getGuiGraphics().fill(
            barLeft,
            barTop,
            barLeft + PROGRESS_BAR_WIDTH,
            barTop + PROGRESS_BAR_HEIGHT,
            0xFF4A4A4A
        );
        if (progressWidth > 0) {
            event.getGuiGraphics().fill(
                barLeft,
                barTop,
                barLeft + progressWidth,
                barTop + PROGRESS_BAR_HEIGHT,
                0xFFE14B4B
            );
        }
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
}
