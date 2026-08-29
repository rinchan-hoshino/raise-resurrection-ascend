# Changelog

## 1.0.4

- Align recovery with the visible health threshold and preserve the original downing cause through final-death presentation and persistence.

## 1.0.3

- Runs NeoForge's downed-state `LivingDeathEvent` cancellation at `LOW`, before final-death observers at `LOWEST`; a canceled downing event can no longer create dog tags, increment kill counters, or trigger other final-death cleanup.
- Leaves downed damage pre/post processing and native totem observation at `LOWEST`.

## 1.0.2

- Uses a finite `1,000,000F` native damage amount for final-death dispatch while preserving the reconstructed original source and the existing totem/death pipeline.

## 1.0.1

- Restored the hold-G give-up path as a server-authoritative 40-tick input state: clients report only key transitions, while the server owns completion.
- Routed give-up through the persisted original downing cause and native final-death totem pipeline.
- Restored localized HUD instruction and progress presentation without restoring any feeding, drink, admin, screenshot, or GameTest scope.

## 1.0.0

- Publishes Fabric and NeoForge builds for Minecraft 1.21.1, 26.1.2, and 26.2.
- Redesigned the mod around a server-authoritative, damage-vulnerable downed state while retaining the absorption clock and crawl presentation.
- Healing now revives at the dynamic `min(max health, 20)` threshold shown by the HUD.
- Preserved native Totem of Undying protection on both the initial lethal transition and the original-cause final death attempt.
- Added vanilla-totem assistance: another player may right-click a downed player to consume one totem (except in Creative) and apply the recipient's native totem result.
- Persisted a structured original-cause snapshot with damage type, entity UUID/dimension references, source position, and immutable localized death message across reconnects and restarts.
- Routed timeout and follow-up final deaths back through Minecraft's native damage/death-protection pipeline using a bounded state machine.
- Made the versioned S2C state channel required and reset client state explicitly on login/logout.
- Kept death-message delivery aligned with vanilla game-rule and team visibility.
- Removed friend-drink/feeding integration, Let Your Friend Eating dependencies, give-up controls and networking, admin revive commands, feeding mixins, screenshot harnesses, and GameTest production resources.
- Added focused pure policy/state-machine tests and static public-contract checks.
