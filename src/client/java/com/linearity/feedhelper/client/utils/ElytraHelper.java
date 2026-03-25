package com.linearity.feedhelper.client.utils;

import static com.linearity.feedhelper.config.Configs.Generic.AVOID_ELYTRA_COLLISION_MIN_SPEED;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ElytraHelper {

    public static void avoidElytraCollisionLoop(Minecraft client) {
        var player = client.player;
        var world = client.level;
        if (player == null || world == null) return;
        if (!player.isFallFlying()) return;

        Vec3 vel = player.getDeltaMovement();

        double minSpeed = AVOID_ELYTRA_COLLISION_MIN_SPEED.getDoubleValue();
        // 飞太慢，不需要反射
        if (vel.lengthSqr() < minSpeed*minSpeed) return;

        Vec3 startPos = player.position();

        int i = 0;
        for (i = 0; i < 20; i++) {

            Vec3 futurePos = startPos.add(vel.scale(5));

            BlockHitResult hr = world.clip(new ClipContext(
                    startPos,
                    futurePos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            // 没撞上 → 结束循环
            if (hr.getType() != HitResult.Type.BLOCK && hr.getType() != HitResult.Type.ENTITY) {
                break;
            }

            // 撞上 → 计算反射
            Direction hitFace = hr.getDirection();

            // 碰撞面的法线
            Vec3 normal = Vec3.atLowerCornerOf(hitFace.getUnitVec3i());

            // 反射： R = V - 2*(V·N)*N
            vel = vel.subtract(normal.scale(2 * vel.dot(normal)));

            // 加 **随机偏差（-4° 到 +4°）**
            float deviationDegrees = (float)((Math.random() - 0.5) * 8.0);
            vel = vel.yRot((float)Math.toRadians(deviationDegrees));

            // 更新起点 → 下次继续往未来预测
            startPos = hr.getLocation().add(normal.scale(0.1));  // 稍微往外推一点
        }

        if (i == 0){return;}
        // 应用最终飞行速度
        player.setDeltaMovement(vel);
        player.needsSync = true;
        if (vel.lengthSqr() > minSpeed) {

            // 根据速度向量计算 yaw & pitch
            double dx = vel.x;
            double dy = vel.y;
            double dz = vel.z;

            float targetYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
            float targetPitch = (float)Math.toDegrees(-Math.atan2(
                    dy,
                    Math.sqrt(dx*dx + dz*dz)
            ));

            player.setYRot(targetYaw);
            player.setXRot(targetPitch);
        }
    }
}
