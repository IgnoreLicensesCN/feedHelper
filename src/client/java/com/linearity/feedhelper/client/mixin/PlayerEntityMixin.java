package com.linearity.feedhelper.client.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.config.FeatureToggle.AVOID_ELYTRA_RESISTANCE;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
//    @Inject(
//            method = "getVelocityMultiplier", // 每 tick 调用
//            at = @At(
//                    value = "HEAD"
//            ),
//            cancellable = true)
//    private void removeElytraDrag(CallbackInfoReturnable<Float> cir) {
//        if (AVOID_ELYTRA_RESISTANCE.getBooleanValue()){
//            cir.setReturnValue(1.0f);
//        }
//    }
}
