package io.github.avacadowizard120.omni.legacyfabric.mixin;

import io.github.avacadowizard120.omni.OmniConfig;
import net.minecraft.client.input.Input;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LegacyLivingEntityJumpMixin {
    @Unique
    private double omni$preJumpX;

    @Unique
    private double omni$preJumpZ;

    @Inject(method = "jump", at = @At("HEAD"))
    private void omni$capturePreJumpVelocity(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        this.omni$preJumpX = self.velocityX;
        this.omni$preJumpZ = self.velocityZ;
    }

    @Inject(method = "jump", at = @At("TAIL"))
    private void omni$redirectSprintJumpBoost(CallbackInfo ci) {
        if (!OmniConfig.isEnabled()) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ClientPlayerEntity) || !self.isSprinting()) {
            return;
        }

        Input input = ((ClientPlayerEntity) self).input;
        if (input == null) {
            return;
        }

        double side = input.movementSideways;
        double forward = input.movementForward;
        double inputLengthSquared = side * side + forward * forward;
        if (inputLengthSquared < 1.0E-7D) {
            return;
        }
        if (inputLengthSquared > 1.0D) {
            double inputLength = Math.sqrt(inputLengthSquared);
            side /= inputLength;
            forward /= inputLength;
        }

        double vanillaAddedX = self.velocityX - this.omni$preJumpX;
        double vanillaAddedZ = self.velocityZ - this.omni$preJumpZ;
        double vanillaBoost = Math.sqrt(vanillaAddedX * vanillaAddedX + vanillaAddedZ * vanillaAddedZ);
        if (vanillaBoost < 1.0E-7D) {
            return;
        }

        float yawRadians = self.yaw * ((float) Math.PI / 180.0F);
        double sin = Math.sin(yawRadians);
        double cos = Math.cos(yawRadians);
        double worldX = side * cos - forward * sin;
        double worldZ = forward * cos + side * sin;
        double worldLength = Math.sqrt(worldX * worldX + worldZ * worldZ);
        if (worldLength < 1.0E-7D) {
            return;
        }

        self.velocityX = this.omni$preJumpX + worldX / worldLength * vanillaBoost;
        self.velocityZ = this.omni$preJumpZ + worldZ / worldLength * vanillaBoost;
    }
}
