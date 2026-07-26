# Omni-Overhaul

A client-side omni-directional sprint mod built with Stonecutter.

## Supported targets

- Fabric for Minecraft 1.14.4 through 1.21.11, and 26.1 through 26.2
- Forge for Minecraft 1.8, 1.8.8, 1.8.9, 1.9, 1.9.4, 1.10, 1.10.2,
  1.11, 1.11.2, 1.12 through 1.12.2, 1.14.4, 1.15 through 1.15.2,
  1.16.2 through 1.16.5, 1.17.1, 1.18 through 1.20.4, 1.20.6 through
  1.21.1, 1.21.3 through 1.21.11, and 26.1.1 through 26.2
- Quilt for Minecraft 1.20.1, 1.20.4, 1.21.1 through 1.21.11, and 26.1 through 26.2
- NeoForge for Minecraft 1.20.4, 1.21.1 through 1.21.11, and 26.1 through 26.2
- LiteLoader for Minecraft 1.12.2 as a separate Java 8 legacy target
- Legacy Fabric for all 35 buildable releases from Minecraft 1.3.1 through
  1.13.2: 1.3.1 through 1.3.2; 1.4.2 and 1.4.4 through 1.4.7; 1.5.1 through
  1.5.2; 1.6.1, 1.6.2, and 1.6.4; 1.7.2 through 1.7.8 and 1.7.10; 1.8 through
  1.8.9; 1.9.4; 1.10.2; 1.11.2; 1.12.2; and 1.13.2

## Build

- Shared sources for modern loaders and separate Java 8 sources for legacy loaders
- No extra runtime libraries
- Stonecutter manages the version/loader matrix
- Client mixins and legacy transformers patch sprint checks

Vanilla controls hunger and exhaustion. Omni multiplies vanilla sprint speed.

## Runtime controls

Omni creates `config/omni.properties` on first launch. The mod starts enabled,
and every speed multiplier defaults to `1.0`.

In-game commands:

```text
/omni status
/omni enable
/omni disable
/omni reload
```

Default `config/omni.properties`:

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

Speed multipliers are relative to vanilla sprint speed. `1.0` is unchanged, `0.5` is half speed, and `2.0` is double speed.

## Tooling

- Gradle wrapper: 9.5.0
- Stonecutter: 0.9
- Fabric Loom Remap plugin for Fabric
- Quilt Loom for Quilt
- Fabric Loom Remap plus Legacy Looming for Legacy Fabric
- NeoForged ModDev / LegacyForge plugins for NeoForge and Forge
- A standalone Java/LiteLoader build produces the 1.12.2 `.litemod`

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

- Fabric and Quilt builds use only mixins and metadata. They do not depend on
  Fabric API, QSL, or Quilted Fabric API.
- Forge uses a standalone coremod build for 1.8 through 1.12.2, LegacyForge for
  1.20.1, and ForgeGradle for newer targets.
- NeoForge uses `neoforge.mods.toml`, and the mixin config is declared there with `[[mixins]]`.
- LiteLoader uses `litemod.json` plus a LaunchWrapper class transformer. It
  directly patches Minecraft 1.12.2's obfuscated/MCP sprint checks instead of
  using the named modern mixins.
- Legacy Fabric builds are Java 8 mixin jars with no Legacy Fabric API
  dependency. Version-specific mixins handle movement, jumping, local commands,
  and versions without a sprint key.
- Modrinth publishing uses `modrinth-publish.local.json`, which is ignored by git.
  Each artifact gets its own Modrinth version with matching loader and Minecraft
  version metadata.
- Legacy Fabric artifacts are tagged `legacy-fabric` on Modrinth and are not
  published to CurseForge.
- Bulk client smoke tests run launchable `runClient` targets one at a time.
  A normal Minecraft quit should return exit code `0`; pass
  `--ok-exit-code -1` to `tools/run_client_matrix.py` if a launcher reports that
  as its normal close code.

## Loader gaps

- Old-version work lives under `backports/`. Legacy Fabric 1.7.9 is omitted
  because it has no published mappings or loader metadata. Collection requires
  compilation and bytecode checks for each mapping set.
- Fabric 1.14 through 1.14.3 need Yarn/intermediary mappings because official
  Mojang mappings are not available there.
- Forge 1.14.4, 1.15.x, and 1.16.2 through 1.16.5 use the
  pre-1.17 package gates in the shared source. Forge 1.13.x and 1.16.1 remain
  outside the release matrix until their build tooling works reliably.
- Forge has no releases for Minecraft 1.20.5 or 1.21.2. Forge 26.1 exists, but
  currently fails during Forge's own FieldToMethodTransformer bootstrap.
- Possible loaders from Modrinth's list: Babric, BTA (Babric),
  Ornithe, NilLoader, Rift, Risugami's ModLoader, and Java Agent.
