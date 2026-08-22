package dev.rinchan.raiseresurrectionascend;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AppleHealing {
    public static final float HEALING = 2.0F;

    private AppleHealing() {
    }

    public static boolean apply(Player recipient, ItemStack consumed) {
        if (recipient == null || consumed == null || !recipient.isAlive() || !consumed.is(Items.APPLE)) {
            return false;
        }
        recipient.heal(HEALING);
        return true;
    }
}
