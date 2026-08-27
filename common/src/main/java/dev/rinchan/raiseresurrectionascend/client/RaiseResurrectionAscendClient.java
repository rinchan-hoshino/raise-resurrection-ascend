package dev.rinchan.raiseresurrectionascend.client;

import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;

/** Loader-neutral owner of the local downed presentation. */
public final class RaiseResurrectionAscendClient {
    private static boolean downed;
    private static float recoveryThreshold;

    private RaiseResurrectionAscendClient() {
    }

    public static void applyState(RaiseResurrectionAscendStatePacket packet) {
        downed = packet.downed();
        recoveryThreshold = packet.recoveryThreshold();
        applyLocalPose();
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            resetState();
            return;
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
        int width = minecraft.font.width(recovery) + 12;
        graphics.fill(centerX - width / 2, centerY - 5, centerX + width / 2, centerY + 13, 0xA0000000);
        graphics.drawCenteredString(minecraft.font, recovery, centerX, centerY, 0xFFFFFF);
    }

    public static void resetState() {
        downed = false;
        recoveryThreshold = 0.0F;
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
