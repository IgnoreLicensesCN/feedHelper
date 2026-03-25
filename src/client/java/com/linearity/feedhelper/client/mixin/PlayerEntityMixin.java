package com.linearity.feedhelper.client.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
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
