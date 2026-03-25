package com.linearity.feedhelper.client.mixin;


import com.linearity.feedhelper.config.FeatureToggle;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void alwaysGlow(CallbackInfoReturnable<Boolean> cir) {
        if (FeatureToggle.ENTITY_GLOWING.getBooleanValue()){
            cir.setReturnValue(true);
        }
    }

}
