package dev.rinchan.raiseresurrectionascend;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RaiseResurrectionAscend {
    public static final String MOD_ID = "raise_resurrection_ascend";
    private static final float DOWNED_HEALTH = 1.0F;
    private static final Map<UUID, DamageSource> DOWNING_SOURCES = new ConcurrentHashMap<>();
    private static final Set<UUID> FINAL_DEATH = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PENDING_FINAL_DEATH = ConcurrentHashMap.newKeySet();

    private RaiseResurrectionAscend() {
    }

    public static boolean isDowned(ServerPlayer player) {
        return DOWNING_SOURCES.containsKey(player.getUUID());
    }

    public static void enterDowned(ServerPlayer player, DamageSource damageSource) {
        DOWNING_SOURCES.put(player.getUUID(), damageSource);
        player.setHealth(DOWNED_HEALTH);
        player.setAbsorptionAmount(DownedAbsorptionPolicy.initialAbsorption(player.getAbsorptionAmount()));
        beginCrawling(player);
        syncState(player, true);
        player.displayClientMessage(Component.translatable("message.raise_resurrection_ascend.downed_self"), false);
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player) {
                other.displayClientMessage(
                    Component.translatable("message.raise_resurrection_ascend.downed_other", player.getDisplayName()),
                    false
                );
            }
        }
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (PENDING_FINAL_DEATH.remove(player.getUUID())) {
                dieNow(player);
                continue;
            }
            if (!isDowned(player)) {
                continue;
            }
            if (!player.isAlive()) {
                clearDownedState(player);
                continue;
            }
            if (recoverIfFullyHealed(player)) {
                continue;
            }
            player.setSprinting(false);
            DownedAbsorptionPolicy.DrainResult result = DownedAbsorptionPolicy.drain(player.getAbsorptionAmount());
            player.setAbsorptionAmount(result.absorption());
            if (result.expired()) {
                dieNow(player);
            }
        }
    }

    public static boolean shouldEnterDowned(ServerPlayer player, float damage) {
        return !isFinalDeath(player) && !isDowned(player) && player.getHealth() - damage <= 0F;
    }

    public static boolean isFinalDeath(ServerPlayer player) {
        UUID playerId = player.getUUID();
        return FINAL_DEATH.contains(playerId) || PENDING_FINAL_DEATH.contains(playerId);
    }

    public static boolean isPendingFinalDeath(ServerPlayer player) {
        return PENDING_FINAL_DEATH.contains(player.getUUID());
    }

    public static void clearFinalDeath(ServerPlayer player) {
        FINAL_DEATH.remove(player.getUUID());
    }

    public static boolean recoverIfFullyHealed(ServerPlayer player) {
        if (isPendingFinalDeath(player)
                || !isDowned(player)
                || !DownedRecoveryPolicy.isFullyHealed(player.getHealth(), player.getMaxHealth())) {
            return false;
        }
        clearDownedState(player);
        player.displayClientMessage(Component.translatable("message.raise_resurrection_ascend.recovered"), false);
        return true;
    }

    public static boolean reviveByCommand(ServerPlayer player) {
        if (!isDowned(player)) {
            return false;
        }
        player.setHealth(player.getMaxHealth());
        return recoverIfFullyHealed(player);
    }

    public static boolean tryFeedRecoveryItem(ServerPlayer feeder, ServerPlayer target, InteractionHand hand) {
        ItemStack itemStack = feeder.getItemInHand(hand);
        var fedItem = itemStack.getItem();
        RecoveryFeedingPolicy.ItemKind itemKind = itemStack.is(Items.APPLE)
            ? RecoveryFeedingPolicy.ItemKind.APPLE
            : RecoveryFeedingPolicy.ItemKind.UNSUPPORTED;
        RecoveryFeedingPolicy.Result result = RecoveryFeedingPolicy.resolve(
            isDowned(target),
            itemKind,
            itemStack.getCount(),
            feeder.hasInfiniteMaterials()
        );
        if (!result.accepted()) {
            return false;
        }

        target.heal(result.healing());
        if (!feeder.hasInfiniteMaterials()) {
            itemStack.shrink(1);
        }

        feeder.awardStat(Stats.ITEM_USED.get(fedItem));
        target.level().playSound(
            null,
            target.getX(),
            target.getY(),
            target.getZ(),
            SoundEvents.GENERIC_EAT,
            SoundSource.PLAYERS,
            0.5F,
            1.0F
        );
        target.gameEvent(GameEvent.EAT);
        recoverIfFullyHealed(target);
        return true;
    }

    public static void giveUp(ServerPlayer player) {
        if (isDowned(player)) {
            dieNow(player);
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        if (DOWNING_SOURCES.remove(player.getUUID()) != null) {
            player.setAbsorptionAmount(0.0F);
        }
        FINAL_DEATH.remove(player.getUUID());
        PENDING_FINAL_DEATH.remove(player.getUUID());
        clearForcedCrawling(player);
    }

    public static void finishDownedFromDamage(ServerPlayer player) {
        if (isDowned(player) && !FINAL_DEATH.contains(player.getUUID())) {
            PENDING_FINAL_DEATH.add(player.getUUID());
        }
    }

    private static void dieNow(ServerPlayer player) {
        if (!isDowned(player)) {
            return;
        }
        DamageSource downingSource = DOWNING_SOURCES.getOrDefault(
            player.getUUID(),
            player.damageSources().genericKill()
        );
        player.getCombatTracker().recordDamage(downingSource, player.getHealth());
        clearDownedState(player);
        FINAL_DEATH.add(player.getUUID());
        try {
            player.setHealth(0.0F);
            player.die(downingSource);
        } finally {
            FINAL_DEATH.remove(player.getUUID());
        }
    }

    private static void beginCrawling(ServerPlayer player) {
        player.setForcedPose(Pose.SWIMMING);
        player.setPose(Pose.SWIMMING);
        player.setSprinting(false);
    }

    private static void clearDownedState(ServerPlayer player) {
        PENDING_FINAL_DEATH.remove(player.getUUID());
        if (DOWNING_SOURCES.remove(player.getUUID()) != null) {
            player.setAbsorptionAmount(0.0F);
        }
        clearForcedCrawling(player);
        syncState(player, false);
    }

    private static void clearForcedCrawling(ServerPlayer player) {
        if (player.getForcedPose() == Pose.SWIMMING) {
            player.setForcedPose(null);
        }
    }

    private static void syncState(ServerPlayer player, boolean downed) {
        PacketDistributor.sendToPlayer(player, new RaiseResurrectionAscendStatePacket(downed));
    }
}
