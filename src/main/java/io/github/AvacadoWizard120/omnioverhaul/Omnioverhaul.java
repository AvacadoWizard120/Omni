package io.github.AvacadoWizard120.omnioverhaul;

import io.github.AvacadoWizard120.omnioverhaul.config.ServerConfigHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class Omnioverhaul implements ModInitializer {

    public static final String MOD_ID = "omnioverhaul";

    @Override
    public void onInitialize() {
        // Load server config
        ServerConfigHandler.loadConfig();

        // Server-side initialization
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Send mod presence and config to client when they join
            OmnidirectionalSprintNetwork.sendModPresence(handler.player);
        });
    }

    public static void applyOmnidirectionalSprint(PlayerEntity player) {
        if (player.getWorld().isClient && !OmnidirectionalSprintNetwork.isServerModded()) {
            // Don't apply sprint on multiplayer servers without the mod
            return;
        }

        Vec3d movement = player.getVelocity();
        float yaw = player.getYaw();

        // Get movement input
        float forwardSpeed = 0;
        float sidewaysSpeed = 0;

        if (player.forwardSpeed > 0) forwardSpeed = 1;
        else if (player.forwardSpeed < 0) forwardSpeed = -1;

        if (player.sidewaysSpeed > 0) sidewaysSpeed = 1;
        else if (player.sidewaysSpeed < 0) sidewaysSpeed = -1;

        if (forwardSpeed == 0 && sidewaysSpeed == 0) {
            return;
        }

        float speed = (float) ServerConfigHandler.getSprintSpeed();
        float acceleration = (float) ServerConfigHandler.getSprintAcceleration();

        double motionX = -Math.sin(Math.toRadians(yaw)) * forwardSpeed + Math.cos(Math.toRadians(yaw)) * sidewaysSpeed;
        double motionZ = Math.cos(Math.toRadians(yaw)) * forwardSpeed + Math.sin(Math.toRadians(yaw)) * sidewaysSpeed;

        // Apply acceleration
        Vec3d newVelocity = new Vec3d(
                movement.x + (motionX * speed - movement.x) * acceleration,
                movement.y,
                movement.z + (motionZ * speed - movement.z) * acceleration
        );

        player.setVelocity(newVelocity);
    }
}