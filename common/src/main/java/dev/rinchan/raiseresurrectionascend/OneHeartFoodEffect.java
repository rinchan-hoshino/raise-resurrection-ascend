package dev.rinchan.raiseresurrectionascend;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class OneHeartFoodEffect extends MobEffect {
    public OneHeartFoodEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE52B50);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.heal(2.0F);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
