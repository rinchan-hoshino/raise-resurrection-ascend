package dev.rinchan.raiseresurrectionascend;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.gameevent.GameEvent;

public final class RaiseResurrectionAscend {
    public static final String MOD_ID = "raise_resurrection_ascend";
    private static final float DOWNED_HEALTH = 1.0F;
    private static final Map<UUID, Integer> DOWNED_UNTIL_TICK = new ConcurrentHashMap<>();
    private static final Set<UUID> FINAL_DEATH = ConcurrentHashMap.newKeySet();

    private RaiseResurrectionAscend() {
    }

    public static boolean isDowned(ServerPlayer player) {
        return DOWNED_UNTIL_TICK.containsKey(player.getUUID());
    }

    public static void enterDowned(ServerPlayer player) {
        DOWNED_UNTIL_TICK.put(player.getUUID(), player.server.getTickCount() + RaiseResurrectionAscendConfig.downedDurationTicks.get());
        player.setHealth(DOWNED_HEALTH);
        forceCrawling(player);
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            Integer until = DOWNED_UNTIL_TICK.get(playerId);
            if (until == null) {
                continue;
            }
            if (!player.isAlive()) {
                DOWNED_UNTIL_TICK.remove(playerId);
                continue;
            }
            if (recoverIfFullyHealed(player)) {
                continue;
            }
            forceCrawling(player);
            if (now >= until) {
                dieNow(player);
            }
        }
    }

    public static boolean shouldEnterDowned(ServerPlayer player, float damage) {
        return !isFinalDeath(player) && !isDowned(player) && player.getHealth() - damage <= 0F;
    }

    public static boolean isFinalDeath(ServerPlayer player) {
        return FINAL_DEATH.contains(player.getUUID());
    }

    public static void clearFinalDeath(ServerPlayer player) {
        FINAL_DEATH.remove(player.getUUID());
    }

    public static boolean recoverIfFullyHealed(ServerPlayer player) {
        if (!isDowned(player) || !DownedRecoveryPolicy.isFullyHealed(player.getHealth(), player.getMaxHealth())) {
            return false;
        }
        DOWNED_UNTIL_TICK.remove(player.getUUID());
        player.setPose(Pose.STANDING);
        return true;
    }

    public static boolean reviveByCommand(ServerPlayer player) {
        if (!isDowned(player)) {
            return false;
        }
        player.setHealth(player.getMaxHealth());
        return recoverIfFullyHealed(player);
    }

    public static boolean tryFeedRecoveryPotion(ServerPlayer feeder, ServerPlayer target, InteractionHand hand) {
        ItemStack potionStack = feeder.getItemInHand(hand);
        PotionContents contents = potionStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        boolean hasRecoveryEffect = false;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().is(MobEffects.HEAL) || effect.getEffect().is(MobEffects.REGENERATION)) {
                hasRecoveryEffect = true;
                break;
            }
        }

        PotionFeedingPolicy.Result result = PotionFeedingPolicy.resolve(
            isDowned(target),
            potionStack.is(Items.POTION),
            hasRecoveryEffect,
            potionStack.getCount(),
            feeder.hasInfiniteMaterials()
        );
        if (!result.accepted()) {
            return false;
        }

        contents.forEachEffect(effect -> {
            if (effect.getEffect().value().isInstantenous()) {
                effect.getEffect().value().applyInstantenousEffect(feeder, feeder, target, effect.getAmplifier(), 1.0);
            } else {
                target.addEffect(effect);
            }
        });

        if (!feeder.hasInfiniteMaterials()) {
            potionStack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (potionStack.isEmpty()) {
                feeder.setItemInHand(hand, bottle);
            } else if (!feeder.getInventory().add(bottle)) {
                feeder.drop(bottle, false);
            }
        }

        feeder.awardStat(Stats.ITEM_USED.get(Items.POTION));
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5F, 1.0F);
        target.gameEvent(GameEvent.DRINK);
        recoverIfFullyHealed(target);
        return true;
    }

    public static void giveUp(ServerPlayer player) {
        if (isDowned(player)) {
            dieNow(player);
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        DOWNED_UNTIL_TICK.remove(player.getUUID());
        FINAL_DEATH.remove(player.getUUID());
    }

    private static void dieNow(ServerPlayer player) {
        DOWNED_UNTIL_TICK.remove(player.getUUID());
        FINAL_DEATH.add(player.getUUID());
        player.setHealth(DOWNED_HEALTH);
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        FINAL_DEATH.remove(player.getUUID());
    }

    private static void forceCrawling(ServerPlayer player) {
        player.setPose(Pose.SWIMMING);
        player.setSprinting(false);
    }
}
