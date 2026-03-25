package com.linearity.feedhelper.client.mixin;

import com.linearity.feedhelper.client.utils.EntityGlowUtils;
import com.linearity.feedhelper.config.FeatureToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.config.FeatureToggle.AVOID_ELYTRA_RESISTANCE;


@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void alwaysGlow(CallbackInfoReturnable<Integer> cir) {
        if (FeatureToggle.ENTITY_GLOWING.getBooleanValue()){

            Entity entity = (Entity) (Object) this;

            Integer result = EntityGlowUtils.getGlowColor(entity);
            if (result != null) {
                cir.setReturnValue(result);
            }

        }
    }


    @Inject(
            method = "isInWater",
            at = @At("HEAD"),
            cancellable = true
    )
    public void noTouchingWaterWhenGliding(CallbackInfoReturnable<Boolean> cir){
        if (AVOID_ELYTRA_RESISTANCE.getBooleanValue()){
            Entity entity = (Entity) (Object) this;
            var player = Minecraft.getInstance().player;
            if (entity == player) {
                if (player.isFallFlying()){
                    cir.setReturnValue(false);
                }
            }
        }
    }
    @Inject(
            method = "isInLava",
            at = @At("HEAD"),
            cancellable = true
    )
    public void noTouchingLavaWhenGliding(CallbackInfoReturnable<Boolean> cir){
        if (AVOID_ELYTRA_RESISTANCE.getBooleanValue()){
            Entity entity = (Entity) (Object) this;
            var player = Minecraft.getInstance().player;
            if (entity == player) {
                if (player.isFallFlying()){
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
