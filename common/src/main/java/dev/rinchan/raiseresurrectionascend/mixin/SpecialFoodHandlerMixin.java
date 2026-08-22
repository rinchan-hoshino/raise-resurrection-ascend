package dev.rinchan.raiseresurrectionascend.mixin;

import dev.rinchan.raiseresurrectionascend.FriendFeedingPolicy;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "th.in.tamkungz.letyourfriendeating.logic.SpecialFoodHandler", remap = false)
public abstract class SpecialFoodHandlerMixin {
    @Redirect(
        method = {"handleEternalFood", "handleStew"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/food/FoodData;needsFood()Z",
            remap = false
        ),
        remap = false
    )
    private static boolean raiseResurrectionAscend$allowFullHungerSpecialFood(FoodData foodData) {
        return FriendFeedingPolicy.canReceiveFood(foodData.getFoodLevel());
    }
}
