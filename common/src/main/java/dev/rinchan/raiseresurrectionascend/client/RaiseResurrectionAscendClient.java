package dev.rinchan.raiseresurrectionascend.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscend;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendGiveUpPacket;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import org.lwjgl.glfw.GLFW;

public final class RaiseResurrectionAscendClient {
    public static final Identifier GIVE_UP_CATEGORY_ID = Identifier.fromNamespaceAndPath(
        RaiseResurrectionAscend.MOD_ID,
        "main"
    );
    private static final int GIVE_UP_HOLD_TICKS = 40;
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 6;
    private static KeyMapping giveUpKey;
    private static Consumer<RaiseResurrectionAscendGiveUpPacket> giveUpSender;
    private static boolean downed;
    private static boolean lastSentGiveUpPressed;
    private static float recoveryThreshold;
    private static int clientGiveUpTicks;

    private RaiseResurrectionAscendClient() {
    }

    public static KeyMapping createGiveUpKey(KeyMapping.Category category) {
        return new KeyMapping(
            "key.raise_resurrection_ascend.give_up",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            category
        );
    }

    public static void initializeGiveUp(KeyMapping keyMapping, Consumer<RaiseResurrectionAscendGiveUpPacket> sender) {
        if (giveUpKey != null || giveUpSender != null) {
            throw new IllegalStateException("RRA client give-up services were initialized twice");
        }
        giveUpKey = keyMapping;
        giveUpSender = sender;
    }

    public static void applyState(RaiseResurrectionAscendStatePacket packet) {
        downed = packet.downed();
        recoveryThreshold = packet.downed() ? packet.recoveryThreshold() : 0.0F;
        if (!downed) {
            updateGiveUpInput(false);
        }
    }

    public static void resetState() {
        downed = false;
        recoveryThreshold = 0.0F;
        clientGiveUpTicks = 0;
        lastSentGiveUpPressed = false;
    }

    public static void clientTick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            resetState();
            return;
        }
        if (downed && client.player.getPose() != Pose.SWIMMING) {
            client.player.setPose(Pose.SWIMMING);
            client.player.setSprinting(false);
        }
        updateGiveUpInput(downed && giveUpKey != null && giveUpKey.isDown());
    }

    private static void updateGiveUpInput(boolean pressed) {
        clientGiveUpTicks = pressed ? Math.min(GIVE_UP_HOLD_TICKS, clientGiveUpTicks + 1) : 0;
        if (giveUpSender != null && pressed != lastSentGiveUpPressed) {
            giveUpSender.accept(new RaiseResurrectionAscendGiveUpPacket(pressed));
            lastSentGiveUpPressed = pressed;
        }
    }

    public static void renderHud(GuiGraphicsExtractor graphics) {
        if (!downed) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.gui.hud.isHidden() || client.player == null) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int textY = graphics.guiHeight() / 2 + 16;
        graphics.centeredText(
            client.font,
            Component.translatable("hud.raise_resurrection_ascend.recovery", formatHealth(recoveryThreshold)),
            centerX,
            textY,
            0xFFFFAA00
        );
        graphics.centeredText(
            client.font,
            Component.translatable("hud.raise_resurrection_ascend.give_up"),
            centerX,
            textY + 14,
            0xFFFFFFFF
        );

        int barX = centerX - BAR_WIDTH / 2;
        int barY = textY + 27;
        int progress = Math.round((BAR_WIDTH - 2) * (clientGiveUpTicks / (float) GIVE_UP_HOLD_TICKS));
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xAA000000);
        if (progress > 0) {
            graphics.fill(barX + 1, barY + 1, barX + 1 + progress, barY + BAR_HEIGHT - 1, 0xFFE53935);
        }
    }

    private static String formatHealth(float health) {
        if (health == Math.rint(health)) {
            return Integer.toString(Math.round(health));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", health);
    }
}
