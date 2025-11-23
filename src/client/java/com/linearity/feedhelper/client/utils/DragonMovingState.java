package com.linearity.feedhelper.client.utils;

import com.linearity.feedhelper.client.mixin.EnderDragonEntityAccessor;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

import java.util.LinkedHashMap;
import java.util.Map;

public record DragonMovingState(
        Vec3d serverPos,
        Vec3d velocity,
        long lastServerTick,
        Map<String, Vec3d> bodyPartPositions
) {
    /** 构造器采集每个 Part 位置 */
    public DragonMovingState(EnderDragonEntity entity, long tickTime) {
        this(entity.getEntityPos(), entity.getVelocity(), tickTime,
                new LinkedHashMap<>() {{
                    put("head", entity.head.getEntityPos());
                    put("neck", ((EnderDragonEntityAccessor)entity).getNeck().getEntityPos());
                    put("body", ((EnderDragonEntityAccessor)entity).getBody().getEntityPos());
                    put("tail1", ((EnderDragonEntityAccessor)entity).getTail1().getEntityPos());
                    put("tail2", ((EnderDragonEntityAccessor)entity).getTail2().getEntityPos());
                    put("tail3", ((EnderDragonEntityAccessor)entity).getTail3().getEntityPos());
                    put("rightWing", ((EnderDragonEntityAccessor)entity).getRightWing().getEntityPos());
                    put("leftWing", ((EnderDragonEntityAccessor)entity).getLeftWing().getEntityPos());
                }}
        );
    }
    public DragonMovingState(EnderDragonEntity entity,
                             Vec3d serverPos,
                             Vec3d velocity,
                             long lastServerTick
    ) {
        this(serverPos, velocity, lastServerTick,
                new LinkedHashMap<>() {{
                    put("head", entity.head.getEntityPos());
                    put("neck", ((EnderDragonEntityAccessor)entity).getNeck().getEntityPos());
                    put("body", ((EnderDragonEntityAccessor)entity).getBody().getEntityPos());
                    put("tail1", ((EnderDragonEntityAccessor)entity).getTail1().getEntityPos());
                    put("tail2", ((EnderDragonEntityAccessor)entity).getTail2().getEntityPos());
                    put("tail3", ((EnderDragonEntityAccessor)entity).getTail3().getEntityPos());
                    put("rightWing", ((EnderDragonEntityAccessor)entity).getRightWing().getEntityPos());
                    put("leftWing", ((EnderDragonEntityAccessor)entity).getLeftWing().getEntityPos());
                }}
        );
    }
}
