package io.github.avacadowizard120.omni.mixin;

import io.github.avacadowizard120.omni.InputAccess;
import io.github.avacadowizard120.omni.OmniConfig;
//? if legacy_forge_pre17 {
/*import net.minecraft.util.MovementInput;*/
//?} else if legacy_input {
import net.minecraft.client.player.Input;
//?}
//? if client_input {
/*import net.minecraft.client.player.ClientInput;*/
//?}
//? if vector_input {
/*import net.minecraft.world.phys.Vec2;*/
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if legacy_forge_pre17 {
/*@Mixin(
        value = MovementInput.class,
        remap = false
)*/
//?} else if legacy_input {
@Mixin(
        value = Input.class
        //? if no_mappings {
        /*, remap = false*/
        //?}
        //? if modern_forge {
        /*, remap = false*/
        //?}
)
//?}
//? if client_input {
/*@Mixin(
        value = ClientInput.class
        //? if no_mappings {
        , remap = false
        //?}
        //? if modern_forge {
        , remap = false
        //?}
)*/
//?}
public abstract class InputMixin {
    //? if impulse_input {
    //? if forge_reflective_access {
    /*// Forge field access is handled through InputAccess. Mixin 0.8.4 rejects aliases for these public target fields.*/
    //?} else {
    @Shadow
    public float leftImpulse;
    @Shadow public float forwardImpulse;
    //?}
    //?}
    //? if vector_input {
    //? if modern_forge {
    /*// Forge field access is handled through InputAccess. Mixin 0.8.4 rejects aliases for this target field.*/
    //?} else {
    /*@Shadow protected Vec2 moveVector;*/
    //?}
    //?}

    //? if legacy_forge_pre17 {
    /*@Inject(method = {"hasForwardImpulse", "func_223135_b", "method_20622", "b"}, at = @At("HEAD"), cancellable = true, require = 0)*/
    //?} else if modern_forge_impulse {
    /*@Inject(method = {"hasForwardImpulse", "m_108577_"}, at = @At("HEAD"), cancellable = true)*/
    //?} else if modern_forge_vector {
    /*@Inject(method = {"hasForwardImpulse", "m_354496_"}, at = @At("HEAD"), cancellable = true)*/
    //?} else {
    @Inject(method = "hasForwardImpulse", at = @At("HEAD"), cancellable = true)
    //?}
    private void omni$allowAnyDirectionalImpulse(CallbackInfoReturnable<Boolean> cir) {
        if (!OmniConfig.isEnabled()) {
            return;
        }

        //? if impulse_input {
        //? if forge_reflective_access {
        /*cir.setReturnValue(
                Math.abs(InputAccess.forwardImpulse(this)) > 1.0E-5F
                        || Math.abs(InputAccess.leftImpulse(this)) > 1.0E-5F
        );
        *///?} else {
        cir.setReturnValue(
                Math.abs(this.forwardImpulse) > 1.0E-5F
                        || Math.abs(this.leftImpulse) > 1.0E-5F
        );
        //?}
        //?}
        //? if vector_input {
        //? if modern_forge {
        /*cir.setReturnValue(
                Math.abs(InputAccess.moveVectorY(this)) > 1.0E-5F
                        || Math.abs(InputAccess.moveVectorX(this)) > 1.0E-5F
        );
        *///?} else {
        /*cir.setReturnValue(
                this.moveVector != null
                        && (Math.abs(this.moveVector.y) > 1.0E-5F
                        || Math.abs(this.moveVector.x) > 1.0E-5F)
        );*/
        //?}
        //?}
    }
}
