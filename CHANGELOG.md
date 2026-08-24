# Changelog

## 0.6.5

- Remove the ordinary apple's one-heart food effect and its private mob-effect registration.
- Leave all food consumption and healing rules to the consuming pack or other food mods.

## 0.6.4

- Grant 3 seconds of Invisibility on entering the downed state.
- Generate temporary absorption equal to maximum health while preserving the one-minute absorption clock.
- Recover at the lower of maximum health or 20 health.
- Keep all existing in-game recovery and downed-state wording unchanged.

## 0.6.3

### Added

- Absorption capacity while downed, so the full one-minute countdown is represented by ordinary absorption health.
- Full-hunger food and drink handoff support when Let Your Friend Eating! is installed.
- Ordinary apple food effects that heal one heart, including apples eaten normally or fed to a downed player.

### Fixed

- Persist the absorption countdown across reconnects.
- Finish the downed state consistently when recovery succeeds or the absorption clock reaches zero.
- Let vanilla death-protection effects, including Totems of Undying, run before downing is considered.

### Changed

- Simplified downed-state player messages while retaining the persistent HUD and give-up control.
