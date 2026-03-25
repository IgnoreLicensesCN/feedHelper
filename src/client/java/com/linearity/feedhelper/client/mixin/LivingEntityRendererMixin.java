package com.linearity.feedhelper.client.mixin;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Inject(method = "getRenderType", at = @At("RETURN"), cancellable = true)
    private void changeRenderLayer(S state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderType> cir) {
        // ARGB 例子：不透明绿色
        RenderType renderLayer = cir.getReturnValue();
        if (renderLayer == null){return;}
        if (state instanceof AvatarRenderState playerRenderState) {
//            Identifier skin = playerRenderState.skinTextures.body().texturePath(); // 或者 skinTextures.skin()
//
//            // 构造发光 RenderLayer
//            RenderLayer emissiveLayer = RenderLayer.of(
//                    "player_emissive",
//                    1536,
//                    true,   // hasCrumbling
//                    true,   // translucent
//                    RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE,
//                    RenderLayer.MultiPhaseParameters.builder()
//                            .texture(new RenderPhase.Texture(skin, false))
//                            .overlay(RenderPhase.DISABLE_OVERLAY_COLOR)
//                            .lightmap(RenderPhase.ENABLE_LIGHTMAP)
//                            .build(false)
//            );
//
//            cir.setReturnValue(emissiveLayer);
        }
        else {
//            cir.setReturnValue(0xFF999999);
        }
    }
}
