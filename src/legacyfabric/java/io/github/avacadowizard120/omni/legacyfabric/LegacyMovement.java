package io.github.avacadowizard120.omni.legacyfabric;

import io.github.avacadowizard120.omni.OmniConfig;
import net.minecraft.client.input.Input;

public final class LegacyMovement {
    private LegacyMovement() {
    }

    public static float directionalForward(Input input) {
        if (input == null || !OmniConfig.isEnabled()) {
            return input == null ? 0.0F : input.movementForward;
        }
        return Math.max(Math.abs(input.movementForward), Math.abs(input.movementSideways));
    }
}
