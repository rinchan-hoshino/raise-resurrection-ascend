package dev.rinchan.raiseresurrectionascend;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

/** Delivers a non-death message through Minecraft's death-chat audience rules. */
public final class DeathMessageDelivery {
    private DeathMessageDelivery() {
    }

    public static void broadcast(ServerPlayer subject, Component message) {
        if (!subject.level().getGameRules().get(GameRules.SHOW_DEATH_MESSAGES)) {
            return;
        }

        PlayerList players = subject.level().getServer().getPlayerList();
        PlayerTeam team = subject.getTeam();
        if (team == null || team.getDeathMessageVisibility() == Team.Visibility.ALWAYS) {
            players.broadcastSystemMessage(message, false);
        } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
            players.broadcastSystemToTeam(subject, message);
        } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
            players.broadcastSystemToAllExceptTeam(subject, message);
        }
    }
}
