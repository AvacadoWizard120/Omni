# Shared Backport Code

This folder contains Java helpers for old modules. The code must not depend on
Minecraft, Forge, Fabric, Mixin, or modern Java APIs. Modules can copy or source
the package after they have a working build.

Use Java 6 language features when possible. Some legacy toolchains require them
even when the game runs on Java 8.
