package dev.rinchan.raiseresurrectionascend;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Loader-owned storage for the one structured downed-state record. */
public interface DownedStatePersistence {
    String STORAGE_KEY = RaiseResurrectionAscend.MOD_ID + ":downed_state";
    String LEGACY_DOWNED_KEY = RaiseResurrectionAscend.MOD_ID + ":downed";
    String LEGACY_ABSORPTION_KEY = RaiseResurrectionAscend.MOD_ID + ":remaining_absorption";

    LoadedDownedState load(ServerPlayer player);

    void save(ServerPlayer player, CompoundTag state);

    void clear(ServerPlayer player);

    record LoadedDownedState(boolean hasState, CompoundTag state, boolean hasLegacyState) {
        public static LoadedDownedState empty() {
            return new LoadedDownedState(false, null, false);
        }
    }
}
