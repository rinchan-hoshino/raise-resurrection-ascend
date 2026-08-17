package dev.rinchan.raiseresurrectionascend.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendConfig;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendGiveUpPacket;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import java.util.Locale;
import net.minecraft.ChatFormatting;
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
    private static int heldTicks;
    private static int remainingTicks;
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
        remainingTicks = Math.max(0, packet.remainingTicks());
        heldTicks = 0;
        sentForCurrentHold = false;
        applyLocalPose();
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GIVE_UP_KEY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            downed = false;
            remainingTicks = 0;
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
        if (remainingTicks > 0) {
            remainingTicks--;
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
        int baseY = event.getGuiGraphics().guiHeight() / 2 + 40;
        int seconds = (remainingTicks + 19) / 20;
        String remaining = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        Component title = Component.translatable("hud.raise_resurrection_ascend.downed", remaining)
            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component recovery = Component.translatable("hud.raise_resurrection_ascend.recovery")
            .withStyle(ChatFormatting.WHITE);
        Component giveUp = Component.translatable(
            "hud.raise_resurrection_ascend.give_up",
            GIVE_UP_KEY.getTranslatedKeyMessage(),
            Math.max(1, RaiseResurrectionAscendConfig.giveUpHoldTicks.get() / 20)
        ).withStyle(ChatFormatting.GRAY);

        int width = Math.max(
            minecraft.font.width(title),
            Math.max(minecraft.font.width(recovery), minecraft.font.width(giveUp))
        ) + 12;
        event.getGuiGraphics().fill(centerX - width / 2, baseY - 5, centerX + width / 2, baseY + 31, 0xA0000000);
        event.getGuiGraphics().drawCenteredString(minecraft.font, title, centerX, baseY, 0xFFFFFF);
        event.getGuiGraphics().drawCenteredString(minecraft.font, recovery, centerX, baseY + 11, 0xFFFFFF);
        event.getGuiGraphics().drawCenteredString(minecraft.font, giveUp, centerX, baseY + 22, 0xFFFFFF);
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
