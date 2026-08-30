package dev.rinchan.raiseresurrectionascend.mixin;

import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscend;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathCompletionMixin {
    @Inject(method = "die", at = @At("TAIL"))
    private void raiseResurrectionAscend$observeNativeDeathCompletion(DamageSource source, CallbackInfo callback) {
        RaiseResurrectionAscend.observeNativeDeathCompletion((ServerPlayer) (Object) this);
    }
}
