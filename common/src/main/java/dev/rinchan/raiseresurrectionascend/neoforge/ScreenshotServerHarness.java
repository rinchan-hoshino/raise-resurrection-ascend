package dev.rinchan.raiseresurrectionascend.neoforge;

import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscend;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

final class ScreenshotServerHarness {
    private static int ticks;
    private static boolean prepared;

    private ScreenshotServerHarness() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScreenshotServerHarness::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        ticks++;
        if (prepared || ticks < 80 || event.getServer().getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        ServerPlayer player = event.getServer().getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000);
        BlockPos base = BlockPos.containing(player.getX(), player.getY() - 1, player.getZ());
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                level.setBlock(base.offset(x, 0, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                level.setBlock(base.offset(x, 1, z), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(base.offset(x, 2, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setPos(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
        player.setYRot(35.0F);
        player.setXRot(15.0F);
        RaiseResurrectionAscend.enterDowned(player, player.damageSources().genericKill());
        prepared = true;
    }
}
