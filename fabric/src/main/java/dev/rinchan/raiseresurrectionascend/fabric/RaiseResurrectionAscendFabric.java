package dev.rinchan.raiseresurrectionascend.fabric;

import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscend;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendGiveUpInputPacket;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

public final class RaiseResurrectionAscendFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(
            RaiseResurrectionAscendStatePacket.TYPE,
            RaiseResurrectionAscendStatePacket.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
            RaiseResurrectionAscendGiveUpInputPacket.TYPE,
            RaiseResurrectionAscendGiveUpInputPacket.CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
            RaiseResurrectionAscendGiveUpInputPacket.TYPE,
            (packet, context) -> context.server().execute(() ->
                RaiseResurrectionAscend.handleGiveUpInput(context.player(), packet.pressed())
            )
        );
        RaiseResurrectionAscend.initialize(
            new FabricDownedStatePersistence(),
            ServerPlayNetworking::send
        );

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damageAmount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true;
            }
            if (!RaiseResurrectionAscend.isDowned(player)) {
                return !RaiseResurrectionAscend.enterDowned(player, source);
            }
            if (RaiseResurrectionAscend.isDispatchingFinalDeath(player)) {
                return true;
            }
            RaiseResurrectionAscend.requestOriginalFinalDeath(player);
            return false;
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer feeder)
                    || !(entity instanceof ServerPlayer recipient)
                    || !feeder.getItemInHand(hand).is(Items.TOTEM_OF_UNDYING)) {
                return InteractionResult.PASS;
            }
            return RaiseResurrectionAscend.tryFeedTotem(feeder, recipient, hand)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(RaiseResurrectionAscend::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            RaiseResurrectionAscend.restorePlayer(handler.player)
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            RaiseResurrectionAscend.suspendPlayer(handler.player)
        );
    }
}
