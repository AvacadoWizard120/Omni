package io.github.avacadowizard120.omni.legacyfabric;

import io.github.avacadowizard120.omni.OmniConfig;
import net.minecraft.client.input.Input;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;

public final class LegacyLivingMovement {
    private static final float EPSILON = 1.0E-5F;

    private LegacyLivingMovement() {
    }

    public static float scaleAcceleration(LivingEntity entity, float speed) {
        if (!(entity instanceof ClientPlayerEntity)
                || !OmniConfig.isEnabled()
                || !OmniConfig.hasCustomSpeed()
                || !entity.isSprinting()) {
            return speed;
        }

        Input input = ((ClientPlayerEntity) entity).input;
        if (input == null || input.movementForward * input.movementForward + input.movementSideways * input.movementSideways < EPSILON * EPSILON) {
            return speed;
        }
        return speed * OmniConfig.directionMultiplier(input.movementForward, input.movementSideways);
    }
}
