package dev.rinchan.raiseresurrectionascend.gametest;

import com.mojang.authlib.GameProfile;
import dev.rinchan.raiseresurrectionascend.RaiseResurrectionAscend;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RaiseResurrectionAscend.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TotemPriorityGameTests {
    private TotemPriorityGameTests() {
    }

    @GameTest(template = "empty")
    public static void heldTotemPreventsInitialDowning(GameTestHelper helper) {
        ServerPlayer player = freshPlayer(helper, "initial_totem");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));

        player.hurt(player.damageSources().generic(), 100.0F);

        helper.assertTrue(player.getItemInHand(InteractionHand.OFF_HAND).isEmpty(),
            "The vanilla death-protection path must consume the held totem");
        helper.assertTrue(player.isAlive(), "The held totem must leave the player alive");
        helper.assertTrue(!RaiseResurrectionAscend.isDowned(player),
            "A successful totem must preempt the initial downed transition");
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lethalDamageWithoutTotemEntersDowned(GameTestHelper helper) {
        ServerPlayer player = freshPlayer(helper, "initial_downed");

        player.hurt(player.damageSources().generic(), 100.0F);

        helper.assertTrue(player.isAlive(), "The downed transition must cancel the initial death");
        helper.assertTrue(RaiseResurrectionAscend.isDowned(player),
            "Lethal damage without death protection must enter the downed state");
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void downedProtectionAndRecoveryScaleWithMaximumHealth(GameTestHelper helper) {
        ServerPlayer player = freshPlayer(helper, "scaled_downed");
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D);
        player.setHealth(40.0F);

        RaiseResurrectionAscend.enterDowned(player, player.damageSources().generic());

        helper.assertTrue(player.hasEffect(MobEffects.INVISIBILITY),
            "Entering the downed state must grant temporary Invisibility");
        helper.assertTrue(player.getEffect(MobEffects.INVISIBILITY).getDuration() == 60,
            "The downed Invisibility effect must last exactly three seconds");
        helper.assertTrue(player.getAbsorptionAmount() == 40.0F,
            "The generated temporary shield must equal maximum health");

        player.setHealth(19.99F);
        helper.assertTrue(!RaiseResurrectionAscend.recoverIfThresholdReached(player),
            "A player with more than 20 maximum health must remain downed below 20 health");

        player.setHealth(20.0F);
        helper.assertTrue(RaiseResurrectionAscend.recoverIfThresholdReached(player),
            "A player with more than 20 maximum health must recover at 20 health");
        helper.assertTrue(!RaiseResurrectionAscend.isDowned(player),
            "Successful threshold recovery must clear the downed state");
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heldTotemCanProtectLethalDamageWhileDowned(GameTestHelper helper) {
        ServerPlayer player = freshPlayer(helper, "downed_totem");
        RaiseResurrectionAscend.enterDowned(player, player.damageSources().generic());
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));

        player.hurt(player.damageSources().generic(), player.getAbsorptionAmount());
        boolean protectionResolved = RaiseResurrectionAscend.resolveDamageProtection(player);

        helper.assertTrue(player.getItemInHand(InteractionHand.OFF_HAND).isEmpty(),
            "Lethal downed damage must still reach the vanilla totem check");
        helper.assertTrue(protectionResolved && player.isAlive(),
            "Successful death protection must resolve the pending downed death");
        helper.assertTrue(!RaiseResurrectionAscend.isDowned(player),
            "A successful totem must clear the downed state");
        helper.assertTrue(player.getAbsorptionAmount() == 8.0F,
            "Resolving the downed state must preserve the totem's four absorption hearts");
        cleanup(player);
        helper.succeed();
    }

    private static ServerPlayer freshPlayer(GameTestHelper helper, String name) {
        UUID id = UUID.nameUUIDFromBytes((RaiseResurrectionAscend.MOD_ID + ':' + name)
            .getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(id, name);
        ServerPlayer player = new ServerPlayer(
            helper.getLevel().getServer(),
            helper.getLevel(),
            profile,
            ClientInformation.createDefault()
        );
        player.connection = new SilentConnectionListener(helper, player);
        for (int tick = 0; tick < 61; tick++) {
            player.tick();
        }
        cleanup(player);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static void cleanup(ServerPlayer player) {
        RaiseResurrectionAscend.clearPlayer(player);
        player.removeAllEffects();
        player.setAbsorptionAmount(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }

    private static final class SilentConnectionListener extends ServerGamePacketListenerImpl {
        private SilentConnectionListener(GameTestHelper helper, ServerPlayer player) {
            super(
                helper.getLevel().getServer(),
                new Connection(PacketFlow.SERVERBOUND),
                player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)
            );
        }

        @Override
        public void send(Packet<?> packet) {
        }

        @Override
        public void send(Packet<?> packet, PacketSendListener sendListener) {
        }
    }
}
