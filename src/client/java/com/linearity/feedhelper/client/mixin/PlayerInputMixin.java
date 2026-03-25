package com.linearity.feedhelper.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.client.FeedhelperClient.forceSneakingFlag;

import net.minecraft.world.entity.player.Input;

@Mixin(Input.class)
public abstract class PlayerInputMixin {
    /**
     * Forces sneak() to always return true
     */
    @Inject(method = "shift", at = @At("HEAD"), cancellable = true)
    private void forceSneak(CallbackInfoReturnable<Boolean> cir) {
        if (forceSneakingFlag.get()){
            cir.setReturnValue(true);
        }
    }


}
