package com.linearity.feedhelper.client.mixin;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.PiglinEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PiglinEntity.class)
public interface PiglinEntityAccessor {
    @Accessor("CHARGING")
    TrackedData<Boolean> getChargingTrackedData();
}
