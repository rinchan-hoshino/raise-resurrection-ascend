package dev.rinchan.raiseresurrectionascend;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.network.PacketDistributor;

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
        int durationTicks = RaiseResurrectionAscendConfig.downedDurationTicks.get();
        DOWNED_UNTIL_TICK.put(player.getUUID(), player.server.getTickCount() + durationTicks);
        player.setHealth(DOWNED_HEALTH);
        beginCrawling(player);
        syncState(player, true, durationTicks);
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
        int now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            Integer until = DOWNED_UNTIL_TICK.get(playerId);
            if (until == null) {
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
        PotionContents potionContents = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        RecoveryFeedingPolicy.ItemKind itemKind = recoveryItemKind(itemStack, potionContents);
        RecoveryFeedingPolicy.Result result = RecoveryFeedingPolicy.resolve(
            isDowned(target),
            itemKind,
            itemStack.getCount(),
            feeder.hasInfiniteMaterials()
        );
        if (!result.accepted()) {
            return false;
        }

        if (itemKind == RecoveryFeedingPolicy.ItemKind.RECOVERY_POTION) {
            applyPotionContents(feeder, target, potionContents);
        } else {
            target.eat(target.level(), itemStack.copyWithCount(1));
        }

        if (!feeder.hasInfiniteMaterials()) {
            itemStack.shrink(1);
            if (result.returnBottle()) {
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (itemStack.isEmpty()) {
                    feeder.setItemInHand(hand, bottle);
                } else if (!feeder.getInventory().add(bottle)) {
                    feeder.drop(bottle, false);
                }
            }
        }

        feeder.awardStat(Stats.ITEM_USED.get(fedItem));
        if (itemKind == RecoveryFeedingPolicy.ItemKind.RECOVERY_POTION) {
            target.level().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.GENERIC_DRINK,
                SoundSource.PLAYERS,
                0.5F,
                1.0F
            );
            target.gameEvent(GameEvent.DRINK);
        }
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
        clearForcedCrawling(player);
    }

    private static RecoveryFeedingPolicy.ItemKind recoveryItemKind(ItemStack stack, PotionContents contents) {
        if (stack.is(Items.GOLDEN_APPLE)) {
            return RecoveryFeedingPolicy.ItemKind.GOLDEN_APPLE;
        }
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return RecoveryFeedingPolicy.ItemKind.ENCHANTED_GOLDEN_APPLE;
        }
        if (!stack.is(Items.POTION)) {
            return RecoveryFeedingPolicy.ItemKind.UNSUPPORTED;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().is(MobEffects.HEAL) || effect.getEffect().is(MobEffects.REGENERATION)) {
                return RecoveryFeedingPolicy.ItemKind.RECOVERY_POTION;
            }
        }
        return RecoveryFeedingPolicy.ItemKind.UNSUPPORTED;
    }

    private static void applyPotionContents(ServerPlayer feeder, ServerPlayer target, PotionContents contents) {
        contents.forEachEffect(effect -> {
            if (effect.getEffect().value().isInstantenous()) {
                effect.getEffect().value().applyInstantenousEffect(feeder, feeder, target, effect.getAmplifier(), 1.0);
            } else {
                target.addEffect(effect);
            }
        });
    }

    private static void dieNow(ServerPlayer player) {
        clearDownedState(player);
        FINAL_DEATH.add(player.getUUID());
        player.setHealth(DOWNED_HEALTH);
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        FINAL_DEATH.remove(player.getUUID());
    }

    private static void beginCrawling(ServerPlayer player) {
        player.setForcedPose(Pose.SWIMMING);
        player.setPose(Pose.SWIMMING);
        player.setSprinting(false);
    }

    private static void clearDownedState(ServerPlayer player) {
        DOWNED_UNTIL_TICK.remove(player.getUUID());
        clearForcedCrawling(player);
        syncState(player, false, 0);
    }

    private static void clearForcedCrawling(ServerPlayer player) {
        if (player.getForcedPose() == Pose.SWIMMING) {
            player.setForcedPose(null);
        }
    }

    private static void syncState(ServerPlayer player, boolean downed, int remainingTicks) {
        PacketDistributor.sendToPlayer(player, new RaiseResurrectionAscendStatePacket(downed, remainingTicks));
    }
}
