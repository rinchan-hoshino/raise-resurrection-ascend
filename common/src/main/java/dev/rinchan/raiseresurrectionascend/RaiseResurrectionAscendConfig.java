package dev.rinchan.raiseresurrectionascend;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RaiseResurrectionAscendConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue giveUpHoldTicks;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("downed");
        giveUpHoldTicks = builder
            .comment("Client key hold time before giving up. 40 ticks = 2 seconds.")
            .defineInRange("giveUpHoldTicks", 40, 1, 20 * 60);
        builder.pop();

        SPEC = builder.build();
    }

    private RaiseResurrectionAscendConfig() {
    }
}
