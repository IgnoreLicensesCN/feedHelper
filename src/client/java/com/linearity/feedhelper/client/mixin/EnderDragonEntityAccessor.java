package com.linearity.feedhelper.client.mixin;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragonEntity.class)
public interface EnderDragonEntityAccessor {

    @Accessor("neck")
    EnderDragonPart getNeck();

    @Accessor("body")
    EnderDragonPart getBody();

    @Accessor("tail1")
    EnderDragonPart getTail1();

    @Accessor("tail2")
    EnderDragonPart getTail2();

    @Accessor("tail3")
    EnderDragonPart getTail3();

    @Accessor("rightWing")
    EnderDragonPart getRightWing();

    @Accessor("leftWing")
    EnderDragonPart getLeftWing();
}
