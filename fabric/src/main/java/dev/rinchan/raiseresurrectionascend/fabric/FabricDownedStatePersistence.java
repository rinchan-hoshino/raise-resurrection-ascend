package dev.rinchan.raiseresurrectionascend.fabric;

import dev.rinchan.raiseresurrectionascend.DownedStatePersistence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

final class FabricDownedStatePersistence implements DownedStatePersistence {
    @Override
    public LoadedDownedState load(ServerPlayer player) {
        FabricPlayerDownedState holder = (FabricPlayerDownedState) player;
        CompoundTag state = holder.raiseResurrectionAscend$getStoredState();
        return new LoadedDownedState(
            holder.raiseResurrectionAscend$hasStoredState(),
            state == null ? null : state.copy(),
            false
        );
    }

    @Override
    public void save(ServerPlayer player, CompoundTag state) {
        ((FabricPlayerDownedState) player).raiseResurrectionAscend$setStoredState(state.copy());
    }

    @Override
    public void clear(ServerPlayer player) {
        ((FabricPlayerDownedState) player).raiseResurrectionAscend$clearStoredState();
    }
}
