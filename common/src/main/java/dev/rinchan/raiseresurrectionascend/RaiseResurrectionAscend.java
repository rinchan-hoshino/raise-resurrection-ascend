package dev.rinchan.raiseresurrectionascend;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import org.slf4j.Logger;

public final class RaiseResurrectionAscend {
    public static final String MOD_ID = "raise_resurrection_ascend";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PERSISTED_CAUSE = "cause";
    private static final String PERSISTED_ABSORPTION = "remaining_absorption";
    private static final float DOWNED_HEALTH = 1.0F;
    private static final float ABSORPTION_TOLERANCE = 0.001F;
    private static final int DOWNED_INVISIBILITY_TICKS = 20 * 3;
    private static final int GIVE_UP_HOLD_TICKS = 40;
    private static final Identifier DOWNED_ABSORPTION_CAPACITY = Identifier.fromNamespaceAndPath(
        MOD_ID,
        "downed_absorption_capacity"
    );
    private static final Map<UUID, DownedState> DOWNED_PLAYERS = new HashMap<>();
    private static final Map<UUID, GiveUpHoldState> GIVE_UP_STATES = new HashMap<>();
    private static final Map<UUID, Integer> SYNTHETIC_TOTEM_RESCUES = new HashMap<>();
    private static DownedStatePersistence persistence;
    private static DownedStateSynchronizer synchronizer;

    private RaiseResurrectionAscend() {
    }

    public static void initialize(DownedStatePersistence statePersistence, DownedStateSynchronizer stateSynchronizer) {
        if (persistence != null || synchronizer != null) {
            throw new IllegalStateException("RRA platform services were initialized twice");
        }
        persistence = statePersistence;
        synchronizer = stateSynchronizer;
    }

    public static boolean isDowned(ServerPlayer player) {
        return DOWNED_PLAYERS.containsKey(player.getUUID());
    }

    public static boolean isDispatchingFinalDeath(ServerPlayer player) {
        DownedState state = DOWNED_PLAYERS.get(player.getUUID());
        return state != null && state.finalDeath.phase() == FinalDeathStateMachine.Phase.DISPATCHING;
    }

    /** True only for initial protection, original-cause dispatch, or a bounded teammate rescue. */
    public static boolean permitsNativeTotemCheck(ServerPlayer player) {
        return !isDowned(player)
            || isDispatchingFinalDeath(player)
            || SYNTHETIC_TOTEM_RESCUES.getOrDefault(player.getUUID(), 0) > 0;
    }

    public static void setGiveUpPressed(ServerPlayer player, boolean pressed) {
        UUID playerId = player.getUUID();
        if (!isDowned(player)) {
            GIVE_UP_STATES.remove(playerId);
            return;
        }
        GIVE_UP_STATES.computeIfAbsent(playerId, ignored -> new GiveUpHoldState(GIVE_UP_HOLD_TICKS))
            .setPressed(pressed);
    }

    static boolean withSyntheticTotemRescue(ServerPlayer player, BooleanSupplier invocation) {
        UUID playerId = player.getUUID();
        SYNTHETIC_TOTEM_RESCUES.merge(playerId, 1, Integer::sum);
        try {
            return invocation.getAsBoolean();
        } finally {
            int remaining = SYNTHETIC_TOTEM_RESCUES.getOrDefault(playerId, 1) - 1;
            if (remaining <= 0) {
                SYNTHETIC_TOTEM_RESCUES.remove(playerId);
            } else {
                SYNTHETIC_TOTEM_RESCUES.put(playerId, remaining);
            }
        }
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
        GIVE_UP_STATES.remove(player.getUUID());
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
            GiveUpHoldState giveUp = GIVE_UP_STATES.get(player.getUUID());
            if (giveUp != null && giveUp.tick()) {
                requestOriginalFinalDeath(player);
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
        GIVE_UP_STATES.remove(player.getUUID());
        SYNTHETIC_TOTEM_RESCUES.remove(player.getUUID());
        removeDownedAbsorptionCapacity(player);
    }

    public static void restorePlayer(ServerPlayer player) {
        requirePlatformServices();
        DOWNED_PLAYERS.remove(player.getUUID());
        GIVE_UP_STATES.remove(player.getUUID());
        SYNTHETIC_TOTEM_RESCUES.remove(player.getUUID());
        DownedStatePersistence.LoadedDownedState persisted = persistence.load(player);
        DownedState state;
        float remainingAbsorption;
        if (persisted.hasState()) {
            CompoundTag saved = persisted.state();
            DowningCauseSnapshot cause = saved == null
                ? null
                : saved.getCompound(PERSISTED_CAUSE)
                    .map(causeTag -> DowningCauseSnapshot.load(player, causeTag))
                    .orElse(null);
            if (cause == null) {
                abandonUnverifiableDownedState(player, "persisted 1.0 cause is missing or invalid");
                return;
            }
            state = new DownedState(cause);
            remainingAbsorption = Math.max(0.0F, saved.getFloatOr(PERSISTED_ABSORPTION, 0.0F));
        } else if (persisted.hasLegacyState()) {
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
        LOGGER.error("Clearing unverifiable downed state for {}: {}", player.getGameProfile().name(), reason);
        DOWNED_PLAYERS.remove(player.getUUID());
        GIVE_UP_STATES.remove(player.getUUID());
        SYNTHETIC_TOTEM_RESCUES.remove(player.getUUID());
        persistence.clear(player);
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
            LOGGER.error("Original-cause final-death dispatch failed for {}", player.getGameProfile().name(), exception);
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
        GIVE_UP_STATES.remove(player.getUUID());
        SYNTHETIC_TOTEM_RESCUES.remove(player.getUUID());
        persistence.clear(player);
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
        requirePlatformServices();
        persistence.save(player, saved);
    }

    private static void syncState(ServerPlayer player, DownedState state, boolean force) {
        float threshold = state == null ? 0.0F : DownedRecoveryPolicy.threshold(player.getMaxHealth());
        if (!force && Float.compare(state.lastSyncedThreshold, threshold) == 0) {
            return;
        }
        if (state != null) {
            state.lastSyncedThreshold = threshold;
        }
        requirePlatformServices();
        try {
            synchronizer.send(player, new RaiseResurrectionAscendStatePacket(state != null, threshold));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to synchronize downed state to {}", player.getGameProfile().name(), exception);
            throw exception;
        }
    }

    private static void requirePlatformServices() {
        if (persistence == null || synchronizer == null) {
            throw new IllegalStateException("RRA platform services are not initialized");
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
        player.setPose(Pose.SWIMMING);
        player.setSprinting(false);
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
