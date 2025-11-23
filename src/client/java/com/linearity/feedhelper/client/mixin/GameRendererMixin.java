package com.linearity.feedhelper.client.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.TorchBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.config.FeatureToggle.AVOID_BREAK_TORCH_WHEN_HOLD_PICKAXE;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    private static HitResult ensureTargetInRange(HitResult hitResult, Vec3d cameraPos, double interactionRange) {
        return null;
    }

    @Inject(
            method = "findCrosshairTarget",
            at = @At("RETURN"),
            cancellable = true
    )
    private void afterFindCrosshairTarget(
            Entity camera, double blockInteractionRange, double entityInteractionRange, float tickProgress, CallbackInfoReturnable<HitResult> cir
    ) {
        if (AVOID_BREAK_TORCH_WHEN_HOLD_PICKAXE.getBooleanValue()){
            MinecraftClient client = MinecraftClient.getInstance();

            HitResult result = cir.getReturnValue();
            if (!(result instanceof BlockHitResult blockHit)) return;

            // 世界/玩家检查
            if (client.world == null || client.player == null) return;

            // 例如手持特定物品才生效
            if (!client.player.getMainHandStack().isIn(ItemTags.PICKAXES)) return;

            BlockPos pos = blockHit.getBlockPos();
            BlockState state = client.world.getBlockState(pos);

            // 必须命中火把
            if (!(state.getBlock() instanceof TorchBlock)) return;

            // 计算火把后方的位置
            Direction side = blockHit.getSide();
            BlockPos newPos = pos.offset(side);

            // 替换成命中后方方块
            BlockHitResult newResult = new BlockHitResult(
                    blockHit.getPos(),
                    side,
                    newPos,
                    false
            );

            Vec3d vec3d = camera.getCameraPosVec(tickProgress);
            cir.setReturnValue(ensureTargetInRange(newResult,vec3d,entityInteractionRange));
        }
    }
}
