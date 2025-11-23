package com.linearity.feedhelper.client.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.config.Configs.Generic.*;
import static com.linearity.feedhelper.config.FeatureToggle.AVOID_ELYTRA_RESISTANCE;
import static com.linearity.feedhelper.config.FeatureToggle.DISABLE_FOG;
import static com.linearity.feedhelper.config.Hotkeys.ELYTRA_SPEED_DOWN;
import static com.linearity.feedhelper.config.Hotkeys.ELYTRA_SPEED_UP;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * 修改鞘翅飞行速度计算
     * oldVelocity 是原来的速度
     */
    @Inject(
            method = "calcGlidingVelocity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void removeElytraDrag(Vec3d oldVelocity, CallbackInfoReturnable<Vec3d> cir) {
        if (AVOID_ELYTRA_RESISTANCE.getBooleanValue()){
            LivingEntity entity = (LivingEntity) (Object) this;

            Vec3d vec3d = entity.getRotationVector();
            double multiplier = 1;
            double multiplyUp = ELYTRA_SPEED_UP_MULTIPLIER.getDoubleValue();
            double multiplyDown = ELYTRA_SPEED_DOWN_MULTIPLIER.getDoubleValue();
            if (ELYTRA_SPEED_UP.getKeybind().isKeybindHeld()){
                multiplier *= multiplyUp;
            }
            if (ELYTRA_SPEED_DOWN.getKeybind().isKeybindHeld()){
                multiplier *= multiplyDown;
            }
            double speedValue = Math.max(
                    Math.min(
                            oldVelocity.length()*multiplier,
                            ELYTRA_MAX_SPEED.getDoubleValue())
                    ,ELYTRA_MIN_SPEED.getDoubleValue()
            );
            cir.setReturnValue(vec3d.normalize().multiply(speedValue));
        }
    }

    @Inject(
            method = "takeKnockback",
            at = @At("HEAD"),
            cancellable = true
    )
    public void takeKnockback(double strength, double x, double z, CallbackInfo ci){
        if (AVOID_ELYTRA_RESISTANCE.getBooleanValue()){
            ci.cancel();
        }
    }

    @Inject(
            method = "getEffectFadeFactor",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getEffectFadeFactor(CallbackInfoReturnable<Float> cir){
        if (DISABLE_FOG.getBooleanValue()){
            cir.setReturnValue(0.f);
        }
    }

}
