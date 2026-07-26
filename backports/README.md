# Omni Backports

This folder is the staging area for very old Minecraft targets. Backport targets
only move into the root release matrix after they have a repeatable build and a
basic client smoke test path.

The goal is to keep Omni's behavior consistent while letting the injection
mechanism change by era:

- Modern Stonecutter targets stay in `src/main`, `src/forge112`, and `src/liteloader`.
- Forge 1.14.4, 1.15.x, and 1.16.2 through 1.16.5 are now active in the root
  Stonecutter build; Forge 1.13.x and 1.16.1 remain research targets.
- Forge 1.7.10 through 1.12.2 can use UniMixins, MixinBooter, or direct ASM.
- Legacy Fabric style targets can use their own Loom/mapping lane.
- Pre-sprint Minecraft versions are deferred. For now, backports only target
  versions where vanilla sprint already exists.

## First Backport Milestones

1. Forge 1.13.x and 1.16.1: find clean build lanes for the remaining sprint-era
   Forge gaps around the now-shipped 1.14.4, 1.15.x, and 1.16.2-1.16.5 targets.
2. Forge 1.7.10: prototype a UniMixins module, with the current 1.12 ASM
   transformer as the fallback shape.
3. Forge 1.8.9: try MixinBooter first, then fall back to ASM if the runtime
   hooks are simpler than the build wiring.
4. Legacy Fabric: the complete 35-version buildable normal-release matrix from
   1.3.1 through 1.13.2 is now active in the root build.
5. Beta 1.8 and newer legacy releases: preserve vanilla sprint rules and redirect
   existing sprint behavior rather than inventing new stamina or hunger systems.

## Source Notes

- SpongePowered MixinGradle is a ForgeGradle-focused build plugin for Mixin
  refmaps and reobfuscation.
- CleanroomMC MixinBooter describes support for Minecraft 1.8 through 1.12.2.
- GTNewHorizons UniMixins targets Minecraft 1.7.10, with partial 1.8.9 through
  1.12.2 support.
- Legacy Fabric API documents tested modules across 1.3.x through 1.13.2.
- Minecraft Beta 1.8 introduced sprinting, so anything before that is outside
  this backport track.

## Guardrail

Do not add a backport module to `settings.gradle.kts` until it builds from a clean
checkout. This keeps the 1.2-OVERHAUL release matrix stable as additional loader
research advances in parallel.
