package com.linearity.feedhelper.client.mixin;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.client.FeedhelperClient.forceSneakingFlag;
import static com.linearity.feedhelper.client.utils.ActiveDefenseRelated.forcedShield;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Final
    @Shadow private String id;

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void forcePressed(CallbackInfoReturnable<Boolean> cir) {
        KeyBinding self = (KeyBinding) (Object) this;

        if ("key.sneak".equals(self.getId()) && forceSneakingFlag.get()) {
            System.out.println("force sneaking");
            cir.setReturnValue(true);  // 永远为 true
        }
        if (self == MinecraftClient.getInstance().options.useKey && forcedShield.get()){
            cir.setReturnValue(true);
        }
    }

}
