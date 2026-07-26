# Legacy Fabric Backport Lane

This lane is for non-Forge old Minecraft targets where Legacy Fabric, Ornithe,
Babric, or related tooling is a better fit than MCP/ForgeGradle.

## Implemented Targets

The active matrix contains every buildable normal Legacy Fabric release from
Minecraft 1.3.1 through 1.13.2:

- 1.3.1 through 1.3.2
- 1.4.2 and 1.4.4 through 1.4.7
- 1.5.1 through 1.5.2
- 1.6.1, 1.6.2, and 1.6.4
- 1.7.2 through 1.7.8 and 1.7.10
- 1.8 through 1.8.9
- 1.9.4, 1.10.2, 1.11.2, 1.12.2, and 1.13.2

Minecraft 1.7.9 is not buildable because Legacy Fabric publishes neither Yarn
mappings nor loader metadata for that release. All 35 buildable targets compile,
remap, collect, expose a valid `runClient` task graph, and pass static jar/mixin
validation. The original nine boundary targets also passed interactive gameplay
smoke tests.

## Expected Differences

- Package and member names will come from legacy mappings, not modern Mojang
  names.
- Client command hooks may need a chat send mixin instead of packet command hooks.
- Config should still be the same `config/omni.properties` file.
- 1.6.4 and older predate the dedicated sprint key, so their sprint eligibility
  hooks cover double-tap start and continuation without a sprint-key ordinal.
- 1.5.2 and older use the mapped `MobEntity`/`Minecraft` class family rather than
  the later `LivingEntity`/`MinecraftClient` family.

## Success Criteria

A target graduates from this folder when it can build, launch a client, create
the config file, toggle `/omni disable`, and restore vanilla sprint behavior.
