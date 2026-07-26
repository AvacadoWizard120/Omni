# Forge 1.13-1.16 Backport Lane

First target: Minecraft Forge 1.14.4.

Forge 1.14.4, 1.15.x, and 1.16.2 through 1.16.5 have graduated into the root
Stonecutter release matrix. This folder now tracks the remaining research and
the compatibility notes learned from that graduation.

## Active Build Shape

- Root Stonecutter targets using the shared `src/main` code.
- ForgeGradle with Sponge Mixin on the compile and annotation-processor classpath.
- Java 8 bytecode.
- Shared source gates for old MCP package names:
  - `net.minecraft.client.entity.player.ClientPlayerEntity`
  - `net.minecraft.util.MovementInput`
  - `net.minecraft.entity.LivingEntity`
  - `net.minecraft.util.math.Vec3d` for 1.14.x and 1.15.x
  - `net.minecraft.util.math.vector.Vector3d` for 1.16.x

## Hook Targets

- Client sprint eligibility: allow any directional input to satisfy the same
  threshold vanilla forward sprint uses.
- Sprint jump: redirect vanilla's forward-only boost to the active input
  direction.
- Travel scaling: apply the configured per-direction multiplier only to movement
  added by the current travel call.
- Client command hook: intercept `/omni ...` before it reaches the server.

## Still Unshipped

- Forge 1.13.x still needs a proven build lane before entering the release
  matrix.
- Forge 1.16.1 currently fails inside Forge's own source recompilation under
  the current Gradle JVM before Omni code compiles.
