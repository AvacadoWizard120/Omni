package io.github.avacadowizard120.omni;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Omni.MODID)
public class Omni {
    private static final double SPRINT_SPEED = 0.28;

    // Define mod id in a common place for everything to reference
    public static final String MODID = "omni";

    public Omni() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player.level().isClientSide) {
            Player player = event.player;
            Minecraft mc = Minecraft.getInstance();

            if (mc.options.keySprint.isDown()) {
                double forward = 0;
                double strafe = 0;

                if (mc.options.keyUp.isDown()) forward++;
                if (mc.options.keyDown.isDown()) forward--;
                if (mc.options.keyLeft.isDown()) strafe++;
                if (mc.options.keyRight.isDown()) strafe--;

                if (forward != 0 || strafe != 0) {
                    // Normalize diagonal movement
                    float inputMagnitude = (float) Math.sqrt(forward * forward + strafe * strafe);
                    forward /= inputMagnitude;
                    strafe /= inputMagnitude;

                    float yaw = (float) Math.toRadians(player.getYRot());

                    double motionX = strafe * Math.cos(yaw) - forward * Math.sin(yaw);
                    double motionZ = forward * Math.cos(yaw) + strafe * Math.sin(yaw);

                    Vec3 motion = new Vec3(motionX, 0, motionZ).normalize().scale(SPRINT_SPEED);

                    // Apply the movement
                    player.setDeltaMovement(motion.x, player.getDeltaMovement().y, motion.z);
                }
            }
        }
    }
}
