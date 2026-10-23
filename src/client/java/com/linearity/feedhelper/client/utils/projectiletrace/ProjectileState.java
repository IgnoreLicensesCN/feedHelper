package com.linearity.feedhelper.client.utils.projectiletrace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public record ProjectileState(
        Vec3 pos,
        Vec3 velocity,
        boolean onFire,
        BlockPos blockPos,
        int lifetime,
        HitResult hitResult
) {
}
