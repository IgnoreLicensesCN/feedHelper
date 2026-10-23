package com.linearity.feedhelper.client.utils.projectiletrace;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public record ProjectileSimulateContext<A>(
        ClientLevel level,
        boolean isInGround,
        InertiaAtPosFunction inertiaAtPos,
        boolean isNoPhysics,
        double gravity,
        AABB boundingBoxAtZeroPos,
        @Nullable A additionalInfo
) {
    public interface InertiaAtPosFunction {
        float calculate(BlockPos pos);
    }
}
