# Legacy Fabric Backports

This folder covers non-Forge backports using Legacy Fabric, Ornithe, or Babric.

## Implemented Targets

The root build includes all 35 buildable Legacy Fabric release targets from
Minecraft 1.3.1 through 1.13.2:

- 1.3.1 through 1.3.2
- 1.4.2 and 1.4.4 through 1.4.7
- 1.5.1 through 1.5.2
- 1.6.1, 1.6.2, and 1.6.4
- 1.7.2 through 1.7.8 and 1.7.10
- 1.8 through 1.8.9
- 1.9.4, 1.10.2, 1.11.2, 1.12.2, and 1.13.2

Minecraft 1.7.9 is not buildable because Legacy Fabric publishes neither Yarn
mappings nor loader metadata for it. Each listed target compiles, remaps,
collects, exposes a valid `runClient` task graph, and passes static jar/mixin
validation. The original nine boundary targets also passed gameplay smoke tests.

## Compatibility Notes

- Package and member names come from legacy mappings, not modern Mojang names.
- Client commands may require a chat-send mixin instead of packet command hooks.
- Use the same `config/omni.properties` file.
- 1.6.4 and older predate the dedicated sprint key, so their sprint eligibility
  hooks cover double-tap start and continuation without a sprint-key ordinal.
- 1.5.2 and older use the mapped `MobEntity`/`Minecraft` class family rather than
  the later `LivingEntity`/`MinecraftClient` family.

## Smoke Test

Build the target, launch a client, confirm that it creates the config file, then
use `/omni disable` and verify that vanilla sprint behavior returns.
