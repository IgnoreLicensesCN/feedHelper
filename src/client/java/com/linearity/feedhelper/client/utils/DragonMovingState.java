package com.linearity.feedhelper.client.utils;

import com.linearity.feedhelper.client.mixin.EnderDragonEntityAccessor;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.Map;
//it's not possible
public record DragonMovingState(
        Vec3 serverPos,
        Vec3 velocity,
        long lastServerTick,
        Map<String, Vec3> bodyPartPositions
) {
    /** 构造器采集每个 Part 位置 */
    public DragonMovingState(EnderDragon entity, long tickTime) {
        this(entity.position(), entity.getDeltaMovement(), tickTime,
                new LinkedHashMap<>() {{
                    put("head", entity.head.position());
                    put("neck", ((EnderDragonEntityAccessor)entity).getNeck().position());
                    put("body", ((EnderDragonEntityAccessor)entity).getBody().position());
                    put("tail1", ((EnderDragonEntityAccessor)entity).getTail1().position());
                    put("tail2", ((EnderDragonEntityAccessor)entity).getTail2().position());
                    put("tail3", ((EnderDragonEntityAccessor)entity).getTail3().position());
                    put("rightWing", ((EnderDragonEntityAccessor)entity).getRightWing().position());
                    put("leftWing", ((EnderDragonEntityAccessor)entity).getLeftWing().position());
                }}
        );
    }
    public DragonMovingState(EnderDragon entity,
                             Vec3 serverPos,
                             Vec3 velocity,
                             long lastServerTick
    ) {
        this(serverPos, velocity, lastServerTick,
                new LinkedHashMap<>() {{
                    put("head", entity.head.position());
                    put("neck", ((EnderDragonEntityAccessor)entity).getNeck().position());
                    put("body", ((EnderDragonEntityAccessor)entity).getBody().position());
                    put("tail1", ((EnderDragonEntityAccessor)entity).getTail1().position());
                    put("tail2", ((EnderDragonEntityAccessor)entity).getTail2().position());
                    put("tail3", ((EnderDragonEntityAccessor)entity).getTail3().position());
                    put("rightWing", ((EnderDragonEntityAccessor)entity).getRightWing().position());
                    put("leftWing", ((EnderDragonEntityAccessor)entity).getLeftWing().position());
                }}
        );
    }
}
