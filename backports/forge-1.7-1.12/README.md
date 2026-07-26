# Forge 1.7-1.12 Backports

The root build ships a LaunchWrapper transformer for Forge 1.8 through 1.12.2,
plus the 1.12.2 LiteLoader artifact. Forge 1.7.10 remains planned.

## Candidate Loaders

- UniMixins for 1.7.10 first.
- Direct ASM/LaunchWrapper transformer for 1.8 through 1.12.2.
- MixinBooter only if the ASM transformer cannot handle a runtime issue.

## Priorities

1. Runtime-test the built 1.8 through 1.12.1 Forge jars.
2. Implement and runtime-test Forge 1.7.10 with UniMixins.
3. Compare a 1.12.2 MixinBooter variant with the shipped ASM target.

## Hooks

Use the 1.12 transformer as the reference:

- Patch `EntityPlayerSP#onLivingUpdate` forward-sprint checks to call an Omni
  hook using `moveForward` and `moveStrafe`.
- Patch `EntityLivingBase#jump` sprint boost to use current input direction.
- Patch `EntityPlayerSP#sendChatMessage` for `/omni ...`.

The transformer matches vanilla sprint bytecode and passes discovered field
names to the runtime hook, avoiding most exact obfuscated member names. Each
target still needs runtime testing because Forge coremod ordering varies by
version.
