# Omni-Overhaul

Fresh Stonecutter workspace for a tiny client-side omni-directional sprint mod.

## Supported targets

- Fabric for Minecraft 1.14.4 through 1.21.11, and 26.1 through 26.2
- Forge for Minecraft 1.12.2, 1.14.4, 1.15 through 1.15.2, 1.16.2 through 1.16.5, 1.17.1, 1.18 through 1.20.4, 1.20.6 through 1.21.1, 1.21.3 through 1.21.11, and 26.1.1 through 26.2
- Quilt for Minecraft 1.20.1, 1.20.4, 1.21.1 through 1.21.11, and 26.1 through 26.2
- NeoForge for Minecraft 1.20.4, 1.21.1 through 1.21.11, and 26.1 through 26.2
- LiteLoader for Minecraft 1.12.2 as a separate Java 8 legacy target
- Legacy Fabric for every buildable normal release from Minecraft 1.3.1 through
  1.13.2: 1.3.1 through 1.3.2; 1.4.2 and 1.4.4 through 1.4.7; 1.5.1 through
  1.5.2; 1.6.1, 1.6.2, and 1.6.4; 1.7.2 through 1.7.8 and 1.7.10; 1.8 through
  1.8.9; 1.9.4; 1.10.2; 1.11.2; 1.12.2; and 1.13.2 as Java 8 client targets

## Design

- One repository
- Shared modern sources plus dedicated Java 8 source sets for legacy loaders
- No user-facing runtime library dependency
- Stonecutter handles the version/loader matrix
- The sprint behavior is changed with focused client mixins and legacy 1.12 transformers

That means vanilla still owns hunger, sprint speed, exhaustion, and the rest of the sprint pipeline.

## Runtime controls

Omni writes `config/omni.properties` the first time it needs config. Defaults match vanilla Omni 1.1 behavior: the mod is enabled, and every speed multiplier is `1.0`.

In game, use:

```text
/omni status
/omni enable
/omni disable
/omni reload
```

The config is intentionally plain:

```properties
enabled=true
forward_multiplier=1.0
backward_multiplier=1.0
left_multiplier=1.0
right_multiplier=1.0
forward_left_multiplier=1.0
forward_right_multiplier=1.0
backward_left_multiplier=1.0
backward_right_multiplier=1.0
```

Multipliers are relative to vanilla sprint speed. `1.0` is unchanged, `0.5` is half speed, and `2.0` is double speed.

## Tooling

- Gradle wrapper: 9.5.0
- Stonecutter: 0.9
- Fabric Loom Remap plugin for Fabric
- Quilt Loom for Quilt
- Fabric Loom Remap plus Legacy Looming for Legacy Fabric
- NeoForged ModDev / LegacyForge plugins for NeoForge and Forge
- A standalone Java/LiteLoader build for the 1.12.2 `.litemod`

## Common commands

Build one target:

```bash
./gradlew :1.21.1-fabric:build
./gradlew :1.21.1-quilt:build
./gradlew :1.20.1-forge:build
./gradlew :1.20.4-neoforge:build
./gradlew :1.12.2-liteloader:build
./gradlew --configure-on-demand :1.13.2-legacyfabric:buildAndCollect
```

Collect and publish:

```bash
./gradlew buildAndCollect
./gradlew modrinthPlan
./gradlew modrinthValidate
./gradlew modrinthPublish
```

Bulk client smoke-test:

```bash
./gradlew runClientMatrixPlan
./gradlew runClientMatrix
python tools/run_client_matrix.py --loader fabric --from-target 1.20.1-fabric
python tools/run_client_matrix.py --loader legacyfabric
```

Run one target in dev:

```bash
./gradlew :1.21.1-fabric:runClient
./gradlew :1.21.1-quilt:runClient
./gradlew :1.20.1-forge:runClient
./gradlew :1.20.4-neoforge:runClient
./gradlew --configure-on-demand :1.13.2-legacyfabric:runClient
```

## Notes

- Fabric builds are pure mixin/metadata builds; there is no Fabric API dependency.
- Quilt builds mirror the Fabric mixin-only shape and do not depend on QSL or Quilted Fabric API.
- Forge uses a standalone 1.12.2 coremod build, LegacyForge for 1.20.1, and ForgeGradle for modern Forge targets.
- NeoForge uses `neoforge.mods.toml`, and the mixin config is declared there with `[[mixins]]`.
- LiteLoader uses `litemod.json` plus a LaunchWrapper class transformer. It patches
  Minecraft 1.12.2's obfuscated/MCP sprint checks directly instead of reusing the
  named modern mixins.
- Legacy Fabric builds are mixin-only Java 8 jars and do not depend on Legacy
  Fabric API. Version-specific mixin families cover the movement, jump, local
  command, and pre-sprint-key differences in the museum releases.
- Modrinth publishing uses `modrinth-publish.local.json`, which is ignored by git.
  Each artifact is published as its own Modrinth version so its loader and Minecraft
  version metadata stays exact.
- Legacy Fabric artifacts are tagged `legacy-fabric` on Modrinth and are
  intentionally excluded from the CurseForge publishing lane.
- Bulk client smoke-testing queues launchable `runClient` targets one at a time.
  A normal Minecraft quit should return exit code `0`; pass
  `--ok-exit-code -1` to `tools/run_client_matrix.py` if a launcher reports that
  as its normal close code.

## Loader expansion notes

- Experimental old-version research lives under `backports/`. The active Legacy
  Fabric matrix contains all 35 normal releases with published loader and Yarn
  metadata from Minecraft 1.3.1 through 1.13.2; every selected release already
  has vanilla sprint. Minecraft 1.7.9 is the sole normal-release omission because
  Legacy Fabric publishes neither mappings nor loader metadata for it. Builds are
  collected only after their mapping-specific mixin targets pass compilation and
  bytecode checks.
- Fabric 1.14 through 1.14.3 need a Yarn/intermediary mapping lane because official
  Mojang mappings are not available there.
- Forge 1.14.4, 1.15.x, and 1.16.2 through 1.16.5 now use the
  pre-1.17 package gates in the shared source. Forge 1.13.x and 1.16.1 remain
  outside the release matrix until their build tooling can be proven cleanly.
- Forge has no releases for Minecraft 1.20.5 or 1.21.2. Forge 26.1 exists, but
  currently fails during Forge's own FieldToMethodTransformer bootstrap.
- Future loader lanes to consider from Modrinth's list: Babric, BTA (Babric),
  Ornithe, NilLoader, Rift, Risugami's ModLoader, and Java Agent.
