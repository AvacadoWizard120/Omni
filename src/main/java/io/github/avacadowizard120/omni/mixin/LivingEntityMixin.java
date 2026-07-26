package io.github.avacadowizard120.omni.mixin;

import io.github.avacadowizard120.omni.InputAccess;
import io.github.avacadowizard120.omni.MovementAccess;
import io.github.avacadowizard120.omni.OmniConfig;
//? if legacy_forge_pre17 {
/*import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
*///?}
//? if legacy_forge_vec3d {
/*import net.minecraft.util.math.Vec3d;
*///?}
//? if legacy_forge_vector3d {
/*import net.minecraft.util.math.vector.Vector3d;
*///?}
//? if legacy_forge_pre17 {
//?} else {
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if legacy_forge_pre17 {
/*@Mixin(
        value = LivingEntity.class,
        remap = false
)*/
//?} else {
@Mixin(
        value = LivingEntity.class
        //? if no_mappings {
        /*, remap = false*/
        //?}
        //? if modern_forge {
        /*, remap = false*/
        //?}
)
//?}
public abstract class LivingEntityMixin {
    @Unique
    private double omni$preJumpX;

    @Unique
    private double omni$preJumpZ;

    @Unique
    private double omni$preTravelX;

    @Unique
    private double omni$preTravelZ;

    //? if legacy_forge_pre17 {
    /*@Inject(method = {"jumpFromGround", "func_70664_aZ"}, at = @At("HEAD"), require = 0)*/
    //?} else if modern_forge {
    /*@Inject(method = {"jumpFromGround", "m_6135_"}, at = @At("HEAD"))*/
    //?} else {
    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    //?}
    private void omni$capturePreJumpVelocity(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        //? if forge_reflective_access {
        /*this.omni$preJumpX = MovementAccess.deltaX(self);
        this.omni$preJumpZ = MovementAccess.deltaZ(self);
        *///?} else {
        //? if legacy_forge_pre17 {
        //? if legacy_forge_vec3d {
        /*Vec3d delta = self.getDeltaMovement();*/
        //?} else {
        /*Vector3d delta = self.getDeltaMovement();*/
        //?}
        //?} else {
        Vec3 delta = self.getDeltaMovement();
        //?}
        this.omni$preJumpX = delta.x;
        this.omni$preJumpZ = delta.z;
        //?}
    }

    //? if legacy_forge_pre17 {
    /*@Inject(method = {"jumpFromGround", "func_70664_aZ"}, at = @At("TAIL"), require = 0)*/
    //?} else if modern_forge {
    /*@Inject(method = {"jumpFromGround", "m_6135_"}, at = @At("TAIL"))*/
    //?} else {
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    //?}
    private void omni$redirectSprintJumpBoost(CallbackInfo ci) {
        if (!OmniConfig.isEnabled()) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;

        //? if legacy_forge_pre17 {
        /*if (!(self instanceof ClientPlayerEntity)) {
            return;
        }
        ClientPlayerEntity player = (ClientPlayerEntity) self;*/
        //?} else {
        if (!(self instanceof LocalPlayer)) {
            return;
        }
        LocalPlayer player = (LocalPlayer) self;
        //?}

        //? if forge_reflective_access {
        /*Object input = InputAccess.playerInput(player);
        if (input == null || !MovementAccess.isSprinting(self)) {
            return;
        }
        *///?} else {
        if (player.input == null || !self.isSprinting()) {
            return;
        }
        //?}

        //? if impulse_input {
        //? if forge_reflective_access {
        /*float side = InputAccess.leftImpulse(input);
        float forward = InputAccess.forwardImpulse(input);
        *///?} else {
        float side = player.input.leftImpulse;
        float forward = player.input.forwardImpulse;
        //?}
        //?}
        //? if vector_input {
        //? if modern_forge {
        /*float side = InputAccess.moveVectorX(input);
        float forward = InputAccess.moveVectorY(input);
        *///?} else {
        /*float side = player.input.getMoveVector().x;
        float forward = player.input.getMoveVector().y;*/
        //?}
        //?}

        double inputLenSqr = (double) (side * side + forward * forward);
        if (inputLenSqr < 1.0E-7) {
            return;
        }

        //? if forge_reflective_access {
        /*double afterX = MovementAccess.deltaX(self);
        double afterY = MovementAccess.deltaY(self);
        double afterZ = MovementAccess.deltaZ(self);
        *///?} else {
        //? if legacy_forge_pre17 {
        //? if legacy_forge_vec3d {
        /*Vec3d after = self.getDeltaMovement();*/
        //?} else {
        /*Vector3d after = self.getDeltaMovement();*/
        //?}
        //?} else {
        Vec3 after = self.getDeltaMovement();
        //?}
        double afterX = after.x;
        double afterY = after.y;
        double afterZ = after.z;
        //?}

        double vanillaAddedX = afterX - this.omni$preJumpX;
        double vanillaAddedZ = afterZ - this.omni$preJumpZ;
        double vanillaBoostLen = Math.sqrt(vanillaAddedX * vanillaAddedX + vanillaAddedZ * vanillaAddedZ);

        if (vanillaBoostLen < 1.0E-7) {
            return;
        }

        double desiredLocalX = side;
        double desiredLocalZ = forward;
        double desiredLocalLenSqr = desiredLocalX * desiredLocalX + desiredLocalZ * desiredLocalZ;
        if (desiredLocalLenSqr > 1.0) {
            double desiredLocalLen = Math.sqrt(desiredLocalLenSqr);
            desiredLocalX /= desiredLocalLen;
            desiredLocalZ /= desiredLocalLen;
        }

        //? if forge_reflective_access {
        /*float yawDegrees = MovementAccess.yRot(self);*/
        //?} else {
        //? if getter_y_rot {
        float yawDegrees = self.getYRot();
        //?}
        //? if field_y_rot {
        /*float yawDegrees = self.yRot;*/
        //?}
        //?}
        float yawRad = yawDegrees * ((float) Math.PI / 180.0F);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double desiredWorldX = desiredLocalX * cos - desiredLocalZ * sin;
        double desiredWorldZ = desiredLocalZ * cos + desiredLocalX * sin;
        double desiredWorldLenSqr = desiredWorldX * desiredWorldX + desiredWorldZ * desiredWorldZ;

        if (desiredWorldLenSqr < 1.0E-7) {
            return;
        }

        double desiredWorldLen = Math.sqrt(desiredWorldLenSqr);
        desiredWorldX = desiredWorldX / desiredWorldLen * vanillaBoostLen;
        desiredWorldZ = desiredWorldZ / desiredWorldLen * vanillaBoostLen;

        double correctedX = this.omni$preJumpX + desiredWorldX;
        double correctedZ = this.omni$preJumpZ + desiredWorldZ;

        //? if forge_reflective_access {
        /*MovementAccess.setDeltaMovement(self, correctedX, afterY, correctedZ);
        *///?} else {
        self.setDeltaMovement(correctedX, afterY, correctedZ);
        //?}
    }

    //? if impulse_input {
    //? if legacy_forge_pre17 {
    /*@Inject(method = {"travel", "func_213352_e"}, at = @At("HEAD"), require = 0)*/
    //?} else if modern_forge {
    /*@Inject(method = {"travel", "m_7023_"}, at = @At("HEAD"))*/
    //?} else {
    @Inject(method = "travel", at = @At("HEAD"))
    //?}
    //? if legacy_forge_pre17 {
    //? if legacy_forge_vec3d {
    /*private void omni$capturePreTravelVelocity(Vec3d travelVector, CallbackInfo ci) {*/
    //?} else {
    /*private void omni$capturePreTravelVelocity(Vector3d travelVector, CallbackInfo ci) {*/
    //?}
    //?} else {
    private void omni$capturePreTravelVelocity(Vec3 travelVector, CallbackInfo ci) {
    //?}
        LivingEntity self = (LivingEntity) (Object) this;
        //? if legacy_forge_pre17 {
        /*if (!(self instanceof ClientPlayerEntity)) {
            return;
        }*/
        //?} else {
        if (!(self instanceof LocalPlayer)) {
            return;
        }
        //?}

        //? if forge_reflective_access {
        /*this.omni$preTravelX = MovementAccess.deltaX(self);
        this.omni$preTravelZ = MovementAccess.deltaZ(self);
        *///?} else {
        //? if legacy_forge_pre17 {
        //? if legacy_forge_vec3d {
        /*Vec3d delta = self.getDeltaMovement();*/
        //?} else {
        /*Vector3d delta = self.getDeltaMovement();*/
        //?}
        //?} else {
        Vec3 delta = self.getDeltaMovement();
        //?}
        this.omni$preTravelX = delta.x;
        this.omni$preTravelZ = delta.z;
        //?}
    }

    //? if legacy_forge_pre17 {
    /*@Inject(method = {"travel", "func_213352_e"}, at = @At("TAIL"), require = 0)*/
    //?} else if modern_forge {
    /*@Inject(method = {"travel", "m_7023_"}, at = @At("TAIL"))*/
    //?} else {
    @Inject(method = "travel", at = @At("TAIL"))
    //?}
    //? if legacy_forge_pre17 {
    //? if legacy_forge_vec3d {
    /*private void omni$scaleDirectionalTravel(Vec3d travelVector, CallbackInfo ci) {*/
    //?} else {
    /*private void omni$scaleDirectionalTravel(Vector3d travelVector, CallbackInfo ci) {*/
    //?}
    //?} else {
    private void omni$scaleDirectionalTravel(Vec3 travelVector, CallbackInfo ci) {
    //?}
        if (!OmniConfig.isEnabled() || !OmniConfig.hasCustomSpeed()) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        //? if legacy_forge_pre17 {
        /*if (!(self instanceof ClientPlayerEntity)) {
            return;
        }
        ClientPlayerEntity player = (ClientPlayerEntity) self;*/
        //?} else {
        if (!(self instanceof LocalPlayer)) {
            return;
        }
        LocalPlayer player = (LocalPlayer) self;
        //?}

        //? if forge_reflective_access {
        /*if (!MovementAccess.isSprinting(self)) {
            return;
        }
        Object input = InputAccess.playerInput(player);
        if (input == null) {
            return;
        }
        *///?} else {
        if (!self.isSprinting() || player.input == null) {
            return;
        }
        //?}

        //? if impulse_input {
        //? if forge_reflective_access {
        /*float side = InputAccess.leftImpulse(input);
        float forward = InputAccess.forwardImpulse(input);
        *///?} else {
        float side = player.input.leftImpulse;
        float forward = player.input.forwardImpulse;
        //?}
        //?}
        //? if vector_input {
        //? if modern_forge {
        /*float side = InputAccess.moveVectorX(input);
        float forward = InputAccess.moveVectorY(input);
        *///?} else {
        /*float side = player.input.getMoveVector().x;
        float forward = player.input.getMoveVector().y;*/
        //?}
        //?}

        if ((double) (side * side + forward * forward) < 1.0E-7D) {
            return;
        }

        float multiplier = OmniConfig.directionMultiplier(forward, side);
        if (Math.abs(multiplier - 1.0F) < 1.0E-5F) {
            return;
        }

        //? if forge_reflective_access {
        /*double afterX = MovementAccess.deltaX(self);
        double afterY = MovementAccess.deltaY(self);
        double afterZ = MovementAccess.deltaZ(self);
        *///?} else {
        //? if legacy_forge_pre17 {
        //? if legacy_forge_vec3d {
        /*Vec3d after = self.getDeltaMovement();*/
        //?} else {
        /*Vector3d after = self.getDeltaMovement();*/
        //?}
        //?} else {
        Vec3 after = self.getDeltaMovement();
        //?}
        double afterX = after.x;
        double afterY = after.y;
        double afterZ = after.z;
        //?}

        double addedX = afterX - this.omni$preTravelX;
        double addedZ = afterZ - this.omni$preTravelZ;
        if (addedX * addedX + addedZ * addedZ < 1.0E-10D) {
            return;
        }

        double desiredLocalX = side;
        double desiredLocalZ = forward;
        double desiredLocalLenSqr = desiredLocalX * desiredLocalX + desiredLocalZ * desiredLocalZ;
        if (desiredLocalLenSqr > 1.0D) {
            double desiredLocalLen = Math.sqrt(desiredLocalLenSqr);
            desiredLocalX /= desiredLocalLen;
            desiredLocalZ /= desiredLocalLen;
        }

        //? if forge_reflective_access {
        /*float yawDegrees = MovementAccess.yRot(self);*/
        //?} else {
        //? if getter_y_rot {
        float yawDegrees = self.getYRot();
        //?}
        //? if field_y_rot {
        /*float yawDegrees = self.yRot;*/
        //?}
        //?}
        float yawRad = yawDegrees * ((float) Math.PI / 180.0F);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double desiredWorldX = desiredLocalX * cos - desiredLocalZ * sin;
        double desiredWorldZ = desiredLocalZ * cos + desiredLocalX * sin;
        double desiredWorldLenSqr = desiredWorldX * desiredWorldX + desiredWorldZ * desiredWorldZ;
        if (desiredWorldLenSqr < 1.0E-7D) {
            return;
        }

        double desiredWorldLen = Math.sqrt(desiredWorldLenSqr);
        desiredWorldX /= desiredWorldLen;
        desiredWorldZ /= desiredWorldLen;

        double inputAlignedAdded = addedX * desiredWorldX + addedZ * desiredWorldZ;
        if (inputAlignedAdded <= 1.0E-10D) {
            return;
        }

        double extraScale = (double) multiplier - 1.0D;
        double correctedX = afterX + desiredWorldX * inputAlignedAdded * extraScale;
        double correctedZ = afterZ + desiredWorldZ * inputAlignedAdded * extraScale;

        //? if forge_reflective_access {
        /*MovementAccess.setDeltaMovement(self, correctedX, afterY, correctedZ);
        *///?} else {
        self.setDeltaMovement(correctedX, afterY, correctedZ);
        //?}
    }
    //?}

    //? if vector_input {
    //? if modern_forge {
    /*@ModifyArg(
            method = {"handleRelativeFrictionAndCalculateMovement", "m_21074_"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"
            ),
            index = 0
    )*/
    //?} else {
    /*@ModifyArg(
            method = "handleRelativeFrictionAndCalculateMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"
            ),
            index = 0
    )*/
    //?}
    private float omni$scaleDirectionalAcceleration(float speed) {
        if (!OmniConfig.isEnabled() || !OmniConfig.hasCustomSpeed()) {
            return speed;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof LocalPlayer)) {
            return speed;
        }
        LocalPlayer player = (LocalPlayer) self;

        //? if modern_forge {
        /*if (!MovementAccess.isSprinting(self)) {
            return speed;
        }
        Object input = InputAccess.playerInput(player);
        if (input == null) {
            return speed;
        }
        float side = InputAccess.moveVectorX(input);
        float forward = InputAccess.moveVectorY(input);
        *///?} else {
        /*if (!self.isSprinting() || player.input == null) {
            return speed;
        }
        float side = player.input.getMoveVector().x;
        float forward = player.input.getMoveVector().y;*/
        //?}

        if ((double) (side * side + forward * forward) < 1.0E-7D) {
            return speed;
        }

        float multiplier = OmniConfig.directionMultiplier(forward, side);
        if (Math.abs(multiplier - 1.0F) < 1.0E-5F) {
            return speed;
        }
        return speed * multiplier;
    }
    //?}
}
