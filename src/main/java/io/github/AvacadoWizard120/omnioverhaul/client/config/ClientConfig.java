package io.github.AvacadoWizard120.omnioverhaul.client.config;

import io.github.AvacadoWizard120.omnioverhaul.Omnioverhaul;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Omnioverhaul.MOD_ID)
public class ClientConfig implements ConfigData
{
    @ConfigEntry.Gui.Tooltip
    public double sprintSpeed = 1.3;

    @ConfigEntry.Gui.Tooltip
    public double sprintAcceleration = 0.1;

    public static ClientConfig get() {
        return AutoConfig.getConfigHolder(ClientConfig.class).getConfig();
    }
}