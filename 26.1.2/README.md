# MITE Legacy Port (Minecraft 26.1.2)

This module is a **bootstrap port target** for migrating legacy MITE 1.6.4 jar-mod behavior to modern Minecraft Java 26.1.2 using Fabric.

## Current state

- Build system: Gradle + Fabric Loom (26.1.2)
- Loader: Fabric Loader 0.19.2
- Java target: 25
- Scope migrated in this stage:
  - Modern mod bootstrap and launch wiring
  - Subsystem isolation scaffolding for progressive migration
  - Hardcore-like defaults on server start (difficulty hard + locked, no natural regen, keepInventory=false)
  - Persistent `player_data` store (world saved data) for per-player MITE state
  - Hunger pressure baseline wired to persistent player data:
    - periodic extra exhaustion for survival players
    - persisted nutrition snapshot, MITE exhaustion counter, and tick progression
  - Player lifecycle hooks for `player_data`:
    - join world / leave world
    - death hook (strict hardcore handling)
    - respawn copy/sync hooks
    - world save checkpoints
    - server stopping flush
  - `data_version` strategy:
    - current schema stays at `v1`
    - migration entrypoint prepared (`v1 -> v2` placeholder)

## Current player_data lifecycle policy

- Player data is keyed by UUID and never dropped automatically on leave/death.
- Join world:
  - creates default data if absent
  - refreshes runtime flags (`hardcore_rules_active`, `survival_rules_active`)
- Leave world:
  - refreshes flags and keeps persisted values
- Death:
  - death is always processed (no cancellation)
  - in hardcore rules context, data is preserved strictly
- Respawn:
  - data is copied from previous player entity via Fabric respawn hooks
  - hardcore rules: keep state as-is
  - non-hardcore/dev: restore state but reset transient nutrition tick accumulator to `0`
- Save/stop:
  - world save checkpoint hook is active
  - server stopping forces a final data flush

## Legacy source constraints

The repository currently contains only compiled `.class` files from `MITE 1.6.4` and no source/build metadata for the legacy codebase.
Because of this, migration proceeds incrementally with explicit subsystem stubs and documented gaps.

## Next migration increments

1. Extend hardcore mode to full permadeath semantics and world-creation UX.
2. Port nutrition depth (food quality, starvation tiers, penalties) and damage pipeline.
3. Port recipes/progression and block/item registration.
4. Port mobs/AI/worldgen with modern data-driven assets.
5. Replace stubs with validated gameplay modules one by one.
