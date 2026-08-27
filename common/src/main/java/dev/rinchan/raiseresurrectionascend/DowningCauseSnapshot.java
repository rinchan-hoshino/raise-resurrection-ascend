package dev.rinchan.raiseresurrectionascend;

import com.mojang.logging.LogUtils;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/** Immutable persisted identity of the damage source that originally downed a player. */
final class DowningCauseSnapshot {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DAMAGE_TYPE = "damage_type";
    private static final String DIRECT_ENTITY = "direct_entity";
    private static final String CAUSING_ENTITY = "causing_entity";
    private static final String SOURCE_POSITION = "source_position";
    private static final String DEATH_MESSAGE = "death_message";

    private final String damageTypeId;
    @Nullable
    private final EntityReference directEntity;
    @Nullable
    private final EntityReference causingEntity;
    @Nullable
    private final Vec3 sourcePosition;
    private final String deathMessageJson;

    private DowningCauseSnapshot(
            String damageTypeId,
            @Nullable EntityReference directEntity,
            @Nullable EntityReference causingEntity,
            @Nullable Vec3 sourcePosition,
            String deathMessageJson) {
        this.damageTypeId = damageTypeId;
        this.directEntity = directEntity;
        this.causingEntity = causingEntity;
        this.sourcePosition = sourcePosition;
        this.deathMessageJson = deathMessageJson;
    }

    @Nullable
    static DowningCauseSnapshot capture(ServerPlayer player, DamageSource source) {
        ResourceLocation typeId = source.typeHolder().unwrapKey().map(ResourceKey::location).orElse(null);
        if (typeId == null) {
            LOGGER.error("Cannot down {} because DamageSource {} has no registered damage type key", player.getGameProfile().getName(), source);
            return null;
        }
        Component deathMessage = source.getLocalizedDeathMessage(player);
        return new DowningCauseSnapshot(
            typeId.toString(),
            EntityReference.capture(source.getDirectEntity()),
            EntityReference.capture(source.getEntity()),
            source.sourcePositionRaw(),
            Component.Serializer.toJson(deathMessage, player.registryAccess())
        );
    }

