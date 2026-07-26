package io.github.avacadowizard120.omni.liteloader;

import com.mumfrey.liteloader.LiteMod;
import java.io.File;

public final class LiteModOmni implements LiteMod {
    @Override
    public String getName() {
        return "Omni";
    }

    @Override
    public String getVersion() {
        return "1.2-OVERHAUL";
    }

    @Override
    public void init(File configPath) {
    }

    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath) {
    }
}
