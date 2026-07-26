package io.github.avacadowizard120.omni.legacyfabric.mixin;

import io.github.avacadowizard120.omni.legacyfabric.LegacyLivingMovement;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LegacyLivingEntityClassicMixin {
    @ModifyArg(
            method = "travel(FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;updateVelocity(FFF)V"
            ),
            index = 2,
            require = 3
    )
    private float omni$scaleDirectionalAcceleration(float speed) {
        return LegacyLivingMovement.scaleAcceleration((LivingEntity) (Object) this, speed);
    }
}
