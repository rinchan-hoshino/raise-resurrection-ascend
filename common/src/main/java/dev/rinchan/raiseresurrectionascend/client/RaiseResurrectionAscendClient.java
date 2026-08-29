package dev.rinchan.raiseresurrectionascend.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rinchan.raiseresurrectionascend.DownedRecoveryPolicy;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscend;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendGiveUpPacket;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Pose;
import org.lwjgl.glfw.GLFW;

/** Loader-neutral owner of the local downed presentation and key transitions. */
public final class RaiseResurrectionAscendClient {
    public static final Identifier GIVE_UP_CATEGORY_ID = Identifier.fromNamespaceAndPath(
        RaiseResurrectionAscend.MOD_ID,
        "main"
    );
    private static final int GIVE_UP_HOLD_TICKS = 40;
    private static KeyMapping giveUpKey;
    private static Consumer<RaiseResurrectionAscendGiveUpPacket> giveUpSender;
    private static boolean downed;
    private static boolean lastSentGiveUpPressed;
    private static float recoveryThreshold;
    private static Component deathMessage = Component.empty();
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
        recoveryThreshold = downed ? packet.recoveryThreshold() : 0.0F;
        deathMessage = downed ? packet.deathMessage().copy() : Component.empty();
        if (!downed) {
            updateGiveUpInput(false);
        }
        applyLocalPose();
    }

    public static void resetState() {
        downed = false;
        recoveryThreshold = 0.0F;
        deathMessage = Component.empty();
        clientGiveUpTicks = 0;
        lastSentGiveUpPressed = false;
    }

    public static void clientTick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            resetState();
            return;
        }
        applyLocalPose();
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
        int centerY = graphics.guiHeight() / 2 + 24;
        Component cause = Component.translatable("hud.raise_resurrection_ascend.cause", deathMessage);
        Component recovery = Component.translatable(DownedRecoveryPolicy.requiresFullHealth(recoveryThreshold)
            ? "hud.raise_resurrection_ascend.recovery_full_hearts"
            : "hud.raise_resurrection_ascend.recovery_ten_hearts");
        Component giveUp = Component.translatable(
            "hud.raise_resurrection_ascend.give_up",
            giveUpKey.getTranslatedKeyMessage(),
            GIVE_UP_HOLD_TICKS / 20
        );
        int maxLineWidth = Math.max(80, Math.min(320, graphics.guiWidth() - 24));
        List<FormattedCharSequence> causeLines = client.font.split(cause, maxLineWidth);
        int causeLineCount = Math.min(2, causeLines.size());
        int width = Math.max(client.font.width(recovery), client.font.width(giveUp));
        for (int index = 0; index < causeLineCount; index++) {
            width = Math.max(width, client.font.width(causeLines.get(index)));
        }
        width += 12;
        int recoveryY = centerY + causeLineCount * 12;
        int giveUpY = recoveryY + 12;
        int barY = giveUpY + 11;
        graphics.fill(centerX - width / 2, centerY - 5, centerX + width / 2, barY + 4, 0xA0000000);
        for (int index = 0; index < causeLineCount; index++) {
            graphics.centeredText(client.font, causeLines.get(index), centerX, centerY + index * 12, 0xFF8080);
        }
        graphics.centeredText(client.font, recovery, centerX, recoveryY, 0xFFFFFF);
        graphics.centeredText(client.font, giveUp, centerX, giveUpY, 0xFFB0B0);
        if (clientGiveUpTicks > 0) {
            int barWidth = Math.max(1, width - 12);
            int filled = Math.round(barWidth * (clientGiveUpTicks / (float) GIVE_UP_HOLD_TICKS));
            graphics.fill(centerX - barWidth / 2, barY, centerX + barWidth / 2, barY + 2, 0xFF303030);
            graphics.fill(centerX - barWidth / 2, barY, centerX - barWidth / 2 + filled, barY + 2, 0xFFE05050);
        }
    }

    private static void applyLocalPose() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (downed) {
            client.player.setPose(Pose.SWIMMING);
            client.player.setSprinting(false);
        }
    }
}
