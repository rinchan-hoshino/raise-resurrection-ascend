package dev.rinchan.raiseresurrectionascend;

import dev.rinchan.raiseresurrectionascend.mixin.LivingEntityTotemInvoker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Delegates teammate rescue to Minecraft's native totem activation implementation. */
final class TotemProtection {
    private TotemProtection() {
    }

    static boolean tryApplyFromFeeder(ServerPlayer feeder, ServerPlayer recipient, InteractionHand feederHand) {
        ItemStack held = feeder.getItemInHand(feederHand);
        if (!held.is(Items.TOTEM_OF_UNDYING)) {
            return false;
        }

        ItemStack recipientMainHand = recipient.getMainHandItem();
        recipient.setItemInHand(InteractionHand.MAIN_HAND, held.copyWithCount(1));
        boolean triggered;
        try {
            // A deliberate teammate rescue is not a repeat of the original damage. Use an eligible
            // neutral source while delegating every effect, hook, statistic, event and animation to vanilla.
            triggered = ((LivingEntityTotemInvoker) recipient)
                .raiseResurrectionAscend$invokeTotemDeathProtection(recipient.damageSources().generic());
        } finally {
            recipient.setItemInHand(InteractionHand.MAIN_HAND, recipientMainHand);
        }
        if (!triggered) {
            return false;
        }
        if (!feeder.getAbilities().instabuild) {
            held.shrink(1);
        }
        return true;
    }
}
