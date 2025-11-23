package com.linearity.feedhelper.client.mixin;

import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.BlindnessEffectFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.linearity.feedhelper.config.FeatureToggle.DISABLE_FOG;

@Mixin(BlindnessEffectFogModifier.class)
public class BlindnessEffectFogModifierMixin {
    @Inject(method = "applyStartEndModifier",at = @At("HEAD"),cancellable = true)
    public void disableMethod(FogData data, Entity cameraEntity, BlockPos cameraPos, ClientWorld world, float viewDistance, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (DISABLE_FOG.getBooleanValue()){
            ci.cancel();
        }
    }
}
