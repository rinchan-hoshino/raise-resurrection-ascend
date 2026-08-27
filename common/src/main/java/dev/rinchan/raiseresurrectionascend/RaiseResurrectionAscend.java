package dev.rinchan.raiseresurrectionascend;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

public final class RaiseResurrectionAscend {
    public static final String MOD_ID = "raise_resurrection_ascend";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PERSISTED_STATE = MOD_ID + ":downed_state";
    private static final String PERSISTED_CAUSE = "cause";
    private static final String PERSISTED_ABSORPTION = "remaining_absorption";
    private static final String LEGACY_DOWNED = MOD_ID + ":downed";
    private static final String LEGACY_ABSORPTION = MOD_ID + ":remaining_absorption";
    private static final float DOWNED_HEALTH = 1.0F;
    private static final float ABSORPTION_TOLERANCE = 0.001F;
    private static final int DOWNED_INVISIBILITY_TICKS = 20 * 3;
    private static final ResourceLocation DOWNED_ABSORPTION_CAPACITY = ResourceLocation.fromNamespaceAndPath(
        MOD_ID,
        "downed_absorption_capacity"
    );
    private static final Map<UUID, DownedState> DOWNED_PLAYERS = new HashMap<>();

    private RaiseResurrectionAscend() {
    }

    public static boolean isDowned(ServerPlayer player) {
        return DOWNED_PLAYERS.containsKey(player.getUUID());
    }

    public static boolean isDispatchingFinalDeath(ServerPlayer player) {
        DownedState state = DOWNED_PLAYERS.get(player.getUUID());
        return state != null && state.finalDeath.phase() == FinalDeathStateMachine.Phase.DISPATCHING;
    }

