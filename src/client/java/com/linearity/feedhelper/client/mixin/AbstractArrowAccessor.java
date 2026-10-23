package com.linearity.feedhelper.client.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Invoker("getWaterInertia")
    float feedhelper$getWaterInertia();
    @Invoker("isInGround")
    boolean feedhelper$isInGround();
    @Accessor("INERTIA")
    float feedhelper$getInertia();

}
