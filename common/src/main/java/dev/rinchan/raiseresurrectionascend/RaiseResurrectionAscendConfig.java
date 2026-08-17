package dev.rinchan.raiseresurrectionascend;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RaiseResurrectionAscendConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue downedDurationTicks;
    public static final ModConfigSpec.IntValue giveUpHoldTicks;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("downed");
        downedDurationTicks = builder
            .comment("Ticks before a downed player dies. 6000 ticks = 5 minutes.")
            .defineInRange("downedDurationTicks", 6000, 1, 20 * 60 * 60);
        giveUpHoldTicks = builder
            .comment("Client key hold time before giving up. 40 ticks = 2 seconds.")
            .defineInRange("giveUpHoldTicks", 40, 1, 20 * 60);
        builder.pop();

        SPEC = builder.build();
    }

    private RaiseResurrectionAscendConfig() {
    }
}
