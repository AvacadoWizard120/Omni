package io.github.avacadowizard120.omni.legacyfabric;

import io.github.avacadowizard120.omni.OmniConfig;
import net.fabricmc.api.ClientModInitializer;

public final class LegacyOmniInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OmniConfig.ensureLoaded();
    }
}
