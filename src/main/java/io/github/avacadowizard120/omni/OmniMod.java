package io.github.avacadowizard120.omni;

//? if forge {
/*import net.minecraftforge.fml.common.Mod;*/
//?}
//? if neoforge {
/*import net.neoforged.fml.common.Mod;
*///?}

//? if forge {
/*@Mod(Omni.MOD_ID)*/
//?}
//? if neoforge {
/*@Mod(Omni.MOD_ID)
*///?}
public final class OmniMod {
    public OmniMod() {
        OmniConfig.ensureLoaded();
        ForgeCommandBridge.register();
    }
}
