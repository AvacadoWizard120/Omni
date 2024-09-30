package io.github.AvacadoWizard120.omnioverhaul;

import io.github.AvacadoWizard120.omnioverhaul.client.config.ClientConfig;
import io.github.AvacadoWizard120.omnioverhaul.config.ServerConfigHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class OmnidirectionalSprintNetwork
{
    private static final Identifier MOD_PRESENCE_PACKET = new Identifier(Omnioverhaul.MOD_ID, "mod_presence");
    private static boolean serverModded = false;

    public static void registerClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(MOD_PRESENCE_PACKET, (client, handler, buf, responseSender) -> {
            serverModded = true;
            double serverSprintSpeed = buf.readDouble();
            double serverSprintAcceleration = buf.readDouble();

            // Update client config with server values
            ClientConfig config = ClientConfig.get();
            config.sprintSpeed = serverSprintSpeed;
            config.sprintAcceleration = serverSprintAcceleration;
        });
    }

    public static void sendModPresence(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(ServerConfigHandler.getSprintSpeed());
        buf.writeDouble(ServerConfigHandler.getSprintAcceleration());
        ServerPlayNetworking.send(player, MOD_PRESENCE_PACKET, buf);
    }

    public static boolean isServerModded() {
        return serverModded;
    }
}