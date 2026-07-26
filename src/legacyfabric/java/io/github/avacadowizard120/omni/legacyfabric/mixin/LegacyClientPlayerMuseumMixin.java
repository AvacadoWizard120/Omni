package io.github.avacadowizard120.omni.legacyfabric.mixin;

import io.github.avacadowizard120.omni.legacyfabric.LegacyMovement;
import net.minecraft.client.input.Input;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public abstract class LegacyClientPlayerMuseumMixin {
    @Redirect(
            method = "method_2651",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/input/Input;movementForward:F",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0
            )
    )
    private float omni$previousDirectionalInput(Input input) {
        return LegacyMovement.directionalForward(input);
    }

    @Redirect(
            method = "method_2651",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/input/Input;movementForward:F",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 2
            )
    )
    private float omni$doubleTapDirectionalInput(Input input) {
        return LegacyMovement.directionalForward(input);
    }

    @Redirect(
            method = "method_2651",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/input/Input;movementForward:F",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 3
            )
    )
    private float omni$continuingDirectionalInput(Input input) {
        return LegacyMovement.directionalForward(input);
    }
}
