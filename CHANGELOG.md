# Changelog

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
