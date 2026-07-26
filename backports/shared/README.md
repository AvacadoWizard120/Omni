# Shared Backport Code

This folder holds pure Java helper code for old modules. It must not depend on
Minecraft, Forge, Fabric, Mixin, or modern Java APIs. Backport modules can copy or
source this package once they have their own build.

Keep this code compatible with Java 6 language features where practical. Some
legacy toolchains still run on very old compiler assumptions even when the user
launches the game on Java 8.