    @Nullable
    static DowningCauseSnapshot load(ServerPlayer player, CompoundTag tag) {
        if (!tag.contains(DAMAGE_TYPE, Tag.TAG_STRING) || !tag.contains(DEATH_MESSAGE, Tag.TAG_STRING)) {
            LOGGER.error("Cannot restore downed state for {} because its persisted cause lacks a damage type or death message", player.getGameProfile().getName());
            return null;
        }
        String damageTypeId = tag.getString(DAMAGE_TYPE);
        ResourceLocation typeLocation = ResourceLocation.tryParse(damageTypeId);
        Registry<DamageType> damageTypes = player.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        if (typeLocation == null || damageTypes.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, typeLocation)).isEmpty()) {
            LOGGER.error("Cannot restore downed state for {} because damage type '{}' is unavailable", player.getGameProfile().getName(), damageTypeId);
            return null;
        }
        EntityReference direct = tag.contains(DIRECT_ENTITY, Tag.TAG_COMPOUND)
            ? EntityReference.load(tag.getCompound(DIRECT_ENTITY), DIRECT_ENTITY)
            : null;
        EntityReference causing = tag.contains(CAUSING_ENTITY, Tag.TAG_COMPOUND)
            ? EntityReference.load(tag.getCompound(CAUSING_ENTITY), CAUSING_ENTITY)
            : null;
        if ((tag.contains(DIRECT_ENTITY, Tag.TAG_COMPOUND) && direct == null)
                || (tag.contains(CAUSING_ENTITY, Tag.TAG_COMPOUND) && causing == null)) {
            LOGGER.error("Cannot restore downed state for {} because an entity reference in its cause is malformed", player.getGameProfile().getName());
            return null;
        }
        Vec3 position = null;
        if (tag.contains(SOURCE_POSITION, Tag.TAG_COMPOUND)) {
            CompoundTag positionTag = tag.getCompound(SOURCE_POSITION);
            if (!positionTag.contains("x", Tag.TAG_ANY_NUMERIC)
                    || !positionTag.contains("y", Tag.TAG_ANY_NUMERIC)
                    || !positionTag.contains("z", Tag.TAG_ANY_NUMERIC)) {
                LOGGER.error("Cannot restore downed state for {} because its source position is malformed", player.getGameProfile().getName());
                return null;
            }
            position = new Vec3(positionTag.getDouble("x"), positionTag.getDouble("y"), positionTag.getDouble("z"));
        }
        String deathMessageJson = tag.getString(DEATH_MESSAGE);
        try {
            if (Component.Serializer.fromJson(deathMessageJson, player.registryAccess()) == null) {
                LOGGER.error("Cannot restore downed state for {} because its death message is empty", player.getGameProfile().getName());
                return null;
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Cannot restore downed state for {} because its death message is malformed", player.getGameProfile().getName(), exception);
            return null;
        }
        return new DowningCauseSnapshot(damageTypeId, direct, causing, position, deathMessageJson);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(DAMAGE_TYPE, damageTypeId);
        if (directEntity != null) {
            tag.put(DIRECT_ENTITY, directEntity.save());
        }
        if (causingEntity != null) {
            tag.put(CAUSING_ENTITY, causingEntity.save());
        }
        if (sourcePosition != null) {
            CompoundTag positionTag = new CompoundTag();
            positionTag.putDouble("x", sourcePosition.x());
            positionTag.putDouble("y", sourcePosition.y());
            positionTag.putDouble("z", sourcePosition.z());
            tag.put(SOURCE_POSITION, positionTag);
        }
        tag.putString(DEATH_MESSAGE, deathMessageJson);
        return tag;
    }

    DamageSource reconstruct(ServerPlayer player) {
        Registry<DamageType> damageTypes = player.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        ResourceLocation location = ResourceLocation.tryParse(damageTypeId);
        if (location == null) {
            throw new IllegalStateException("Validated downing damage type became malformed: " + damageTypeId);
        }
        Holder<DamageType> type = damageTypes.getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, location));
        Entity direct = resolve(player, directEntity);
        Entity causing = resolve(player, causingEntity);
        Component recordedMessage = Component.Serializer.fromJson(deathMessageJson, player.registryAccess());
        if (recordedMessage == null) {
            throw new IllegalStateException("Validated downing death message became empty");
        }
        return RecordedMessageDamageSource.create(type, direct, causing, sourcePosition, recordedMessage);
    }

    @Nullable
    private static Entity resolve(ServerPlayer player, @Nullable EntityReference reference) {
        if (reference == null) {
            return null;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(reference.dimension());
        if (dimensionId != null) {
            ServerLevel originalLevel = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
            if (originalLevel != null) {
                Entity entity = originalLevel.getEntity(reference.uuid());
                if (entity != null) {
                    return entity;
                }
            }
        }
        for (ServerLevel level : player.server.getAllLevels()) {
            Entity entity = level.getEntity(reference.uuid());
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private record EntityReference(UUID uuid, String dimension) {
        @Nullable
        static EntityReference capture(@Nullable Entity entity) {
            return entity == null ? null : new EntityReference(entity.getUUID(), entity.level().dimension().location().toString());
        }

        @Nullable
        static EntityReference load(CompoundTag tag, String field) {
            if (!tag.hasUUID("uuid") || !tag.contains("dimension", Tag.TAG_STRING)) {
                LOGGER.error("Persisted {} reference is malformed", field);
                return null;
            }
            return new EntityReference(tag.getUUID("uuid"), tag.getString("dimension"));
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", uuid);
            tag.putString("dimension", dimension);
            return tag;
        }
    }

    private static final class RecordedMessageDamageSource extends DamageSource {
        private final Component recordedMessage;

        private RecordedMessageDamageSource(
                Holder<DamageType> type,
                @Nullable Entity direct,
                @Nullable Entity causing,
                Component recordedMessage) {
            super(type, direct, causing);
            this.recordedMessage = recordedMessage.copy();
        }

        private RecordedMessageDamageSource(
                Holder<DamageType> type,
                Vec3 position,
                Component recordedMessage) {
            super(type, position);
            this.recordedMessage = recordedMessage.copy();
        }

        private RecordedMessageDamageSource(Holder<DamageType> type, Component recordedMessage) {
            super(type);
            this.recordedMessage = recordedMessage.copy();
        }

        private static DamageSource create(
                Holder<DamageType> type,
                @Nullable Entity direct,
                @Nullable Entity causing,
                @Nullable Vec3 position,
                Component recordedMessage) {
            if (direct != null || causing != null) {
                return new RecordedMessageDamageSource(type, direct, causing, recordedMessage);
            }
            if (position != null) {
                return new RecordedMessageDamageSource(type, position, recordedMessage);
            }
            return new RecordedMessageDamageSource(type, recordedMessage);
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity subject) {
            return recordedMessage.copy();
        }
    }
}
