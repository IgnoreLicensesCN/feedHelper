package com.linearity.feedhelper.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import net.minecraft.client.multiplayer.ClientPacketListener;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

//    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"))
//    private void onEntityVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
//        var client = Minecraft.getInstance();
//        ClientWorld world = client.world;
//        if (world == null) return;
//
//        Entity entity = world.getEntityById(packet.getEntityId());
//        if (entity instanceof EnderDragonEntity dragon) {
//            DragonPredictor.updateVelocity(dragon, packet.getVelocity());
//        }
//    }
//
//    @Inject(
//            method = "setPosition",
//            at = @At("TAIL")
//    )
//    private static void onSetPosition(
//            EntityPosition pos, Set<PositionFlag> flags, Entity entity, boolean bl, CallbackInfoReturnable<Boolean> cir
//    ) {
//        if (entity instanceof EnderDragonEntity dragon) {
//            Vec3d worldPos = entity.getEntityPos();
//            Vec3d vel = entity.getVelocity();
//
//            // 这里你就可以进行预测了
//            DragonPredictor.updateServerPosition(dragon, worldPos);
//            DragonPredictor.updateVelocity(dragon, vel);
//        }
//    }
}
