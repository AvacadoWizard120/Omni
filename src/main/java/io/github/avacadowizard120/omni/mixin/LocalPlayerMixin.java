package io.github.avacadowizard120.omni.mixin;

import io.github.avacadowizard120.omni.InputAccess;
import io.github.avacadowizard120.omni.MovementAccess;
import io.github.avacadowizard120.omni.OmniConfig;
//? if legacy_forge_pre17 {
/*import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.MovementInput;
*///?} else if legacy_input {
import net.minecraft.client.player.Input;
//?}
//? if client_input {
/*import net.minecraft.client.player.ClientInput;*/
//?}
//? if legacy_forge_pre17 {
/*// Forge 1.16.5 uses the pre-1.17 MCP client player package.*/
//?} else {
import net.minecraft.client.player.LocalPlayer;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if legacy_forge_pre17 {
/*@Mixin(
        value = ClientPlayerEntity.class,
        remap = false
)*/
//?} else {
@Mixin(
        value = LocalPlayer.class
        //? if no_mappings {
        /*, remap = false*/
        //?}
        //? if modern_forge {
        /*, remap = false*/
        //?}
)
//?}
public abstract class LocalPlayerMixin {
    //? if legacy_forge_pre17 {
    /*// Forge field access is handled through InputAccess. Mixin 0.8.4 rejects aliases for this public target field.*/
    //?} else if legacy_input {
    //? if forge_reflective_access {
    /*// Forge field access is handled through InputAccess. Mixin 0.8.4 rejects aliases for this public target field.*/
    //?} else {
    @Shadow @Final public Input input;
    //?}
    //?}
    //? if client_input {
    //? if modern_forge {
    /*// Forge field access is handled through InputAccess. Mixin 0.8.4 rejects aliases for this public target field.*/
    //?} else {
    /*@Shadow @Final public ClientInput input;*/
    //?}
    //?}

    //? if impulse_input {
    //? if legacy_forge_pre17 {
    /*@Inject(method = {"hasEnoughImpulseToStartSprinting", "func_223110_ee", "method_20623", "ee", "eI"}, at = @At("HEAD"), cancellable = true, require = 0)*/
    //?} else if modern_forge {
    /*@Inject(method = {"hasEnoughImpulseToStartSprinting", "m_108733_"}, at = @At("HEAD"), cancellable = true)*/
    //?} else {
    @Inject(method = "hasEnoughImpulseToStartSprinting", at = @At("HEAD"), cancellable = true)
    //?}
    private void omni$allowAnyDirectionalSprint(CallbackInfoReturnable<Boolean> cir) {
        if (!OmniConfig.isEnabled()) {
            return;
        }

        //? if legacy_forge_pre17 {
        /*ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;*/
        //?} else {
        LocalPlayer self = (LocalPlayer) (Object) this;
        //?}

        //? if forge_reflective_access {
        /*Object input = InputAccess.playerInput(this);
        if (input == null) {
            return;
        }
        float forwardImpulse = InputAccess.forwardImpulse(input);
        float leftImpulse = InputAccess.leftImpulse(input);
        *///?} else {
        if (this.input == null) {
            return;
        }
        float forwardImpulse = this.input.forwardImpulse;
        float leftImpulse = this.input.leftImpulse;
        //?}

        //? if forge_reflective_access {
        /*boolean underwater = MovementAccess.isUnderWater(self);*/
        //?} else {
        boolean underwater = self.isUnderWater();
        //?}

        if (underwater) {
            cir.setReturnValue(
                    Math.abs(forwardImpulse) > 1.0E-5F
                            || Math.abs(leftImpulse) > 1.0E-5F
            );
            return;
        }

        cir.setReturnValue(
                Math.abs(forwardImpulse) >= 0.8F
                        || Math.abs(leftImpulse) >= 0.8F
        );
    }
    //?}

    //? if vector_input {
    //? if modern_forge {
    /*@Inject(method = {"isMoving", "m_108732_"}, at = @At("HEAD"), cancellable = true)*/
    //?} else {
    /*@Inject(method = "isMoving", at = @At("HEAD"), cancellable = true)*/
    //?}
    private void omni$allowAnyVectorMovement(CallbackInfoReturnable<Boolean> cir) {
        if (!OmniConfig.isEnabled()) {
            return;
        }

        Object input = InputAccess.playerInput(this);
        if (input == null) {
            return;
        }

        cir.setReturnValue(
                Math.abs(InputAccess.moveVectorY(input)) > 1.0E-5F
                        || Math.abs(InputAccess.moveVectorX(input)) > 1.0E-5F
        );
    }
    //?}
}
