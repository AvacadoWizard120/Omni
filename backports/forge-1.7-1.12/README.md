# Forge 1.7-1.12 Backport Lane

The root build ships 1.8 through 1.12.2 Forge through a LaunchWrapper
transformer, plus the existing 1.12.2 LiteLoader artifact. This lane is still
the place for notes about going older without destabilizing those working
targets.

## Candidate Loaders

- UniMixins for 1.7.10 first.
- Direct ASM/LaunchWrapper transformer for 1.8 through 1.12.2.
- MixinBooter remains a fallback idea only if the ASM transformer cannot cover a
  specific runtime quirk.

## First Targets

1. Runtime-test the built 1.8 through 1.12.1 Forge jars.
2. 1.7.10 Forge, which predates vanilla sprint and belongs to the separate
   "add sprint back" decision.
3. 1.12.2 MixinBooter variant, only as a comparison to the shipped ASM target.

## Hook Shape

The 1.12 transformer is the starting reference:

- Patch `EntityPlayerSP#onLivingUpdate` forward-sprint checks to call an Omni
  hook using `moveForward` and `moveStrafe`.
- Patch `EntityLivingBase#jump` sprint boost to use current input direction.
- Patch `EntityPlayerSP#sendChatMessage` for `/omni ...`.

The transformer now avoids most exact obfuscated member-name dependence by
matching the vanilla sprint bytecode shape and passing discovered field names
into the runtime hook. Runtime testing is still required per target because old
coremod ordering can vary by Forge version.
