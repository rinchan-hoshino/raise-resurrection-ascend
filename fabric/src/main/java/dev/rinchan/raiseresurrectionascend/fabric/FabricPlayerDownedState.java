package dev.rinchan.raiseresurrectionascend.fabric;

import net.minecraft.nbt.CompoundTag;

public interface FabricPlayerDownedState {
    boolean raiseResurrectionAscend$hasStoredState();

    CompoundTag raiseResurrectionAscend$getStoredState();

    void raiseResurrectionAscend$setStoredState(CompoundTag state);

    void raiseResurrectionAscend$clearStoredState();
}
