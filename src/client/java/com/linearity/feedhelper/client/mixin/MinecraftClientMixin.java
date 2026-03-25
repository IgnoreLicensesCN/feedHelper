package com.linearity.feedhelper.client.mixin;

import com.linearity.feedhelper.config.FeatureToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void forceOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (FeatureToggle.ENTITY_GLOWING.getBooleanValue()){
            cir.setReturnValue(true);
        }
    }

}
