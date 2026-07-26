# Backport Candidate Matrix

| Era | First Target | Candidate Loader | Injection Strategy | Status |
| --- | --- | --- | --- | --- |
| Forge 1.13-1.16 | 1.14.4 Forge | Root Stonecutter + ForgeGradle/Mixin AP | Mixins into `ClientPlayerEntity`, `MovementInput`, and `LivingEntity` | Partly shipped: 1.14.4, 1.15.x, 1.16.2-1.16.5 |
| Late legacy Forge | 1.8 Forge | LaunchWrapper transformer | ASM coremod shared with 1.12.2 Forge/LiteLoader | Built: 1.8, 1.8.8, 1.8.9, 1.9, 1.9.4, 1.10, 1.10.2, 1.11, 1.11.2, 1.12, 1.12.1, 1.12.2 |
| Classic legacy Forge | 1.7.10 Forge | UniMixins | Mixin first, ASM fallback | Planned |
| Existing legacy | 1.12.2 Forge/LiteLoader | LaunchWrapper transformer | Already active in root build | Shipped |
| Legacy Fabric | 1.3.1-1.13.2 Legacy Fabric | Legacy Fabric/Loom | Version-family mixins with legacy mappings | Built: all 35 buildable normal releases |
| Early release | 1.2.5 or 1.4.7 | ModLoader/Forge/MCP | Direct class patch or ASM | Research |
| Sprint origin | Beta 1.8 | MCP/base patch | Direct class patch | Research |
| Pre-sprint | Alpha/Beta before b1.8 | Deferred | Backporting sprint/stamina/hunger is a separate future feature | Parked |

## Active Scope

The active backport scope is Minecraft Beta 1.8 and newer, because those versions
already have vanilla sprint. Omni should redirect and configure that existing
mechanic. It should not add a new sprint, stamina, or hunger system in this track.

## Shared Behavior Contract

Every backport should keep these behavior promises unless the version predates
vanilla sprint:

- `enabled=false` restores vanilla behavior for that version.
- All multipliers default to `1.0`.
- Direction keys match modern Omni:
  `forward`, `backward`, `left`, `right`, `forward_left`, `forward_right`,
  `backward_left`, `backward_right`.
- Config should remain a plain properties file where possible.
- Commands should use `/omni enable`, `/omni disable`, `/omni status`, and
  `/omni reload` when the target has a usable client chat hook.
