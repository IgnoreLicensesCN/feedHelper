package com.linearity.feedhelper.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.linearity.feedhelper.client.utils.ActiveDefenseRelated.forcedShield;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(
            method = "stopUsingItem",
            at = @At("HEAD"),
            cancellable = true
    )
    public void stopUsingItem(PlayerEntity player, CallbackInfo ci) {
        if (forcedShield.get()){
            ci.cancel();
            new Exception("ForcedShield").printStackTrace();
        }
    }
}
