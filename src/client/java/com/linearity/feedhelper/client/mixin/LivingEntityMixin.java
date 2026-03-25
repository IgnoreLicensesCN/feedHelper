package com.linearity.feedhelper.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.config.Configs.Generic.*;
import static com.linearity.feedhelper.config.FeatureToggle.*;
import static com.linearity.feedhelper.config.Hotkeys.ELYTRA_SPEED_DOWN;
import static com.linearity.feedhelper.config.Hotkeys.ELYTRA_SPEED_UP;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(
            method = "updateFallFlyingMovement",
            at = @At("HEAD"),
            cancellable = true
    )
    private void removeElytraDrag(Vec3 oldVelocity, CallbackInfoReturnable<Vec3> cir) {
        var client = Minecraft.getInstance();
        if (ELYTRA_CREATIVE_FLYING.getBooleanValue()){
            var settings = client.options;
            var player = client.player;
            if (player == null) {return;}
            double velocityLength = ELYTRA_CREATIVE_FLYING_DIRECTION_SPEED.getDoubleValue();
            var movementVec = player.input.getMoveVector();
            movementVec.scale((float) (velocityLength/movementVec.length()));
            double upSpeed = 0;
            if (settings.keyJump.isDown()){
                upSpeed += velocityLength;
            }
            if (settings.keyShift.isDown()){
                upSpeed -= velocityLength;
            }
            if (settings.keySprint.isDown()){
                movementVec = movementVec.scale((float) ELYTRA_CREATIVE_SPRINT_SPEED_MULTIPLIER.getDoubleValue());
            }
            double yawRad = Math.toRadians(player.getVisualRotationYInDegrees());
            double yawCos = Math.cos(yawRad);
            double yawSin = Math.sin(yawRad);
            movementVec = new Vec2(
                    (float) (movementVec.x * yawCos - movementVec.y * yawSin),
                    (float) (movementVec.x * yawSin + movementVec.y * yawCos)
            );//邱维声教的解析几何（指某本北大教材）

            cir.setReturnValue(new Vec3(movementVec.x,upSpeed,movementVec.y));
        }
        else if (AVOID_ELYTRA_RESISTANCE.getBooleanValue()){
            LivingEntity entity = (LivingEntity) (Object) this;

            Vec3 vec3d = entity.getLookAngle();
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
            cir.setReturnValue(vec3d.normalize().scale(speedValue));
        }
    }

    @Inject(
            method = "knockback",
            at = @At("HEAD"),
            cancellable = true
    )
    public void takeKnockback(double strength, double x, double z, CallbackInfo ci){
        if (AVOID_ELYTRA_RESISTANCE.getBooleanValue()){
            ci.cancel();
        }
    }

    @Inject(
            method = "getEffectBlendFactor",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getEffectFadeFactor(CallbackInfoReturnable<Float> cir){
        if (DISABLE_FOG.getBooleanValue()){
            cir.setReturnValue(0.f);
        }
    }

}
