package com.linearity.feedhelper.client.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class BlockPosUtils {
    public static BlockPos fromVec3(Vec3 vec) {
        return new BlockPos(
                Mth.floor(vec.x),
                Mth.floor(vec.y),
                Mth.floor(vec.z)
        );
    }
}
