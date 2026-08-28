# Raise & Resurrection & Ascend

A Fabric and NeoForge mod that replaces a player's first lethal outcome with a server-authoritative, damage-vulnerable downed state.

## How it works

- Native death protection runs first. A held Totem of Undying can save the initial lethal hit normally.
- Otherwise, lethal damage downs the player at 1 health, starts the existing absorption clock, and forces the crawling presentation.
- Further damage consumes that absorption clock. When the clock expires or follow-up damage finishes it, the original downing source re-enters Minecraft's native damage and death-protection pipeline.
- A held Totem of Undying can therefore also protect the final death attempt.
- Healing revives the player at `min(max health, 20)` health. The HUD displays that current server-authoritative threshold.
- Holding **G** for 40 server ticks gives up. The client sends only key down/up transitions; the server owns the timer and resolves the resulting death through the original downing source and native totem pipeline.
- Another player can right-click a downed player while holding a vanilla Totem of Undying. One totem is consumed unless the helper is in Creative mode, and the recipient receives the native totem result, effects, advancement/stat trigger, game event, and animation.

The original downing cause survives reconnects and server restarts as a structured snapshot: damage type, entity UUIDs and dimensions, source position, and the immutable localized death message. Final deaths keep that original cause even when referenced entities are no longer loaded. A legacy or corrupted downed record with no verifiable original cause is safely cleared instead of inventing a different death source.

Death and downed announcements respect vanilla `showDeathMessages` and team death-message visibility.

## Compatibility recommendations

RRA has no compile-time or metadata dependency on other gameplay mods. For a broader co-op recovery setup, consider:

- **Let Your Friend Eating** for ordinary friend-feeding interactions.
- **[Let Your Friend Drink](https://github.com/rinchan-hoshino/let-your-friend-drink)** for native drink-item completion on the recipient (Minecraft 1.21.1, 26.1.2, and 26.2 on Fabric or NeoForge).
- An **Always Eat / No Hunger** style mod so healing foods remain usable when the hunger bar would normally prevent eating.

These are recommendations only and are not required by RRA.

## Supported versions

| Minecraft | Fabric | NeoForge | Java |
|---|---:|---:|---:|
| 1.21.1 | ✓ | ✓ | 21 |
| 26.1.2 | ✓ | ✓ | 25 |
| 26.2 | ✓ | ✓ | 25 |

Install the matching loader-specific RRA JAR on both client and server. Fabric builds require Fabric API. RRA's versioned S2C state and C2S input channels are required.

## License

GPL-3.0-or-later
