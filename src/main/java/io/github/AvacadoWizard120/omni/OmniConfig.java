package io.github.AvacadoWizard120.omni;

import net.neoforged.neoforge.common.ModConfigSpec;

public class OmniConfig {
    public final ModConfigSpec.DoubleValue forwardMultiplier;
    public final ModConfigSpec.DoubleValue backwardMultiplier;
    public final ModConfigSpec.DoubleValue leftMultiplier;
    public final ModConfigSpec.DoubleValue rightMultiplier;

    public OmniConfig(ModConfigSpec.Builder builder) {
        builder.push("movement");

        forwardMultiplier = builder
                .comment("Forward sprint speed multiplier")
                .translation("omni.config.forward_multiplier")
                .defineInRange("forward_multiplier", 0.28, 0, 99999.0);

        backwardMultiplier = builder
                .comment("Backward sprint speed multiplier")
                .translation("omni.config.backward_multiplier")
                .defineInRange("backward_multiplier", 0.28, 0, 99999.0);

        leftMultiplier = builder
                .comment("Left sprint speed multiplier")
                .translation("omni.config.left_multiplier")
                .defineInRange("left_multiplier", 0.28, 0, 99999.0);

        rightMultiplier = builder
                .comment("Right sprint speed multiplier")
                .translation("omni.config.right_multiplier")
                .defineInRange("right_multiplier", 0.28, 0, 99999.0);

        builder.pop();
    }
}