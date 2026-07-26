# Forge 1.13-1.16 Backports

Forge 1.14.4, 1.15.x, and 1.16.2 through 1.16.5 are in the root Stonecutter
release matrix. Forge 1.13.x and 1.16.1 remain unfinished.

## Build

- Root Stonecutter targets use the shared `src/main` code.
- ForgeGradle includes Sponge Mixin on the compile and annotation-processor
  classpaths.
- Java 8 bytecode.
- Shared source gates handle these MCP package names:
  - `net.minecraft.client.entity.player.ClientPlayerEntity`
  - `net.minecraft.util.MovementInput`
  - `net.minecraft.entity.LivingEntity`
  - `net.minecraft.util.math.Vec3d` for 1.14.x and 1.15.x
  - `net.minecraft.util.math.vector.Vector3d` for 1.16.x

## Hooks

- Sprint eligibility: apply vanilla's forward-sprint threshold to any
  directional input.
- Sprint jump: apply vanilla's forward-only boost in the current input direction.
- Travel scaling: apply the configured direction multiplier only to movement
  from the current travel call.
- Commands: intercept `/omni ...` before it reaches the server.

## Remaining Versions

- Forge 1.13.x needs a working build before it can enter the release matrix.
- Forge 1.16.1 currently fails inside Forge's own source recompilation under
  the current Gradle JVM before Omni code compiles.
