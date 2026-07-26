package io.github.avacadowizard120.omni.legacyfabric.mixin;

import io.github.avacadowizard120.omni.legacyfabric.LegacyOmniCommand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class LegacyClientCommandMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void omni$handleLocalCommand(String message, CallbackInfo ci) {
        String feedback = LegacyOmniCommand.handle(message);
        if (feedback == null) {
            return;
        }

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText("[Omni] " + feedback));
        ci.cancel();
    }
}