    public static boolean enterDowned(ServerPlayer player, DamageSource damageSource) {
        if (isDowned(player)) {
            return false;
        }
        DowningCauseSnapshot cause = DowningCauseSnapshot.capture(player, damageSource);
        if (cause == null) {
            return false;
        }

        DownedState state = new DownedState(cause);
        DOWNED_PLAYERS.put(player.getUUID(), state);
        player.setHealth(DOWNED_HEALTH);
        ensureDownedAbsorptionCapacity(player);
        player.setAbsorptionAmount(DownedAbsorptionPolicy.initialAbsorption(
            player.getAbsorptionAmount(),
            player.getMaxHealth()
        ));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DOWNED_INVISIBILITY_TICKS));
        beginCrawling(player);
        persistDownedState(player, state);
        syncState(player, state, true);
        DeathMessageDelivery.broadcast(
            player,
            Component.translatable("message.raise_resurrection_ascend.downed_other", player.getDisplayName())
        );
        return true;
    }

    /** Caps a finishing follow-up at the absorption boundary so the original source can be dispatched next. */
    public static float adjustDownedDamage(ServerPlayer player, float damage) {
        DownedState state = DOWNED_PLAYERS.get(player.getUUID());
        if (state == null || state.finalDeath.phase() == FinalDeathStateMachine.Phase.DISPATCHING) {
            return damage;
        }
        if (!DownedDamagePolicy.finishesDownedState(player.getAbsorptionAmount(), damage)) {
            return damage;
        }
        state.boundaryDamagePending = true;
        return DownedDamagePolicy.damageBeforeOriginalDispatch(player.getAbsorptionAmount(), damage);
    }

    /** Runs after the capped damage sequence, outside the native hurt recursion. */
    public static void resolveDownedDamage(ServerPlayer player) {
        DownedState state = DOWNED_PLAYERS.get(player.getUUID());
        if (state == null || !state.boundaryDamagePending) {
            return;
        }
        state.boundaryDamagePending = false;
        if (player.getAbsorptionAmount() <= ABSORPTION_TOLERANCE) {
            state.finalDeath.requestFinalDeath();
            persistDownedState(player, state);
        }
    }

    /** Called by the lowest-priority, non-canceled native totem hook. */
    public static void observeNativeTotemTrigger(ServerPlayer player) {
        DownedState state = DOWNED_PLAYERS.get(player.getUUID());
        if (state != null) {
            state.finalDeath.observeTotemTrigger();
        }
    }

    /** Called by the lowest-priority, non-canceled native death event. */
    public static void observeFinalDeath(ServerPlayer player) {
        DownedState state = DOWNED_PLAYERS.get(player.getUUID());
        if (state != null) {
            state.finalDeath.observeFinalDeath();
        }
    }

    /** Reroutes any non-dispatch death of a downed player through the original source. */
    public static void requestOriginalFinalDeath(ServerPlayer player) {
        DownedState state = DOWNED_PLAYERS.get(player.getUUID());
        if (state == null) {
            return;
        }
        player.setHealth(DOWNED_HEALTH);
        state.finalDeath.requestFinalDeath();
        persistDownedState(player, state);
    }

    public static boolean tryFeedTotem(ServerPlayer feeder, ServerPlayer recipient, InteractionHand hand) {
        if (feeder == recipient) {
            return false;
        }
        DownedState state = DOWNED_PLAYERS.get(recipient.getUUID());
        if (state == null) {
            return false;
        }
        if (!TotemProtection.tryApplyFromFeeder(feeder, recipient, hand)) {
            return false;
        }
        clearDownedState(recipient, true);
        return true;
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DownedState state = DOWNED_PLAYERS.get(player.getUUID());
            if (state == null) {
                continue;
            }

            if (DownedRecoveryPolicy.canRecover(player.getHealth(), player.getMaxHealth())) {
                clearDownedState(player, false);
                continue;
            }
            if (state.finalDeath.phase() == FinalDeathStateMachine.Phase.REQUESTED) {
                dispatchFinalDeath(player, state);
                continue;
            }

            beginCrawling(player);
            syncState(player, state, false);
            DownedAbsorptionPolicy.DrainResult result = DownedAbsorptionPolicy.drain(
                player.getAbsorptionAmount(),
                player.getMaxHealth()
            );
            player.setAbsorptionAmount(result.absorption());
            if (result.expired()) {
                state.finalDeath.requestFinalDeath();
            }
            persistDownedState(player, state);
        }
    }

    public static void suspendPlayer(ServerPlayer player) {
        DownedState state = DOWNED_PLAYERS.get(player.getUUID());
        if (state == null) {
            return;
        }
        persistDownedState(player, state);
        DOWNED_PLAYERS.remove(player.getUUID());
        stopCrawling(player);
        removeDownedAbsorptionCapacity(player);
    }

    public static void restorePlayer(ServerPlayer player) {
        DOWNED_PLAYERS.remove(player.getUUID());
        CompoundTag persistent = player.getPersistentData();
        DownedState state;
        float remainingAbsorption;
        if (persistent.contains(PERSISTED_STATE, Tag.TAG_COMPOUND)) {
            CompoundTag saved = persistent.getCompound(PERSISTED_STATE);
            DowningCauseSnapshot cause = saved.contains(PERSISTED_CAUSE, Tag.TAG_COMPOUND)
                ? DowningCauseSnapshot.load(player, saved.getCompound(PERSISTED_CAUSE))
                : null;
            if (cause == null) {
                abandonUnverifiableDownedState(player, "persisted 1.0 cause is missing or invalid");
                return;
            }
            state = new DownedState(cause);
            remainingAbsorption = saved.contains(PERSISTED_ABSORPTION, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0.0F, saved.getFloat(PERSISTED_ABSORPTION))
                : 0.0F;
        } else if (persistent.getBoolean(LEGACY_DOWNED)) {
            abandonUnverifiableDownedState(player, "legacy pre-1.0 state has no original cause snapshot");
            return;
        } else {
            syncState(player, null, true);
            return;
        }

        DOWNED_PLAYERS.put(player.getUUID(), state);
        player.setHealth(Math.max(DOWNED_HEALTH, player.getHealth()));
        ensureDownedAbsorptionCapacity(player);
        player.setAbsorptionAmount(remainingAbsorption);
        beginCrawling(player);
        persistDownedState(player, state);
        syncState(player, state, true);
        if (remainingAbsorption <= ABSORPTION_TOLERANCE) {
            state.finalDeath.requestFinalDeath();
        }
    }

    private static void abandonUnverifiableDownedState(ServerPlayer player, String reason) {
        LOGGER.error("Clearing unverifiable downed state for {}: {}", player.getGameProfile().getName(), reason);
        DOWNED_PLAYERS.remove(player.getUUID());
        CompoundTag persistent = player.getPersistentData();
        persistent.remove(PERSISTED_STATE);
        persistent.remove(LEGACY_DOWNED);
        persistent.remove(LEGACY_ABSORPTION);
        stopCrawling(player);
        removeDownedAbsorptionCapacity(player);
        player.setHealth(Math.max(DOWNED_HEALTH, player.getHealth()));
        player.setAbsorptionAmount(0.0F);
        syncState(player, null, true);
    }

    private static void dispatchFinalDeath(ServerPlayer player, DownedState state) {
        if (!state.finalDeath.beginDispatch()) {
            return;
        }
        DamageSource downingSource = state.cause.reconstruct(player);
        try {
            // The accepted finishing hit just refreshed hurt invulnerability; this dispatch is its death phase,
            // not a second hit that should be discarded by that cooldown.
            player.invulnerableTime = 0;
            player.hurt(downingSource, Float.MAX_VALUE);
        } catch (RuntimeException exception) {
            LOGGER.error("Original-cause final-death dispatch failed for {}", player.getGameProfile().getName(), exception);
            state.finalDeath.completeDispatch(player.getHealth() > 0.0F);
            if (player.getHealth() <= 0.0F) {
                player.setHealth(DOWNED_HEALTH);
            }
            persistDownedState(player, state);
            throw exception;
        }

        FinalDeathStateMachine.Outcome outcome = state.finalDeath.completeDispatch(player.getHealth() > 0.0F);
        if (outcome == FinalDeathStateMachine.Outcome.FINAL_DEATH) {
            clearDownedState(player, false);
        } else if (outcome == FinalDeathStateMachine.Outcome.TOTEM_TRIGGERED) {
            clearDownedState(player, true);
        } else {
            if (player.getHealth() <= 0.0F) {
                player.setHealth(DOWNED_HEALTH);
            }
            beginCrawling(player);
            persistDownedState(player, state);
            syncState(player, state, false);
        }
    }

    private static void clearDownedState(ServerPlayer player, boolean preserveAbsorption) {
        DOWNED_PLAYERS.remove(player.getUUID());
        player.getPersistentData().remove(PERSISTED_STATE);
        player.getPersistentData().remove(LEGACY_DOWNED);
        player.getPersistentData().remove(LEGACY_ABSORPTION);
        stopCrawling(player);
        removeDownedAbsorptionCapacity(player);
        if (!preserveAbsorption) {
            player.setAbsorptionAmount(0.0F);
        }
        syncState(player, null, true);
    }

    private static void persistDownedState(ServerPlayer player, DownedState state) {
        CompoundTag saved = new CompoundTag();
        saved.putInt("schema", 1);
        saved.putFloat(PERSISTED_ABSORPTION, Math.max(0.0F, player.getAbsorptionAmount()));
        saved.put(PERSISTED_CAUSE, state.cause.save());
        CompoundTag persistent = player.getPersistentData();
        persistent.put(PERSISTED_STATE, saved);
        persistent.remove(LEGACY_DOWNED);
        persistent.remove(LEGACY_ABSORPTION);
    }

    private static void syncState(ServerPlayer player, DownedState state, boolean force) {
        float threshold = state == null ? 0.0F : DownedRecoveryPolicy.threshold(player.getMaxHealth());
        if (!force && Float.compare(state.lastSyncedThreshold, threshold) == 0) {
            return;
        }
        if (state != null) {
            state.lastSyncedThreshold = threshold;
        }
        try {
            PacketDistributor.sendToPlayer(
                player,
                new RaiseResurrectionAscendStatePacket(state != null, threshold)
            );
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to synchronize downed state to {}", player.getGameProfile().getName(), exception);
            throw exception;
        }
    }

    private static void ensureDownedAbsorptionCapacity(ServerPlayer player) {
        AttributeInstance capacity = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (capacity == null || capacity.hasModifier(DOWNED_ABSORPTION_CAPACITY)) {
            return;
        }
        capacity.addTransientModifier(new AttributeModifier(
            DOWNED_ABSORPTION_CAPACITY,
            DownedAbsorptionPolicy.generatedAbsorption(player.getMaxHealth()),
            AttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static void removeDownedAbsorptionCapacity(ServerPlayer player) {
        AttributeInstance capacity = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (capacity != null) {
            capacity.removeModifier(DOWNED_ABSORPTION_CAPACITY);
        }
    }

    private static void beginCrawling(ServerPlayer player) {
        player.setForcedPose(Pose.SWIMMING);
        player.setPose(Pose.SWIMMING);
        player.setSprinting(false);
    }

    private static void stopCrawling(ServerPlayer player) {
        if (player.getForcedPose() == Pose.SWIMMING) {
            player.setForcedPose(null);
        }
    }

    private static final class DownedState {
        private final DowningCauseSnapshot cause;
        private final FinalDeathStateMachine finalDeath = new FinalDeathStateMachine();
        private boolean boundaryDamagePending;
        private float lastSyncedThreshold = Float.NaN;

        private DownedState(DowningCauseSnapshot cause) {
            this.cause = cause;
        }
    }
}
