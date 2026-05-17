package dev.rinchan.raiseresurrectionascend.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.tutorial.TutorialSteps;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ScreenshotClientHarness {
    private static int clientTicks;
    private static int inWorldTicks;
    private static boolean connecting;
    private static boolean firstCaptured;
    private static boolean secondCaptured;

    private ScreenshotClientHarness() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScreenshotClientHarness::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        clientTicks++;
        if (!connecting && minecraft.player == null && minecraft.level == null && clientTicks >= 40 && minecraft.screen != null) {
            connecting = true;
            ConnectScreen.startConnecting(
                minecraft.screen,
                minecraft,
                ServerAddress.parseString("localhost"),
                new ServerData("Raise Screenshot", "localhost", ServerData.Type.OTHER),
                false,
                null
            );
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.getMainRenderTarget() == null) {
            return;
        }
        inWorldTicks++;
        minecraft.options.tutorialStep = TutorialSteps.NONE;
        minecraft.options.hideGui = false;
        minecraft.options.pauseOnLostFocus = false;
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        minecraft.gui.getChat().clearMessages(false);
        minecraft.getToasts().clear();
        minecraft.player.setYRot(35.0F);
        minecraft.player.setXRot(18.0F);
        minecraft.player.getInventory().selected = 0;

        if (!firstCaptured && inWorldTicks >= 180) {
            save(minecraft, "raise-resurrection-ascend-downed.png");
            firstCaptured = true;
        }
        if (!secondCaptured && inWorldTicks >= 260) {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            save(minecraft, "raise-resurrection-ascend-totem-ready.png");
            secondCaptured = true;
        }
        if ((secondCaptured && inWorldTicks >= 320) || inWorldTicks >= 700) {
            minecraft.stop();
        }
    }

    private static void save(Minecraft minecraft, String fileName) {
        try {
            Path outputDir = Path.of(System.getProperty("raiseResurrectionAscend.screenshot.dir", minecraft.gameDirectory.getAbsolutePath()));
            Files.createDirectories(outputDir);
            try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                image.writeToFile(outputDir.resolve(fileName));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save screenshot " + fileName, e);
        }
    }
}
