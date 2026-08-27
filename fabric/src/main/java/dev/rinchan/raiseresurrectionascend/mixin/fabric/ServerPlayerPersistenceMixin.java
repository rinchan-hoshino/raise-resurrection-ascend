package dev.rinchan.raiseresurrectionascend.mixin.fabric;

import dev.rinchan.raiseresurrectionascend.DownedStatePersistence;
import dev.rinchan.raiseresurrectionascend.fabric.FabricPlayerDownedState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerPersistenceMixin implements FabricPlayerDownedState {
    @Unique
    private boolean raiseResurrectionAscend$hasStoredState;
    @Unique
    private CompoundTag raiseResurrectionAscend$storedState;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void raiseResurrectionAscend$writeState(ValueOutput output, CallbackInfo callback) {
        if (raiseResurrectionAscend$hasStoredState && raiseResurrectionAscend$storedState != null) {
            output.store(
                DownedStatePersistence.STORAGE_KEY,
                CompoundTag.CODEC,
                raiseResurrectionAscend$storedState.copy()
            );
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void raiseResurrectionAscend$readState(ValueInput input, CallbackInfo callback) {
        raiseResurrectionAscend$storedState = input.read(
            DownedStatePersistence.STORAGE_KEY,
            CompoundTag.CODEC
        ).map(CompoundTag::copy).orElse(null);
        raiseResurrectionAscend$hasStoredState = raiseResurrectionAscend$storedState != null;
    }

    @Override
    public boolean raiseResurrectionAscend$hasStoredState() {
        return raiseResurrectionAscend$hasStoredState;
    }

    @Override
    public CompoundTag raiseResurrectionAscend$getStoredState() {
        return raiseResurrectionAscend$storedState;
    }

    @Override
    public void raiseResurrectionAscend$setStoredState(CompoundTag state) {
        raiseResurrectionAscend$hasStoredState = true;
        raiseResurrectionAscend$storedState = state.copy();
    }

    @Override
    public void raiseResurrectionAscend$clearStoredState() {
        raiseResurrectionAscend$hasStoredState = false;
        raiseResurrectionAscend$storedState = null;
    }
}
