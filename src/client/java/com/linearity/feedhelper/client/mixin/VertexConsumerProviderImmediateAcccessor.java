package com.linearity.feedhelper.client.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VertexConsumerProvider.Immediate.class)
public interface VertexConsumerProviderImmediateAcccessor {
    @Accessor("allocator")
    BufferAllocator getAllocator();
}
