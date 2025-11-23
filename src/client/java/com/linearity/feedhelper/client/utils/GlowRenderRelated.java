package com.linearity.feedhelper.client.utils;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;

public class GlowRenderRelated {

    public static RenderLayer createPlayerEmissiveLayer(Identifier skinTexture) {
        RenderLayer.MultiPhaseParameters params = RenderLayer.MultiPhaseParameters.builder()
                .texture(new RenderPhase.Texture(skinTexture, false))
                .overlay(RenderPhase.DISABLE_OVERLAY_COLOR)
                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                .build(false); // affectsOutline=false

        return RenderLayer.of(
                "player_emissive",
                1536,
                true,
                true,
                RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE,
                params
        );
    }
}
