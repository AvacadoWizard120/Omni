package io.github.avacadowizard120.omni.legacyfabric.mixin;

import io.github.avacadowizard120.omni.legacyfabric.LegacyOmniCommand;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.entity.player.ControllablePlayerEntity")
public abstract class LegacyClientCommandMuseumMixin {
    @Inject(method = "method_1262", at = @At("HEAD"), cancellable = true)
    private void omni$handleLocalCommand(String message, CallbackInfo ci) {
        String feedback = LegacyOmniCommand.handle(message);
        if (feedback == null) {
            return;
        }

        Minecraft.getMinecraft().inGameHud.getChatHud().addMessage("[Omni] " + feedback);
        ci.cancel();
    }
}
