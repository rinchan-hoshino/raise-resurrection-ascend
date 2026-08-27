package dev.rinchan.raiseresurrectionascend.neoforge;

import com.mojang.logging.LogUtils;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscend;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendGiveUpInputPacket;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import dev.rinchan.raiseresurrectionascend.client.RaiseResurrectionAscendClient;
import dev.rinchan.raiseresurrectionascend.client.neoforge.RaiseResurrectionAscendNeoForgeClient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(RaiseResurrectionAscend.MOD_ID)
public class RaiseResurrectionAscendNeoForge {
    private static final Logger LOGGER = LogUtils.getLogger();

    public RaiseResurrectionAscendNeoForge(IEventBus modBus) {
        RaiseResurrectionAscend.initialize(
            new NeoForgeDownedStatePersistence(),
            (player, packet) -> PacketDistributor.sendToPlayer(player, packet)
        );
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onLivingUseTotem);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, this::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RaiseResurrectionAscendNeoForgeClient.register(modBus);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.1");
        registrar.playToClient(
            RaiseResurrectionAscendStatePacket.TYPE,
            RaiseResurrectionAscendStatePacket.CODEC,
            (packet, context) -> context.enqueueWork(() -> RaiseResurrectionAscendClient.applyState(packet))
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("Failed to apply downed-state payload", throwable);
                    }
                })
        );
        registrar.playToServer(
            RaiseResurrectionAscendGiveUpInputPacket.TYPE,
            RaiseResurrectionAscendGiveUpInputPacket.CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    RaiseResurrectionAscend.handleGiveUpInput(player, packet.pressed());
                }
            }).whenComplete((ignored, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Failed to handle give-up input payload", throwable);
                }
            })
        );
    }

    private void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setNewDamage(RaiseResurrectionAscend.adjustDownedDamage(player, event.getNewDamage()));
        }
    }

    private void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaiseResurrectionAscend.resolveDownedDamage(player);
        }
    }

    private void onLivingUseTotem(LivingUseTotemEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) {
            RaiseResurrectionAscend.observeNativeTotemTrigger(player);
        }
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!RaiseResurrectionAscend.isDowned(player)) {
            if (RaiseResurrectionAscend.enterDowned(player, event.getSource())) {
                event.setCanceled(true);
            }
            return;
        }
        if (RaiseResurrectionAscend.isDispatchingFinalDeath(player)) {
            RaiseResurrectionAscend.observeFinalDeath(player);
            return;
        }
        event.setCanceled(true);
        RaiseResurrectionAscend.requestOriginalFinalDeath(player);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer feeder)
                || !(event.getTarget() instanceof ServerPlayer recipient)
                || !feeder.getItemInHand(event.getHand()).is(Items.TOTEM_OF_UNDYING)) {
            return;
        }
        if (RaiseResurrectionAscend.tryFeedTotem(feeder, recipient, event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        RaiseResurrectionAscend.tick(event.getServer());
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaiseResurrectionAscend.restorePlayer(player);
        }
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaiseResurrectionAscend.suspendPlayer(player);
        }
    }
}
