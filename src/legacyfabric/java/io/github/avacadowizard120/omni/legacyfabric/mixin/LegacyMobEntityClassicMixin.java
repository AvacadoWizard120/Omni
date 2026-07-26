package io.github.avacadowizard120.omni.legacyfabric.mixin;

import io.github.avacadowizard120.omni.legacyfabric.LegacyMobMovement;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MobEntity.class)
public abstract class LegacyMobEntityClassicMixin {
    @ModifyArg(
            method = "method_2657(FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/mob/MobEntity;updateVelocity(FFF)V"
            ),
            index = 2,
            require = 3
    )
    private float omni$scaleDirectionalAcceleration(float speed) {
        return LegacyMobMovement.scaleAcceleration((MobEntity) (Object) this, speed);
    }
}
