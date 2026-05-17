package dev.rinchan.downedrevival;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class DownedRevivalConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue downedDurationTicks;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> reviveItems;
    public static final ModConfigSpec.BooleanValue consumeReviveItem;
    public static final ModConfigSpec.BooleanValue enableKillRevive;
    public static final ModConfigSpec.IntValue giveUpHoldTicks;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("downed");
        downedDurationTicks = builder
            .comment("Ticks before a downed player dies. 600 ticks = 30 seconds.")
            .defineInRange("downedDurationTicks", 600, 1, 20 * 60 * 60);
        giveUpHoldTicks = builder
            .comment("Client key hold time before giving up. 40 ticks = 2 seconds.")
            .defineInRange("giveUpHoldTicks", 40, 1, 20 * 60);
        builder.pop();

        builder.push("revival");
        reviveItems = builder
            .comment("Items that can revive a downed player when another player right-clicks them.", "Entries are item ids, for example \"minecraft:totem_of_undying\".")
            .defineListAllowEmpty("reviveItems", List.of("minecraft:totem_of_undying"), value -> value instanceof String);
        consumeReviveItem = builder
            .comment("Consume one revive item from non-creative rescuers.")
            .define("consumeReviveItem", true);
        enableKillRevive = builder
            .comment("If true, a downed player revives when they kill a living entity.")
            .define("enableKillRevive", false);
        builder.pop();

        SPEC = builder.build();
    }

    private DownedRevivalConfig() {
    }
}
