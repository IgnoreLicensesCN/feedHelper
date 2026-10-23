package com.linearity.feedhelper.client.mixin;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.component.FireworkExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketEntityAccessor {
    @Accessor("life")
    int feedhelper$getLife();
    @Accessor("lifetime")
    int feedhelper$getLifetime();
    @Invoker("getExplosions")
    List<FireworkExplosion> feedhelper$getExplosions();
}
