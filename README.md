# Raise & Resurrection & Ascend

**Give players a downed state before final death.**

Raise & Resurrection & Ascend keeps a player from dying immediately when lethal damage would reduce them to zero health. Instead, the player enters a crawling downed state and must recover to the lower of maximum health or 20 health before their absorption clock runs out.

## Default behavior

- Lethal damage puts the player into a downed state instead of killing them immediately.
- A downed player starts at 1 health, remains crawling, receives 3 seconds of Invisibility, and gains absorption equal to their maximum health.
- The absorption clock drains one 1200th of maximum health each tick, so the generated bar lasts exactly 1200 ticks, or 1 minute.
- Any normal mechanic that adds absorption extends the downed time; damage, effect removal, and every other normal absorption reduction shorten it without special integrations.
- Reaching zero absorption causes final death. Reaching the lower of maximum health or 20 health first ends the downed state and clears the remaining clock.
- Healing, regeneration and instant-health effects continue to affect downed players.
- Downed players continue to take normal damage. A lethal follow-up, absorption depletion, or give-up keeps the DamageSource that originally downed them for death messages and death-event consumers.
- Food items and food-derived healing are not modified by this mod; consuming packs and food mods own those rules.
- The downed player sees a persistent HUD with the recovery rule and bound give-up key; the vanilla absorption hearts are the visible remaining-time bar.
- Entering the downed state sends one chat message through Minecraft's death-message delivery rules, including `showDeathMessages` and team visibility; final death keeps its separate vanilla death message.
- The mod does not imitate or consume Totems of Undying. Vanilla death-protection effects run first; downing is only the fallback when they do not save the player.
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

When Let Your Friend Eating! 1.1.4 is installed, players can feed ordinary food, stew, and Eternal Food to another player even at full hunger. Players can also give another player any drink-animation consumable during ordinary play; the original milk-bucket path remains authoritative. The feeding mod's configured cooldown and feed/eaten statistics remain shared across food and drink interactions.

## Requirements

- Minecraft 1.21.1
- NeoForge
