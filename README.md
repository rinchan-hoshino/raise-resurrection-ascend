# Raise & Resurrection & Ascend

**Give players a configurable downed state before final death.**

Raise & Resurrection & Ascend keeps a player from dying immediately when lethal damage would reduce them to zero health. Instead, the player enters a crawling downed state for a configurable duration.

## Default behavior

- Lethal damage puts the player into a downed state instead of killing them immediately.
- Downed players are forced into the crawling/swimming pose and stay at 1 health.
- The downed state lasts 600 ticks, or 30 seconds, by default.
- Other players can right-click a downed player with a totem of undying to revive them.
- Totem revival consumes one totem unless the rescuer is in creative mode.
- Kill-to-revive is available but disabled by default.
- Holding G for 2 seconds gives up and triggers final death.
- `/raise_resurrection_ascend revive <targets>` lets datapack functions, commands, or pack mechanics revive downed players.

## Configuration

Config file:

```text
config/raise_resurrection_ascend-common.toml
```

Default values:

```toml
[downed]
downedDurationTicks = 600
giveUpHoldTicks = 40

[revival]
reviveItems = ["minecraft:totem_of_undying"]
consumeReviveItem = true
enableKillRevive = false
```

## Requirements

- Minecraft 1.21.1
- NeoForge
