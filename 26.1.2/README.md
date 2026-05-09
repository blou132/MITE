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

## Debug validation commands (dev/op)

- `/mite debug playerdata` or `/mite debug dump`
  - print current persisted player data + runtime food level + active scenario state
- `/mite debug setnutrition <value>`
  - updates persisted nutrition and current food level
- `/mite debug setexhaustion <value>`
  - updates persisted MITE exhaustion
- `/mite debug setticks <value>`
  - updates persisted nutrition tick accumulator
- `/mite debug killtest`
  - non-hardcore/dev kill test (forces immediate respawn + validates reset behavior)
- `/mite debug killtest_hardcore`
  - hardcore-style kill test (forces immediate respawn + validates strict preservation)
- `/mite debug autotest start|status|stop`
  - scripted 2-phase scenario: dev respawn validation then hardcore respawn validation
- `/mite debug savenow`
  - forces immediate `SavedData` flush

These commands are restricted to local owner/op context and are intended only for migration validation.

## Automated death/respawn smoke scenario

- World name trigger: `MITE_DEATH_RESPAWN_TEST`
- Or flag trigger: create `.mite_playerdata_autotest.flag` in the world root save folder
- On player join in this world, the mod auto-starts a scripted scenario:
  1. apply non-hardcore/dev rules, set test data, force real death, verify respawn reset policy;
  2. apply hardcore-like rules, set test data, force real death, verify strict respawn preservation;
  3. force save flush and emit pass/fail logs.
- The scenario includes a server-side forced-respawn fallback (20 ticks) for environments where client respawn acknowledgement is delayed.
- Validation accepts expected hunger-exhaustion progression between death and respawn, while still enforcing:
  - non-hardcore/dev: tick accumulator reset to `0`;
  - hardcore-like: no lifecycle reset of progression state.
- Key proof logs:
  - `MITE debug scenario DEV respawn PASS ...`
  - `MITE debug scenario HARDCORE respawn PASS ...`

## Legacy source constraints

The repository currently contains only compiled `.class` files from `MITE 1.6.4` and no source/build metadata for the legacy codebase.
Because of this, migration proceeds incrementally with explicit subsystem stubs and documented gaps.

## Next migration increments

1. Extend hardcore mode to full permadeath semantics and world-creation UX.
2. Port nutrition depth (food quality, starvation tiers, penalties) and damage pipeline.
3. Port recipes/progression and block/item registration.
4. Port mobs/AI/worldgen with modern data-driven assets.
5. Replace stubs with validated gameplay modules one by one.
