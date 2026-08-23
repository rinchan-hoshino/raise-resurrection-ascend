package dev.rinchan.raiseresurrectionascend;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RaiseResurrectionAscend {
    public static final String MOD_ID = "raise_resurrection_ascend";
    private static final float DOWNED_HEALTH = 1.0F;
    private static final int DOWNED_INVISIBILITY_TICKS = 20 * 3;
    private static final ResourceLocation DOWNED_ABSORPTION_CAPACITY =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "downed_absorption_capacity");
    private static final String DOWNED_PERSISTED_TAG = MOD_ID + ":downed";
    private static final String DOWNED_ABSORPTION_TAG = MOD_ID + ":remaining_absorption";
    private static final Map<UUID, DamageSource> DOWNING_SOURCES = new ConcurrentHashMap<>();
    private static final Set<UUID> FINAL_DEATH = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PENDING_FINAL_DEATH = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> AWAITING_DAMAGE_RESOLUTION = ConcurrentHashMap.newKeySet();

    private RaiseResurrectionAscend() {
    }

    public static boolean isDowned(ServerPlayer player) {
        return DOWNING_SOURCES.containsKey(player.getUUID());
    }

    public static void enterDowned(ServerPlayer player, DamageSource damageSource) {
        DOWNING_SOURCES.put(player.getUUID(), damageSource);
        player.setHealth(DOWNED_HEALTH);
        ensureDownedAbsorptionCapacity(player);
        player.setAbsorptionAmount(DownedAbsorptionPolicy.initialAbsorption(
            player.getAbsorptionAmount(),
            player.getMaxHealth()
        ));
        player.addEffect(new MobEffectInstance(
            MobEffects.INVISIBILITY, DOWNED_INVISIBILITY_TICKS, 0, false, true, true
        ));
        persistDownedState(player);
        beginCrawling(player);
        syncState(player, true);
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
            if (resolveDamageProtection(player)) {
                continue;
            }
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
            if (recoverIfThresholdReached(player)) {
                continue;
            }
            player.setSprinting(false);
            DownedAbsorptionPolicy.DrainResult result = DownedAbsorptionPolicy.drain(
                player.getAbsorptionAmount(),
                player.getMaxHealth()
            );
            player.setAbsorptionAmount(result.absorption());
            persistDownedState(player);
            if (result.expired()) {
                dieNow(player);
            }
        }
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

    public static boolean recoverIfThresholdReached(ServerPlayer player) {
        if (isPendingFinalDeath(player)
                || !isDowned(player)
                || !DownedRecoveryPolicy.canRecover(player.getHealth(), player.getMaxHealth())) {
            return false;
        }
        clearDownedState(player);
        return true;
    }

    public static boolean reviveByCommand(ServerPlayer player) {
        if (!isDowned(player)) {
            return false;
        }
        player.setHealth(player.getMaxHealth());
        return recoverIfThresholdReached(player);
    }

    public static void giveUp(ServerPlayer player) {
        if (isDowned(player)) {
            dieNow(player);
        }
    }

    public static void suspendPlayer(ServerPlayer player) {
        if (isDowned(player)) {
            persistDownedState(player);
        }
        DOWNING_SOURCES.remove(player.getUUID());
        FINAL_DEATH.remove(player.getUUID());
        PENDING_FINAL_DEATH.remove(player.getUUID());
        AWAITING_DAMAGE_RESOLUTION.remove(player.getUUID());
        removeDownedAbsorptionCapacity(player);
        clearForcedCrawling(player);
    }

    public static void restorePlayer(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(DOWNED_PERSISTED_TAG)) {
            return;
        }
        float remainingAbsorption = player.getPersistentData().getFloat(DOWNED_ABSORPTION_TAG);
        DOWNING_SOURCES.put(player.getUUID(), player.damageSources().genericKill());
        player.setHealth(Math.max(DOWNED_HEALTH, player.getHealth()));
        ensureDownedAbsorptionCapacity(player);
        player.setAbsorptionAmount(Math.max(0.0F, remainingAbsorption));
        beginCrawling(player);
        syncState(player, true);
        if (remainingAbsorption <= 0.0F) {
            PENDING_FINAL_DEATH.add(player.getUUID());
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        DOWNING_SOURCES.remove(player.getUUID());
        FINAL_DEATH.remove(player.getUUID());
        PENDING_FINAL_DEATH.remove(player.getUUID());
        AWAITING_DAMAGE_RESOLUTION.remove(player.getUUID());
        clearDownedAbsorption(player);
        clearForcedCrawling(player);
        clearPersistedDownedState(player);
    }

    public static void awaitDamageResolution(ServerPlayer player) {
        if (isDowned(player) && !isFinalDeath(player)) {
            AWAITING_DAMAGE_RESOLUTION.add(player.getUUID());
        }
    }

    public static boolean resolveDamageProtection(ServerPlayer player) {
        if (!AWAITING_DAMAGE_RESOLUTION.remove(player.getUUID())
                || !isDowned(player)
                || !player.isAlive()) {
            return false;
        }
        clearDownedState(player, true);
        return true;
    }

    public static void finishDownedDeath(ServerPlayer player) {
        if (isDowned(player)) {
            clearDownedState(player);
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
        clearDownedState(player, false);
    }

    private static void clearDownedState(ServerPlayer player, boolean preserveAbsorption) {
        PENDING_FINAL_DEATH.remove(player.getUUID());
        AWAITING_DAMAGE_RESOLUTION.remove(player.getUUID());
        if (DOWNING_SOURCES.remove(player.getUUID()) != null) {
            if (preserveAbsorption) {
                removeDownedAbsorptionCapacity(player);
            } else {
                clearDownedAbsorption(player);
            }
        }
        clearForcedCrawling(player);
        clearPersistedDownedState(player);
        syncState(player, false);
    }

    private static void ensureDownedAbsorptionCapacity(ServerPlayer player) {
        AttributeInstance capacity = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (capacity != null && !capacity.hasModifier(DOWNED_ABSORPTION_CAPACITY)) {
            capacity.addTransientModifier(new AttributeModifier(
                DOWNED_ABSORPTION_CAPACITY,
                DownedAbsorptionPolicy.generatedAbsorption(player.getMaxHealth()),
                AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    private static void clearDownedAbsorption(ServerPlayer player) {
        player.setAbsorptionAmount(0.0F);
        removeDownedAbsorptionCapacity(player);
    }

    private static void removeDownedAbsorptionCapacity(ServerPlayer player) {
        AttributeInstance capacity = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (capacity != null) {
            capacity.removeModifier(DOWNED_ABSORPTION_CAPACITY);
        }
    }

    private static void persistDownedState(ServerPlayer player) {
        player.getPersistentData().putBoolean(DOWNED_PERSISTED_TAG, true);
        player.getPersistentData().putFloat(DOWNED_ABSORPTION_TAG, player.getAbsorptionAmount());
    }

    private static void clearPersistedDownedState(ServerPlayer player) {
        player.getPersistentData().remove(DOWNED_PERSISTED_TAG);
        player.getPersistentData().remove(DOWNED_ABSORPTION_TAG);
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
