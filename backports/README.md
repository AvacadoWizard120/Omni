# Omni Backports

This directory tracks old Minecraft versions. A version enters the root release
matrix after it has a repeatable build and a client smoke-test task. See
[`MATRIX.md`](MATRIX.md) for status and behavior requirements.

Released backports use the root source sets: `src/main`, `src/forge112`,
`src/forge114`, `src/legacyfabric`, and `src/liteloader`.

## Directories

- [`forge-1.13-1.16`](forge-1.13-1.16/README.md): ForgeGradle and Mixin notes.
- [`forge-1.7-1.12`](forge-1.7-1.12/README.md): UniMixins, MixinBooter, and ASM
  notes.
- [`legacy-fabric`](legacy-fabric/README.md): Legacy Fabric versions and mapping
  differences.
- [`pre-sprint`](pre-sprint/README.md): scope for versions before Beta 1.8.
- [`shared`](shared/README.md): Java helpers for old toolchains.

## Tooling Notes

- SpongePowered MixinGradle handles Mixin refmaps and reobfuscation.
- CleanroomMC MixinBooter supports Minecraft 1.8 through 1.12.2.
- GTNewHorizons UniMixins targets Minecraft 1.7.10, with partial 1.8.9 through
  1.12.2 support.
- Legacy Fabric API lists tested modules for 1.3.x through 1.13.2.
- Minecraft Beta 1.8 introduced sprinting. Earlier versions are not standard
  backports.

## Build Rule

Add a backport module to `settings.gradle.kts` only after it builds from a clean
checkout.
