package io.github.avacadowizard120.omni.legacyfabric.mixin;

import io.github.avacadowizard120.omni.legacyfabric.LegacyOmniCommand;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.entity.player.ControllablePlayerEntity")
public abstract class LegacyClientCommandEarlyMixin {
    @Inject(method = "method_1262", at = @At("HEAD"), cancellable = true)
    private void omni$handleLocalCommand(String message, CallbackInfo ci) {
        String feedback = LegacyOmniCommand.handle(message);
        if (feedback == null) {
            return;
        }

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage("[Omni] " + feedback);
        ci.cancel();
    }
}
