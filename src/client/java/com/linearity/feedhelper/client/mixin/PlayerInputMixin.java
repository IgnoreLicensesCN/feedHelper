package com.linearity.feedhelper.client.mixin;

import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.client.FeedhelperClient.forceSneakingFlag;

@Mixin(PlayerInput.class)
public abstract class PlayerInputMixin {
    /**
     * Forces sneak() to always return true
     */
    @Inject(method = "sneak", at = @At("HEAD"), cancellable = true)
    private void forceSneak(CallbackInfoReturnable<Boolean> cir) {
        if (forceSneakingFlag.get()){
            cir.setReturnValue(true);
        }
    }


}
