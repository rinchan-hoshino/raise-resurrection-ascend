# Raise & Resurrection & Ascend

**Give players a configurable downed state before final death.**

Raise & Resurrection & Ascend keeps a player from dying immediately when lethal damage would reduce them to zero health. Instead, the player enters a crawling downed state for a configurable duration and must be healed back to full health before they can stand again.

## Default behavior

- Lethal damage puts the player into a downed state instead of killing them immediately.
- A downed player starts at 1 health and remains crawling.
- Healing, regeneration and instant-health effects continue to affect downed players.
- Downed players continue to take normal damage. A lethal follow-up, timeout, or give-up keeps the DamageSource that originally downed them for death messages and death-event consumers.
- Another player can right-click a downed player with an ordinary Apple to restore exactly 2 health (one heart). Survival consumes one Apple; Creative consumes nothing. Potions and golden apples are no longer special feeding inputs.
- Reaching full health ends the downed state; partial healing does not.
- The downed player sees a persistent HUD with the recovery rule, remaining time, and bound give-up key.
- The downed state lasts 6000 ticks, or 5 minutes, by default.
- The mod does not use, consume or imitate a totem of undying.
- Holding G for 2 seconds gives up and triggers final death.
- `/raise_resurrection_ascend revive <targets>` is an administrator override that fills the targets' health and ends their downed state.

## Configuration

Config file:

```text
config/raise_resurrection_ascend-common.toml
```

Default values:

```toml
[downed]
downedDurationTicks = 6000
giveUpHoldTicks = 40
```

## Requirements

- Minecraft 1.21.1
- NeoForge
