package com.linearity.feedhelper.client.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.piglin.Piglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Piglin.class)
public interface PiglinEntityAccessor {
    @Accessor("DATA_IS_CHARGING_CROSSBOW")
    EntityDataAccessor<Boolean> getChargingTrackedData();
}
