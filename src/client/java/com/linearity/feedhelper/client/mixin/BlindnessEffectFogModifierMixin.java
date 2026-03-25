package com.linearity.feedhelper.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.linearity.feedhelper.config.FeatureToggle.DISABLE_FOG;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;

@Mixin(BlindnessFogEnvironment.class)
public class BlindnessEffectFogModifierMixin {
    @Inject(method = "setupFog",at = @At("HEAD"),cancellable = true)
    public void disableMethod(FogData data, Camera camera, ClientLevel clientWorld, float f, DeltaTracker renderTickCounter, CallbackInfo ci) {
        if (DISABLE_FOG.getBooleanValue()){
            ci.cancel();
        }
    }
}
