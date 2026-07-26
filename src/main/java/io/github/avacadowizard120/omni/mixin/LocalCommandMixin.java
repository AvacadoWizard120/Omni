package io.github.avacadowizard120.omni.mixin;

import io.github.avacadowizard120.omni.OmniCommand;
//? if legacy_forge_pre17 {
/*import net.minecraft.client.entity.player.ClientPlayerEntity;*/
//?} else {
import net.minecraft.client.player.LocalPlayer;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if legacy_forge_pre17 {
/*@Mixin(
        value = ClientPlayerEntity.class,
        remap = false
)*/
//?} else {
@Mixin(
        value = LocalPlayer.class
        //? if no_mappings {
        /*, remap = false*/
        //?}
        //? if modern_forge {
        /*, remap = false*/
        //?}
)
//?}
public abstract class LocalCommandMixin {
    //? if legacy_chat_command {
    //? if modern_forge {
    /*@Inject(method = {"chat", "m_108747_"}, at = @At("HEAD"), cancellable = true, require = 0)*/
    //?} else {
    @Inject(method = "chat", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void omni$handleChatCommand(String message, CallbackInfo ci) {
        if (OmniCommand.handle(message)) {
            ci.cancel();
        }
    }
    //?}

    //? if command_method {
    //? if modern_forge {
    /*@Inject(method = {"command", "m_233646_"}, at = @At("HEAD"), cancellable = true, require = 0)*/
    //?} else {
    @Inject(method = "command", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void omni$handleCommand(String command, CallbackInfo ci) {
        if (OmniCommand.handle(command)) {
            ci.cancel();
        }
    }
    //?}

    //? if signed_command_method {
    //? if modern_forge {
    /*@Inject(method = {"commandUnsigned", "m_241770_"}, at = @At("HEAD"), cancellable = true, require = 0)*/
    //?} else {
    @Inject(method = "commandUnsigned", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void omni$handleUnsignedCommand(String command, CallbackInfoReturnable<Boolean> cir) {
        if (OmniCommand.handle(command)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

    //? if modern_forge {
    /*@Inject(method = {"commandSigned", "m_241720_"}, at = @At("HEAD"), cancellable = true, require = 0)*/
    //?} else {
    @Inject(method = "commandSigned", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void omni$handleSignedCommand(String command, Object preview, CallbackInfo ci) {
        if (OmniCommand.handle(command)) {
            ci.cancel();
        }
    }
    //?}
}
