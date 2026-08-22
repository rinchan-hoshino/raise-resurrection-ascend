# Raise & Resurrection & Ascend

**Give players a downed state before final death.**

Raise & Resurrection & Ascend keeps a player from dying immediately when lethal damage would reduce them to zero health. Instead, the player enters a crawling downed state and must be healed back to full health before their absorption clock runs out.

## Default behavior

- Lethal damage puts the player into a downed state instead of killing them immediately.
- A downed player starts at 1 health, remains crawling, and receives 20 absorption health.
- The absorption clock drains by 1/60 health each tick, so the generated bar lasts exactly 1200 ticks, or 1 minute.
- Any normal mechanic that adds absorption extends the downed time; damage, effect removal, and every other normal absorption reduction shorten it without special integrations.
- Reaching zero absorption causes final death. Reaching full health first ends the downed state and clears the remaining clock.
- Healing, regeneration and instant-health effects continue to affect downed players.
- Downed players continue to take normal damage. A lethal follow-up, absorption depletion, or give-up keeps the DamageSource that originally downed them for death messages and death-event consumers.
- Another player can right-click a downed player with an ordinary Apple to restore exactly 2 health (one heart). Survival consumes one Apple; Creative consumes nothing. Potions and golden apples are not special feeding inputs.
- The downed player sees a persistent HUD with the recovery rule and bound give-up key; the vanilla absorption hearts are the visible remaining-time bar.
- The mod does not use, consume or imitate a totem of undying.
- Holding G for 2 seconds gives up and triggers final death.
- `/raise_resurrection_ascend revive <targets>` is an administrator override that fills the targets' health and ends their downed state.

## Configuration

Config file:

```text
config/raise_resurrection_ascend-common.toml
```

Default value:

```toml
[downed]
giveUpHoldTicks = 40
```

## Compatibility

When Let Your Friend Eating! 1.1.4 is installed, players can feed ordinary food, stew, and Eternal Food to another player even at full hunger. The feeding mod still owns item consumption, food effects, statistics, particles, and its configured cooldown.

## Requirements

- Minecraft 1.21.1
- NeoForge
