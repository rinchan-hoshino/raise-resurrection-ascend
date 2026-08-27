package dev.rinchan.raiseresurrectionascend.neoforge;

import dev.rinchan.raiseresurrectionascend.DownedStatePersistence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

final class NeoForgeDownedStatePersistence implements DownedStatePersistence {
    @Override
    public LoadedDownedState load(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        boolean hasState = persistent.contains(STORAGE_KEY);
        CompoundTag state = persistent.contains(STORAGE_KEY, Tag.TAG_COMPOUND)
            ? persistent.getCompound(STORAGE_KEY).copy()
            : null;
        return new LoadedDownedState(hasState, state, persistent.getBoolean(LEGACY_DOWNED_KEY));
    }

    @Override
    public void save(ServerPlayer player, CompoundTag state) {
        CompoundTag persistent = player.getPersistentData();
        persistent.put(STORAGE_KEY, state.copy());
        persistent.remove(LEGACY_DOWNED_KEY);
        persistent.remove(LEGACY_ABSORPTION_KEY);
    }

    @Override
    public void clear(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        persistent.remove(STORAGE_KEY);
        persistent.remove(LEGACY_DOWNED_KEY);
        persistent.remove(LEGACY_ABSORPTION_KEY);
    }
}
