package dev.rinchan.raiseresurrectionascend.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.rinchan.raiseresurrectionascend.GiveUpHoldState;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendGiveUpInputPacket;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/** Loader-neutral owner of the local downed presentation and key transitions. */
public final class RaiseResurrectionAscendClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final KeyMapping GIVE_UP_KEY = new KeyMapping(
        "key.raise_resurrection_ascend.give_up",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        "key.categories.raise_resurrection_ascend"
    );

    private static GiveUpInputSender giveUpSender;
    private static boolean downed;
    private static float recoveryThreshold;
    private static boolean reportedPressed;
    private static int localHeldTicks;

    private RaiseResurrectionAscendClient() {
    }

    public static void initialize(GiveUpInputSender sender) {
        giveUpSender = sender;
    }

    public static KeyMapping giveUpKey() {
        return GIVE_UP_KEY;
    }

    public static void applyState(RaiseResurrectionAscendStatePacket packet) {
        downed = packet.downed();
        recoveryThreshold = packet.recoveryThreshold();
        if (!downed) {
            reportedPressed = false;
            localHeldTicks = 0;
        }
        applyLocalPose();
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            resetState();
            return;
        }

        boolean pressed = downed && GIVE_UP_KEY.isDown();
        if (pressed != reportedPressed) {
            sendGiveUpTransition(pressed);
            reportedPressed = pressed;
        }
        if (pressed) {
            localHeldTicks = Math.min(GiveUpHoldState.REQUIRED_TICKS, localHeldTicks + 1);
        } else {
            localHeldTicks = 0;
        }
        applyLocalPose();
    }

    public static void renderHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!downed || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2 + 32;
        Component recovery = Component.translatable(
            "hud.raise_resurrection_ascend.recovery",
            formatHealth(recoveryThreshold)
        );
        Component giveUp = Component.translatable(
            "hud.raise_resurrection_ascend.give_up",
            GIVE_UP_KEY.getTranslatedKeyMessage(),
            GiveUpHoldState.REQUIRED_TICKS / 20
        );
        int width = Math.max(minecraft.font.width(recovery), minecraft.font.width(giveUp)) + 12;
        graphics.fill(centerX - width / 2, centerY - 5, centerX + width / 2, centerY + 27, 0xA0000000);
        graphics.drawCenteredString(minecraft.font, recovery, centerX, centerY, 0xFFFFFF);
        graphics.drawCenteredString(minecraft.font, giveUp, centerX, centerY + 12, 0xFFB0B0);
        if (localHeldTicks > 0) {
            int barWidth = Math.max(1, width - 12);
            int filled = Math.round(barWidth * (localHeldTicks / (float) GiveUpHoldState.REQUIRED_TICKS));
            graphics.fill(centerX - barWidth / 2, centerY + 23, centerX + barWidth / 2, centerY + 25, 0xFF303030);
            graphics.fill(centerX - barWidth / 2, centerY + 23, centerX - barWidth / 2 + filled, centerY + 25, 0xFFE05050);
        }
    }

    public static void resetState() {
        downed = false;
        recoveryThreshold = 0.0F;
        reportedPressed = false;
        localHeldTicks = 0;
    }

    private static void sendGiveUpTransition(boolean pressed) {
        if (giveUpSender == null) {
            LOGGER.error("Cannot send give-up input before client networking is initialized");
            return;
        }
        try {
            giveUpSender.send(new RaiseResurrectionAscendGiveUpInputPacket(pressed));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send give-up input transition", exception);
            throw exception;
        }
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
            minecraft.player.setPose(Pose.SWIMMING);
            minecraft.player.setSprinting(false);
        }
    }
}
