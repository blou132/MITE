# MITE Legacy Port (Minecraft 26.1.2)

This module is a **bootstrap port target** for migrating legacy MITE 1.6.4 jar-mod behavior to modern Minecraft Java 26.1.2 using Fabric.

## Current state

- Build system: Gradle + Fabric Loom (26.1.2)
- Loader: Fabric Loader 0.19.2
- Java target: 25
- Scope migrated in this stage:
  - Modern mod bootstrap and launch wiring
  - Subsystem isolation scaffolding for progressive migration
  - Server lifecycle and server tick hooks reserved for gameplay parity work

## Legacy source constraints

The repository currently contains only compiled `.class` files from `MITE 1.6.4` and no source/build metadata for the legacy codebase.
Because of this, migration proceeds incrementally with explicit subsystem stubs and documented gaps.

## Next migration increments

1. Implement hardcore defaults and survival rules in `hardcore_difficulty`.
2. Port hunger/nutrition and damage pipeline.
3. Port recipes/progression and block/item registration.
4. Port mobs/AI/worldgen with modern data-driven assets.
5. Replace stubs with validated gameplay modules one by one.
