package io.github.AvacadoWizard120.omni;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Omni.MODID)
public class Omni
{
    private static final double SPRINT_SPEED = 0.28;

    public static final String MODID = "omni";

    public Omni(IEventBus modEventBus, ModContainer modContainer)
    {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event)
    {
        Player player = event.getEntity();
        if (player.level().isClientSide)
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.keySprint.isDown()) {
                double forward = 0;
                double strafe = 0;
                if (mc.options.keyUp.isDown()) forward++;
                if (mc.options.keyDown.isDown()) forward--;
                if (mc.options.keyLeft.isDown()) strafe++;
                if (mc.options.keyRight.isDown()) strafe--;
                if (forward != 0 || strafe != 0) {
                    float inputMagnitude = (float) Math.sqrt(forward * forward + strafe * strafe);
                    forward /= inputMagnitude;
                    strafe /= inputMagnitude;
                    float yaw = (float) Math.toRadians(player.getYRot());
                    double motionX = strafe * Math.cos(yaw) - forward * Math.sin(yaw);
                    double motionZ = forward * Math.cos(yaw) + strafe * Math.sin(yaw);
                    Vec3 motion = new Vec3(motionX, 0, motionZ).normalize().scale(SPRINT_SPEED);
                    player.setDeltaMovement(motion.x, player.getDeltaMovement().y, motion.z);
                }
            }
        }
    }
}
