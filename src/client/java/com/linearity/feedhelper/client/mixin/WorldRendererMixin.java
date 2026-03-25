package com.linearity.feedhelper.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.config.FeatureToggle.DISABLE_FOG;

import net.minecraft.client.renderer.LevelRenderer;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin{
    @Inject(
            method = "doesMobEffectBlockSky",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hasBlindnessOrDarkness(CallbackInfoReturnable<Boolean> cir){
        if (DISABLE_FOG.getBooleanValue()){
            cir.setReturnValue(false);
        }
    }
}

