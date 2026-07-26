package io.github.avacadowizard120.omni.forge112;

import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("Omni")
@IFMLLoadingPlugin.SortingIndex(1001)
public final class OmniForgeLoadingPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                "io.github.avacadowizard120.omni.liteloader.OmniLiteTransformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
