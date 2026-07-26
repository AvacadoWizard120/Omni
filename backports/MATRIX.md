# Backport Matrix

| Versions | First Target | Loader or Tooling | Injection Strategy | Status |
| --- | --- | --- | --- | --- |
| Forge 1.13-1.16 | 1.14.4 Forge | Root Stonecutter + ForgeGradle/Mixin AP | Mixins into `ClientPlayerEntity`, `MovementInput`, and `LivingEntity` | Built: 1.14.4, 1.15.x, 1.16.2-1.16.5; 1.13.x and 1.16.1 pending |
| Forge 1.8-1.12 | 1.8 Forge | LaunchWrapper transformer | ASM coremod shared with 1.12.2 LiteLoader | Built: Forge 1.8, 1.8.8, 1.8.9, 1.9, 1.9.4, 1.10, 1.10.2, 1.11, 1.11.2, 1.12, 1.12.1, and 1.12.2; LiteLoader 1.12.2 shipped |
| Forge 1.7.10 | 1.7.10 Forge | UniMixins | Mixin first, ASM fallback | Planned |
| Legacy Fabric 1.3.1-1.13.2 | 1.3.1-1.13.2 Legacy Fabric | Legacy Fabric/Loom | Version-family mixins with legacy mappings | Built: all 35 buildable release targets |
| Release 1.2-1.4 | 1.2.5 or 1.4.7 | ModLoader/Forge/MCP | Direct class patch or ASM | Research |
| Beta 1.8 | Beta 1.8 | MCP/base patch | Direct class patch | Research |
| Before Beta 1.8 | Alpha/Beta before b1.8 | Deferred | Separate artifacts that add sprint | Deferred |

## Scope

Backports cover Minecraft Beta 1.8 and newer, where vanilla sprint exists. They
configure that mechanic without adding sprint, stamina, or hunger systems.

## Required Behavior

For versions with vanilla sprint:

- `enabled=false` restores vanilla behavior for that version.
- All multipliers default to `1.0`.
- Direction keys match modern Omni:
  `forward`, `backward`, `left`, `right`, `forward_left`, `forward_right`,
  `backward_left`, `backward_right`.
- Use `config/omni.properties` where possible.
- Commands should use `/omni enable`, `/omni disable`, `/omni status`, and
  `/omni reload` when the target has a usable client chat hook.
