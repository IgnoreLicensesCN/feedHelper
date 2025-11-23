package com.linearity.feedhelper.client.mixin;


import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.DarknessEffectFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.config.FeatureToggle.DISABLE_FOG;

@Mixin(DarknessEffectFogModifier.class)
public class DarknessEffectFogModifierMixin {

    @Inject(method = "applyStartEndModifier",at = @At("HEAD"),cancellable = true)
    public void disableApplyStartEndModifier(FogData data, Entity cameraEntity, BlockPos cameraPos, ClientWorld world, float viewDistance, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (DISABLE_FOG.getBooleanValue()){
            ci.cancel();
        }
    }

    @Inject(method = "applyDarknessModifier",at = @At("HEAD"),cancellable = true)
    public void disableApplyDarknessModifier(LivingEntity cameraEntity, float darkness, float tickProgress, CallbackInfoReturnable<Float> cir) {
        if (DISABLE_FOG.getBooleanValue()){
            cir.setReturnValue(1.f);
        }
    }
}
