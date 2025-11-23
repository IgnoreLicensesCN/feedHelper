package com.linearity.feedhelper.client.mixin;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

import static com.linearity.feedhelper.config.FeatureToggle.DISABLE_FOG;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(
            method = "applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void applyFog(ByteBuffer buffer,
                          int bufPos,
                          Vector4f fogColor,
                          float environmentalStart,
                          float environmentalEnd,
                          float renderDistanceStart,
                          float renderDistanceEnd,
                          float skyEnd,
                          float cloudEnd,
                          CallbackInfo ci) {
        if (DISABLE_FOG.getBooleanValue()){
            ci.cancel();
        }
    }

}

