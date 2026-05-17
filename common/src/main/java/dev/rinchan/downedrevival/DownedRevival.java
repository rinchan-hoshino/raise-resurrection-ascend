package dev.rinchan.downedrevival;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DownedRevival {
    public static final String MOD_ID = "downed_revival";
    private static final float DOWNED_HEALTH = 1.0F;
    private static final Map<UUID, Integer> DOWNED_UNTIL_TICK = new ConcurrentHashMap<>();
    private static final Set<UUID> FINAL_DEATH = ConcurrentHashMap.newKeySet();

    private DownedRevival() {
    }

    public static boolean isDowned(ServerPlayer player) {
        return DOWNED_UNTIL_TICK.containsKey(player.getUUID());
    }

    public static void enterDowned(ServerPlayer player) {
        DOWNED_UNTIL_TICK.put(player.getUUID(), player.server.getTickCount() + DownedRevivalConfig.downedDurationTicks.get());
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
            player.setHealth(Math.max(DOWNED_HEALTH, player.getHealth()));
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

    public static boolean tryReviveWithItem(ServerPlayer rescuer, ServerPlayer downed, InteractionHand hand) {
        if (!isDowned(downed)) {
            return false;
        }
        ItemStack stack = rescuer.getItemInHand(hand);
        if (!isReviveItem(stack)) {
            return false;
        }
        if (DownedRevivalConfig.consumeReviveItem.get() && !rescuer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        reviveWithTotemEffects(downed);
        return true;
    }

    public static boolean tryKillRevive(ServerPlayer killer) {
        if (!DownedRevivalConfig.enableKillRevive.get() || !isDowned(killer)) {
            return false;
        }
        reviveWithTotemEffects(killer);
        return true;
    }

    public static boolean reviveWithTotemEffects(ServerPlayer player) {
        if (!isDowned(player)) {
            return false;
        }
        DOWNED_UNTIL_TICK.remove(player.getUUID());
        player.setPose(Pose.STANDING);
        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.level().broadcastEntityEvent(player, (byte) 35);
        return true;
    }

    public static boolean reviveByCommand(ServerPlayer player) {
        return reviveWithTotemEffects(player);
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

    private static boolean isReviveItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (String configuredId : DownedRevivalConfig.reviveItems.get()) {
            if (configuredId.trim().equals(itemId)) {
                return true;
            }
        }
        return false;
    }
}
