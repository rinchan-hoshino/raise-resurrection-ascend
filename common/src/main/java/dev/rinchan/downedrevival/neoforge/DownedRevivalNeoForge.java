package dev.rinchan.downedrevival.neoforge;

import net.minecraft.commands.arguments.EntityArgument;
import dev.rinchan.downedrevival.DownedRevival;
import dev.rinchan.downedrevival.DownedRevivalConfig;
import dev.rinchan.downedrevival.DownedRevivalGiveUpPacket;
import dev.rinchan.downedrevival.client.DownedRevivalClient;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(DownedRevival.MOD_ID)
public class DownedRevivalNeoForge {
    public DownedRevivalNeoForge(IEventBus modBus) {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, DownedRevivalConfig.SPEC);
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            DownedRevivalClient.register(modBus);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(DownedRevivalGiveUpPacket.TYPE, DownedRevivalGiveUpPacket.CODEC, (packet, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    DownedRevival.giveUp(player);
                }
            }).exceptionally(throwable -> null);
        });
    }

    private void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (DownedRevival.isFinalDeath(player)) {
            return;
        }
        if (DownedRevival.isDowned(player)) {
            event.setNewDamage(0F);
            return;
        }
        if (DownedRevival.shouldEnterDowned(player, event.getNewDamage())) {
            event.setNewDamage(0F);
            DownedRevival.enterDowned(player);
        }
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer && event.getEntity() instanceof LivingEntity victim && victim != killer) {
            DownedRevival.tryKillRevive(killer);
        }
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer rescuer) || !(event.getTarget() instanceof ServerPlayer downed)) {
            return;
        }
        if (DownedRevival.tryReviveWithItem(rescuer, downed, event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        DownedRevival.tick(event.getServer());
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DownedRevival.clearPlayer(player);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("downed_revival")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("revive")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(context -> {
                        int count = 0;
                        for (ServerPlayer player : EntityArgument.getPlayers(context, "targets")) {
                            if (DownedRevival.reviveByCommand(player)) {
                                count++;
                            }
                        }
                        return count;
                    }))));
    }
}
