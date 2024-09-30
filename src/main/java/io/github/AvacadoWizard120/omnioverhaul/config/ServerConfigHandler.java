package io.github.AvacadoWizard120.omnioverhaul.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ServerConfigHandler
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("omnioverhaul-server.json").toFile();

    private static ServerConfig config;

    public static class ServerConfig {
        public double sprintSpeed = 1.3;
        public double sprintAcceleration = 0.1;
    }

    public static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, ServerConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            config = new ServerConfig();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double getSprintSpeed() {
        return config.sprintSpeed;
    }

    public static double getSprintAcceleration() {
        return config.sprintAcceleration;
    }
}