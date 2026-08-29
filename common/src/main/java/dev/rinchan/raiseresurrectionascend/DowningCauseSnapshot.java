package dev.rinchan.raiseresurrectionascend;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
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
        Identifier typeId = source.typeHolder().unwrapKey().map(ResourceKey::identifier).orElse(null);
        if (typeId == null) {
            LOGGER.error(
                "Cannot down {} because DamageSource {} has no registered damage type key",
                player.getGameProfile().name(),
                source
            );
            return null;
        }
        Component deathMessage = source.getLocalizedDeathMessage(player);
        return new DowningCauseSnapshot(
            typeId.toString(),
            EntityReference.capture(source.getDirectEntity()),
            EntityReference.capture(source.getEntity()),
            source.sourcePositionRaw(),
            serializeComponent(player, deathMessage)
        );
    }

    @Nullable
    static DowningCauseSnapshot load(ServerPlayer player, CompoundTag tag) {
        Optional<String> storedDamageType = tag.getString(DAMAGE_TYPE);
        Optional<String> storedDeathMessage = tag.getString(DEATH_MESSAGE);
        if (storedDamageType.isEmpty() || storedDeathMessage.isEmpty()) {
            LOGGER.error(
                "Cannot restore downed state for {} because its persisted cause lacks a damage type or death message",
                player.getGameProfile().name()
            );
            return null;
        }

        String damageTypeId = storedDamageType.get();
        Identifier typeIdentifier = Identifier.tryParse(damageTypeId);
        Registry<DamageType> damageTypes = player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        if (typeIdentifier == null
                || damageTypes.get(ResourceKey.create(Registries.DAMAGE_TYPE, typeIdentifier)).isEmpty()) {
            LOGGER.error(
                "Cannot restore downed state for {} because damage type '{}' is unavailable",
                player.getGameProfile().name(),
                damageTypeId
            );
            return null;
        }

        Optional<CompoundTag> directTag = tag.getCompound(DIRECT_ENTITY);
        Optional<CompoundTag> causingTag = tag.getCompound(CAUSING_ENTITY);
        EntityReference direct = directTag.map(value -> EntityReference.load(value, DIRECT_ENTITY)).orElse(null);
        EntityReference causing = causingTag.map(value -> EntityReference.load(value, CAUSING_ENTITY)).orElse(null);
        if ((tag.contains(DIRECT_ENTITY) && direct == null) || (tag.contains(CAUSING_ENTITY) && causing == null)) {
            LOGGER.error(
                "Cannot restore downed state for {} because an entity reference in its cause is malformed",
                player.getGameProfile().name()
            );
            return null;
        }

        Vec3 position = null;
        if (tag.contains(SOURCE_POSITION)) {
            Optional<CompoundTag> positionTag = tag.getCompound(SOURCE_POSITION);
            if (positionTag.isEmpty()) {
                LOGGER.error(
                    "Cannot restore downed state for {} because its source position is malformed",
                    player.getGameProfile().name()
                );
                return null;
            }
            Optional<Double> x = positionTag.get().getDouble("x");
            Optional<Double> y = positionTag.get().getDouble("y");
            Optional<Double> z = positionTag.get().getDouble("z");
            if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
                LOGGER.error(
                    "Cannot restore downed state for {} because its source position is malformed",
                    player.getGameProfile().name()
                );
                return null;
            }
            position = new Vec3(x.get(), y.get(), z.get());
        }

        String deathMessageJson = storedDeathMessage.get();
        try {
            deserializeComponent(player, deathMessageJson);
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Cannot restore downed state for {} because its death message is malformed",
                player.getGameProfile().name(),
                exception
            );
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

    Component recordedDeathMessage(ServerPlayer player) {
        return deserializeComponent(player, deathMessageJson).copy();
    }

    DamageSource reconstruct(ServerPlayer player) {
        Registry<DamageType> damageTypes = player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        Identifier identifier = Identifier.tryParse(damageTypeId);
        if (identifier == null) {
            throw new IllegalStateException("Validated downing damage type became malformed: " + damageTypeId);
        }
        Holder<DamageType> type = damageTypes.getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, identifier));
        Entity direct = resolve(player, directEntity);
        Entity causing = resolve(player, causingEntity);
        return RecordedMessageDamageSource.create(
            type,
            direct,
            causing,
            sourcePosition,
            recordedDeathMessage(player)
        );
    }

    private static String serializeComponent(ServerPlayer player, Component component) {
        return ComponentSerialization.CODEC.encodeStart(
            player.registryAccess().createSerializationContext(JsonOps.INSTANCE),
            component
        ).getOrThrow().toString();
    }

    private static Component deserializeComponent(ServerPlayer player, String json) {
        return ComponentSerialization.CODEC.parse(
            player.registryAccess().createSerializationContext(JsonOps.INSTANCE),
            JsonParser.parseString(json)
        ).getOrThrow();
    }

    @Nullable
    private static Entity resolve(ServerPlayer player, @Nullable EntityReference reference) {
        if (reference == null) {
            return null;
        }
        MinecraftServer server = player.level().getServer();
        Identifier dimensionId = Identifier.tryParse(reference.dimension());
        if (dimensionId != null) {
            ServerLevel originalLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
            if (originalLevel != null) {
                Entity entity = originalLevel.getEntity(reference.uuid());
                if (entity != null) {
                    return entity;
                }
            }
        }
        for (ServerLevel level : server.getAllLevels()) {
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
            return entity == null
                ? null
                : new EntityReference(entity.getUUID(), entity.level().dimension().identifier().toString());
        }

        @Nullable
        static EntityReference load(CompoundTag tag, String field) {
            Optional<UUID> uuid = tag.read("uuid", UUIDUtil.CODEC);
            Optional<String> dimension = tag.getString("dimension");
            if (uuid.isEmpty() || dimension.isEmpty()) {
                LOGGER.error("Persisted {} reference is malformed", field);
                return null;
            }
            return new EntityReference(uuid.get(), dimension.get());
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.store("uuid", UUIDUtil.CODEC, uuid);
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
