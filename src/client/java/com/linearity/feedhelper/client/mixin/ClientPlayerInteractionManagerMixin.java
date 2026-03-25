package com.linearity.feedhelper.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.client.utils.ActiveDefenseRelated.forcedShield;
import static com.linearity.feedhelper.client.utils.InventoryUtils.equipToHandIf;
import static com.linearity.feedhelper.config.FeatureToggle.FILL_LAVA_WHEN_MINING;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(
            method = "releaseUsingItem",
            at = @At("HEAD"),
            cancellable = true
    )
    public void stopUsingItem(Player player, CallbackInfo ci) {
        if (forcedShield.get()){
            ci.cancel();
            new Exception("ForcedShield").printStackTrace();
        }
    }

    @Inject(
            method = "startDestroyBlock",
            at = @At("HEAD")
    )
    public void tryFillLava(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir){
        tryFillLavaInternal(pos);
    }

    @Inject(
            method = "destroyBlock",
            at = @At("HEAD")
    )
    public void tryFillLava(BlockPos pos, CallbackInfoReturnable<Boolean> cir){
        tryFillLavaInternal(pos);
    }

    @Unique
    private static void tryFillLavaInternal(BlockPos pos){
        if (FILL_LAVA_WHEN_MINING.getBooleanValue()){
            var world = Minecraft.getInstance().level;
            var player = Minecraft.getInstance().player;
            var interactionManager = Minecraft.getInstance().gameMode;
            if (world == null || interactionManager == null || player == null){return;}
            var stack = player.getOffhandItem();
            var remainingBefore = stack.getCount();
            var usingItem = stack.getItem();
            if (!(usingItem instanceof BlockItem)){return;}
            for (Direction side : Direction.values()) {
                var checkPos = pos.offset(side.getUnitVec3i());
                var fluid = world.getBlockState(checkPos).getFluidState().getType();
                if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA){
                    interactionManager.useItemOn(player, InteractionHand.OFF_HAND, new BlockHitResult(
                            pos.getCenter(),
                            side.getOpposite(),
                            checkPos,
                            false
                    ));
                    if (remainingBefore == 1){
                        equipToHandIf(stack1 -> stack1.is(usingItem),Minecraft.getInstance(),InteractionHand.OFF_HAND);
                    }
                }
            }
        }
    }
}
