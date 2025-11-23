package com.linearity.feedhelper.client.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import static com.linearity.feedhelper.config.Configs.Generic.AVOID_ELYTRA_COLLISION_MIN_SPEED;
import static net.minecraft.util.math.MathHelper.lerp;

public class ElytraHelper {

    public static void avoidElytraCollisionLoop(MinecraftClient client) {
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) return;
        if (!player.isGliding()) return;

        Vec3d vel = player.getVelocity();

        double minSpeed = AVOID_ELYTRA_COLLISION_MIN_SPEED.getDoubleValue();
        // 飞太慢，不需要反射
        if (vel.lengthSquared() < minSpeed*minSpeed) return;

        Vec3d startPos = player.getEntityPos();

        int i = 0;
        for (i = 0; i < 20; i++) {

            Vec3d futurePos = startPos.add(vel.multiply(5));

            HitResult hr = world.raycast(new RaycastContext(
                    startPos,
                    futurePos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player
            ));

            // 没撞上 → 结束循环
            if (hr.getType() != HitResult.Type.BLOCK && hr.getType() != HitResult.Type.ENTITY) {
                break;
            }

            // 撞上 → 计算反射
            BlockHitResult bhr = (BlockHitResult) hr;
            Direction hitFace = bhr.getSide();

            // 碰撞面的法线
            Vec3d normal = Vec3d.of(hitFace.getVector());

            // 反射： R = V - 2*(V·N)*N
            vel = vel.subtract(normal.multiply(2 * vel.dotProduct(normal)));

            // 加 **随机偏差（-4° 到 +4°）**
            float deviationDegrees = (float)((Math.random() - 0.5) * 8.0);
            vel = vel.rotateY((float)Math.toRadians(deviationDegrees));

            // 更新起点 → 下次继续往未来预测
            startPos = bhr.getPos().add(normal.multiply(0.1));  // 稍微往外推一点
        }

        if (i == 0){return;}
        // 应用最终飞行速度
        player.setVelocity(vel);
        player.velocityModified = true;
        if (vel.lengthSquared() > minSpeed) {

            // 根据速度向量计算 yaw & pitch
            double dx = vel.x;
            double dy = vel.y;
            double dz = vel.z;

            float targetYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
            float targetPitch = (float)Math.toDegrees(-Math.atan2(
                    dy,
                    Math.sqrt(dx*dx + dz*dz)
            ));

            // 平滑插值（越大视角转得越快）
            float smooth = 0.35f;

            player.setYaw(targetYaw);
            player.setPitch(targetPitch);
        }
    }
}
