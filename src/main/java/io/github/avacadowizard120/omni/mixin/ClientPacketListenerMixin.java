package io.github.avacadowizard120.omni.mixin;

import io.github.avacadowizard120.omni.OmniCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "net.minecraft.client.multiplayer.ClientPacketListener"
        //? if no_mappings {
        /*, remap = false*/
        //?}
        //? if modern_forge {
        /*, remap = false*/
        //?}
)
public abstract class ClientPacketListenerMixin {
    //? if packet_command {
    //? if forge_packet_command_aliases {
    /*@Inject(method = {"sendCommand", "m_246958_"}, at = @At("HEAD"), cancellable = true, require = 0)*/
    //?} else {
    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void omni$handleCommand(String command, CallbackInfo ci) {
        if (OmniCommand.handle(command)) {
            ci.cancel();
        }
    }

    //? if unsigned_packet_command {
    //? if forge_packet_command_aliases {
    /*@Inject(method = {"sendUnsignedCommand", "m_246773_"}, at = @At("HEAD"), cancellable = true, require = 0)*/
    //?} else {
    @Inject(method = "sendUnsignedCommand", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void omni$handleUnsignedCommand(String command, CallbackInfoReturnable<Boolean> cir) {
        if (OmniCommand.handle(command)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
    //?}
    //?}
}
