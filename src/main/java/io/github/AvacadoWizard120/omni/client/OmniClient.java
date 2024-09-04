package io.github.AvacadoWizard120.omni.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class OmniClient implements ClientModInitializer {

    private static final double SPRINT_SPEED = 0.28;

    @Override
    public void onInitializeClient()
    {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.options.sprintKey.isPressed()) {
                handleOmnidirectionalSprint(client, client.player);
            }
        });
    }

    private void handleOmnidirectionalSprint(MinecraftClient client, PlayerEntity player) {
        double forward = 0;
        double strafe = 0;
        if (client.options.forwardKey.isPressed()) forward++;
        if (client.options.backKey.isPressed()) forward--;
        if (client.options.leftKey.isPressed()) strafe++;
        if (client.options.rightKey.isPressed()) strafe--;

        if (forward != 0 || strafe != 0) {
            float inputMagnitude = (float) Math.sqrt(forward * forward + strafe * strafe);
            forward /= inputMagnitude;
            strafe /= inputMagnitude;
            float yaw = (float) Math.toRadians(player.getYaw());
            double motionX = strafe * Math.cos(yaw) - forward * Math.sin(yaw);
            double motionZ = forward * Math.cos(yaw) + strafe * Math.sin(yaw);
            Vec3d motion = new Vec3d(motionX, 0, motionZ).normalize().multiply(SPRINT_SPEED);
            player.setVelocity(motion.x, player.getVelocity().y, motion.z);
        }
    }
}
