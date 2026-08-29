package dev.rinchan.raiseresurrectionascend.mixin.fabric;

import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscend;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityTotemGuardMixin {
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void raiseResurrectionAscend$guardFollowUpTotem(
            DamageSource source,
            CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player
                && !RaiseResurrectionAscend.permitsNativeTotemCheck(player)) {
            callback.setReturnValue(false);
        }
    }
}
