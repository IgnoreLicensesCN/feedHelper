package com.linearity.feedhelper.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.config.FeatureToggle.AVOID_BREAK_TORCH_WHEN_HOLD_PICKAXE;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    Minecraft minecraft;

    @Inject(
            method = "pick",
            at = @At("RETURN"),
            cancellable = true
    )
    private void afterUpdateCrosshairTarget(
            float tickProgress, CallbackInfo cir
    ) {
        if (AVOID_BREAK_TORCH_WHEN_HOLD_PICKAXE.getBooleanValue()){
            Minecraft client = Minecraft.getInstance();

            HitResult result = client.hitResult;
            if (!(result instanceof BlockHitResult blockHit)) return;

            // 世界/玩家检查
            if (client.level == null || client.player == null) return;

            // 例如手持特定物品才生效
            if (!client.player.getMainHandItem().is(ItemTags.PICKAXES)) return;

            BlockPos pos = blockHit.getBlockPos();
            BlockState state = client.level.getBlockState(pos);

            // 必须命中火把
            if (!(state.getBlock() instanceof TorchBlock)) return;

            client.hitResult = null;
//            // 计算火把后方的位置
//            Direction side = blockHit.getSide();
//            BlockPos newPos = pos.offset(side);
//
//            // 替换成命中后方方块
//            BlockHitResult newResult = new BlockHitResult(
//                    blockHit.getPos(),
//                    side,
//                    newPos,
//                    false
//            );
//
//            Vec3d vec3d = camera.getCameraPosVec(tickProgress);
//            cir.setReturnValue(ensureTargetInRange(newResult,vec3d,entityInteractionRange));
        }
    }
}
