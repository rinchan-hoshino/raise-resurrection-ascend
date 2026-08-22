package dev.rinchan.raiseresurrectionascend;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import th.in.tamkungz.letyourfriendeating.data.FeedStatsSavedData;
import th.in.tamkungz.letyourfriendeating.manager.CooldownManager;

public final class FriendDrinkFeeder {
    private FriendDrinkFeeder() {
    }

    public static InteractionResult tryFeedDrink(Player feeder, Player recipient, InteractionHand hand) {
        ItemStack held = feeder.getItemInHand(hand);
        boolean handledByOriginalMod = held.is(Items.MILK_BUCKET);
        if (!FriendDrinkPolicy.shouldHandle(held.getUseAnimation() == UseAnim.DRINK, handledByOriginalMod)) {
            return null;
        }
        if (feeder == recipient
            || !feeder.isAlive()
            || !recipient.isAlive()
            || feeder.isSpectator()
            || !(feeder.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }
        if (!CooldownManager.canFeed(feeder.getUUID())) {
            return InteractionResult.FAIL;
        }

        ItemStack offered = held.copyWithCount(1);
        ItemStack original = offered.copy();
        boolean recipientHadInfiniteMaterials = recipient.getAbilities().instabuild;
        ItemStack remainder;
        recipient.getAbilities().instabuild = false;
        try {
            remainder = offered.finishUsingItem(level, recipient);
        } finally {
            recipient.getAbilities().instabuild = recipientHadInfiniteMaterials;
        }

        CooldownManager.recordFeed(feeder.getUUID());
        recordStats(level, feeder, recipient);
        feeder.swing(hand, true);
        recipient.playNotifySound(held.getDrinkingSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(
            ParticleTypes.HAPPY_VILLAGER,
            recipient.getX(), recipient.getY() + 1.0D, recipient.getZ(),
            5, 0.5D, 0.5D, 0.5D, 0.0D
        );
        transferRemainder(feeder, hand, held, original, remainder);
        return InteractionResult.SUCCESS;
    }

    private static void recordStats(ServerLevel level, Player feeder, Player recipient) {
        FeedStatsSavedData stats = FeedStatsSavedData.get(level);
        stats.incrementFedCount(feeder.getUUID());
        stats.incrementEatenCount(recipient.getUUID());
    }

    private static void transferRemainder(
        Player feeder,
        InteractionHand hand,
        ItemStack held,
        ItemStack original,
        ItemStack remainder
    ) {
        if (FriendDrinkPolicy.preservesHeldItem(feeder.isCreative(), ItemStack.matches(original, remainder))) {
            return;
        }
        held.shrink(1);
        if (remainder.isEmpty()) {
            return;
        }
        if (held.isEmpty()) {
            feeder.setItemInHand(hand, remainder);
        } else if (!feeder.getInventory().add(remainder)) {
            feeder.drop(remainder, false);
        }
    }
}
