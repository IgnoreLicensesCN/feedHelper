package com.linearity.feedhelper.client.mixin;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.client.utils.GlowRenderRelated.createPlayerEmissiveLayer;


@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Inject(method = "getRenderLayer", at = @At("RETURN"), cancellable = true)
    private void changeRenderLayer(S state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> cir) {
        // ARGB 例子：不透明绿色
        RenderLayer renderLayer = cir.getReturnValue();
        if (renderLayer == null){return;}
        if (state instanceof PlayerEntityRenderState playerRenderState) {
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
