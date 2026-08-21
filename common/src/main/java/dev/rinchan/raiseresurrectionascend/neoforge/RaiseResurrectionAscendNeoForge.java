package dev.rinchan.raiseresurrectionascend.neoforge;

import dev.rinchan.raiseresurrectionascend.DownedDamagePolicy;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscend;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendConfig;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendGiveUpPacket;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscendStatePacket;
import dev.rinchan.raiseresurrectionascend.client.RaiseResurrectionAscendClient;
import dev.rinchan.raiseresurrectionascend.client.ScreenshotClientHarness;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(RaiseResurrectionAscend.MOD_ID)
public class RaiseResurrectionAscendNeoForge {
    public RaiseResurrectionAscendNeoForge(IEventBus modBus) {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, RaiseResurrectionAscendConfig.SPEC);
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        if (Boolean.getBoolean("raiseResurrectionAscend.screenshot")) {
            ScreenshotServerHarness.register();
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RaiseResurrectionAscendClient.register(modBus);
            if (Boolean.getBoolean("raiseResurrectionAscend.screenshot")) {
                ScreenshotClientHarness.register();
            }
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(RaiseResurrectionAscendGiveUpPacket.TYPE, RaiseResurrectionAscendGiveUpPacket.CODEC, (packet, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    RaiseResurrectionAscend.giveUp(player);
                }
            }).exceptionally(throwable -> null);
        });
        registrar.playToClient(RaiseResurrectionAscendStatePacket.TYPE, RaiseResurrectionAscendStatePacket.CODEC, (packet, context) ->
            context.enqueueWork(() -> RaiseResurrectionAscendClient.applyState(packet)).exceptionally(throwable -> null)
        );
    }

    private void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (RaiseResurrectionAscend.isPendingFinalDeath(player)) {
            event.setNewDamage(0F);
            return;
        }
        if (RaiseResurrectionAscend.isFinalDeath(player)) {
            return;
        }
        float damage = event.getNewDamage();
        if (RaiseResurrectionAscend.isDowned(player)) {
            if (DownedDamagePolicy.finishesDownedState(player.getHealth(), damage)) {
                event.setNewDamage(0F);
                RaiseResurrectionAscend.finishDownedFromDamage(player);
            }
            return;
        }
        if (RaiseResurrectionAscend.shouldEnterDowned(player, damage)) {
            event.setNewDamage(0F);
            RaiseResurrectionAscend.enterDowned(player, event.getSource());
        }
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer feeder) || !(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        if (RaiseResurrectionAscend.tryFeedRecoveryItem(feeder, target, event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        RaiseResurrectionAscend.tick(event.getServer());
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaiseResurrectionAscend.clearPlayer(player);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("raise_resurrection_ascend")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("revive")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(context -> {
                        int count = 0;
                        for (ServerPlayer player : EntityArgument.getPlayers(context, "targets")) {
                            if (RaiseResurrectionAscend.reviveByCommand(player)) {
                                count++;
                            }
                        }
                        return count;
                    }))));
    }
}
