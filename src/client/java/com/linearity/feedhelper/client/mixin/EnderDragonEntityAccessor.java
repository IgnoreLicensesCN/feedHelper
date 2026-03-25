package com.linearity.feedhelper.client.mixin;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragon.class)
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

    @Accessor("wing1")
    EnderDragonPart getRightWing();

    @Accessor("wing2")
    EnderDragonPart getLeftWing();
}
