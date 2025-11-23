package com.linearity.feedhelper.client.mixin;

import com.linearity.feedhelper.client.utils.EntityGlowUtils;
import com.linearity.feedhelper.config.FeatureToggle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.config.FeatureToggle.AVOID_ELYTRA_RESISTANCE;


@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
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
            method = "isTouchingWater",
            at = @At("HEAD"),
            cancellable = true
    )
    public void noTouchingWaterWhenGliding(CallbackInfoReturnable<Boolean> cir){
        if (AVOID_ELYTRA_RESISTANCE.getBooleanValue()){
            Entity entity = (Entity) (Object) this;
            var player = MinecraftClient.getInstance().player;
            if (entity == player) {
                if (player.isGliding()){
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
            var player = MinecraftClient.getInstance().player;
            if (entity == player) {
                if (player.isGliding()){
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
