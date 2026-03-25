package com.linearity.feedhelper.client.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiBufferSource.BufferSource.class)
public interface VertexConsumerProviderImmediateAcccessor {
    @Accessor("sharedBuffer")
    ByteBufferBuilder getAllocator();
}
