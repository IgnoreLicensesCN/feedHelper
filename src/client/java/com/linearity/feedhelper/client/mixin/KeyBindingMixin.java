package com.linearity.feedhelper.client.mixin;


import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.client.FeedhelperClient.forceSneakingFlag;
import static com.linearity.feedhelper.client.utils.ActiveDefenseRelated.forcedShield;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

@Mixin(KeyMapping.class)
public abstract class KeyBindingMixin {
    @Final
    @Shadow private String name;

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void forcePressed(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping self = (KeyMapping) (Object) this;

        if ("key.sneak".equals(self.getName()) && forceSneakingFlag.get()) {
            System.out.println("force sneaking");
            cir.setReturnValue(true);  // 永远为 true
        }
        if (self == Minecraft.getInstance().options.keyUse && forcedShield.get()){
            cir.setReturnValue(true);
        }
    }

}
