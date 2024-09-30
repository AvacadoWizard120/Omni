package io.github.AvacadoWizard120.omnioverhaul.client;

import io.github.AvacadoWizard120.omnioverhaul.OmnidirectionalSprintNetwork;
import io.github.AvacadoWizard120.omnioverhaul.Omnioverhaul;
import io.github.AvacadoWizard120.omnioverhaul.client.config.ClientConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class OmnioverhaulClient implements ClientModInitializer {

    private static KeyBinding sprintKey;

    @Override
    public void onInitializeClient() {
        // Register Cloth Config
        AutoConfig.register(ClientConfig.class, GsonConfigSerializer::new);

        // Register the sprint key binding
        sprintKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.omnioverhaul.sprint",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.omnioverhaul.sprint"
        ));

        // Register the client tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && sprintKey.isPressed() && (client.player.forwardSpeed != 0 || client.player.sidewaysSpeed != 0)) {
                // Apply omnidirectional sprint
                Omnioverhaul.applyOmnidirectionalSprint(client.player);
            }
        });

        // Register client-side network packets
        OmnidirectionalSprintNetwork.registerClientPackets();
    }
}