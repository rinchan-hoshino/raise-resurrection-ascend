package dev.rinchan.raiseresurrectionascend.mixin;

import dev.rinchan.raiseresurrectionascend.FriendDrinkFeeder;
import dev.rinchan.raiseresurrectionascend.FriendFeedingPolicy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "th.in.tamkungz.letyourfriendeating.logic.PlayerFeeder", remap = false)
public abstract class PlayerFeederMixin {
    @Inject(method = "tryFeedPlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private static void raiseResurrectionAscend$feedDrink(
        Player feeder,
        Player recipient,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> callback
    ) {
        InteractionResult result = FriendDrinkFeeder.tryFeedDrink(feeder, recipient, hand);
        if (result != null) {
            callback.setReturnValue(result);
        }
    }

    @ModifyConstant(method = "validateItemAndPlayer", constant = @Constant(intValue = 20), remap = false)
    private static int raiseResurrectionAscend$allowFullHungerFeeding(int originalUpperBound) {
        return FriendFeedingPolicy.exclusiveFoodLevelUpperBound();
    }
}
