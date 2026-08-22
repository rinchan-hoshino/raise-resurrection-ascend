package dev.rinchan.raiseresurrectionascend.mixin;

import dev.rinchan.raiseresurrectionascend.FriendFeedingPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = "th.in.tamkungz.letyourfriendeating.logic.PlayerFeeder", remap = false)
public abstract class PlayerFeederMixin {
    @ModifyConstant(method = "validateItemAndPlayer", constant = @Constant(intValue = 20), remap = false)
    private static int raiseResurrectionAscend$allowFullHungerFeeding(int originalUpperBound) {
        return FriendFeedingPolicy.exclusiveFoodLevelUpperBound();
    }
}
