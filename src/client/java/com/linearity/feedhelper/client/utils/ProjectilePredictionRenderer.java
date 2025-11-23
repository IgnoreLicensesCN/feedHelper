package com.linearity.feedhelper.client.utils;

import net.minecraft.block.ShapeContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class ProjectilePredictionRenderer {

    /**
     * 统一预测玩家或实体投射物轨迹
     * @param startPos 起点（玩家眼睛位置或实体位置）
     * @param initialVelocity 初速度向量
     * @param world 客户端世界
     * @param maxTicks 最大预测 tick 数
     * @param affectedByGravity 是否受重力
     * @param affectedByWater 是否受水阻力
     * @return List<Vec3d> 轨迹点
     */
    public static List<Vec3d> predictProjectilePath(Vec3d startPos, Vec3d initialVelocity,
                                                    ClientWorld world, int maxTicks,
                                                    boolean affectedByGravity, boolean affectedByWater) {
        List<Vec3d> path = new ArrayList<>();
        Vec3d pos = startPos;
        Vec3d vel = initialVelocity;

        path.add(pos);

        // Minecraft 内部物理参数（可调）
        double gravity = affectedByGravity ? -0.05 : 0.0;
        double dragAir = 0.99;
        double dragWater = 0.8;
        double tickLength = 1.0; // 每 tick 模拟一次

        for (int tick = 0; tick < maxTicks; tick++) {
            Vec3d nextPos = pos.add(vel.multiply(tickLength));

            // 射线检测方块
            RaycastContext ctx = new RaycastContext(pos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.ANY,
                    ShapeContext.absent()
            );
            BlockHitResult hit = world.raycast(ctx);
            if (hit.getType() != HitResult.Type.MISS) {
                path.add(hit.getPos());
                break;
            }

            pos = nextPos;
            path.add(pos);

            // 水中阻力
            boolean inWater = affectedByWater && world.getBlockState(BlockPos.ofFloored(pos)).getFluidState().isIn(FluidTags.WATER);
            vel = vel.multiply(inWater ? dragWater : dragAir);

            // 重力
            if (affectedByGravity) {
                vel = vel.add(0, gravity, 0);
            }
        }

        return path;
    }

    /**
     * 渲染投射物路径
     */
    public static void renderProjectilePath(MatrixStack matrices, List<Vec3d> path, Vec3d cameraPos,
                                            float[] color, double beamWidth, float sphereRadius) {
        if (path.isEmpty()) return;

        matrices.push();

        // 渲染光束
        for (int i = ((int)Math.floor(path.size()*0.15)); i < path.size() - 1; i++) {
            Vec3d start = path.get(i).subtract(cameraPos);
            Vec3d end = path.get(i + 1).subtract(cameraPos);
            RenderingUtils.renderBeam(matrices, start, end, color, (float) beamWidth);
        }

        // 渲染终点小球
        Vec3d hitPos = path.get(path.size() - 1).subtract(cameraPos);
        matrices.push();
        matrices.translate(hitPos.x, hitPos.y, hitPos.z);
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, sphereRadius, color, 8, 8);
        matrices.pop();

    }

    /**
     * 玩家拉弓预测
     */
    public static void renderPlayerBowPrediction(PlayerEntity player, ItemStack bow,
                                                 ClientWorld world, MatrixStack matrices,
                                                 Vec3d cameraPos, float tickDelta) {
        int useTicks = player.getItemUseTime();
        float charge = BowItem.getPullProgress(useTicks);

        Vec3d startPos = player.getEyePos();
        Vec3d velocity = player.getRotationVector().multiply(charge * 3.0); // Minecraft 弓初速度

        List<Vec3d> path = predictProjectilePath(startPos, velocity, world, 50, true, true);
        renderProjectilePath(matrices, path, cameraPos, new float[]{1f, 0.9f, 0.1f, 0.5f}, 0.1, 0.5f);
    }

    public static void renderPlayerCrossbowPrediction(PlayerEntity player,
                                                       ItemStack stack,
                                                       ClientWorld world,
                                                       MatrixStack matrices,
                                                       Vec3d cameraPos,
                                                       float tickDelta) {
        if (!(stack.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(stack)) return;

        ChargedProjectilesComponent projectiles = stack.getOrDefault(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);

        boolean multishot = stack.getEnchantments().getEnchantments().stream()
                .anyMatch(e -> e.getKey().map(k -> k == Enchantments.MULTISHOT).orElse(false));

        float[] angles = multishot ? new float[]{0f, 10f, -10f} : new float[]{0f};

        Vec3d startPos = player.getEyePos();
        float yaw = player.getYaw(tickDelta);
        float pitch = player.getPitch(tickDelta);
        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);

        Vec3d look = new Vec3d(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        Vec3d right = new Vec3d(
                Math.cos(yawRad),
                0,
                -Math.sin(yawRad)
        ).normalize();

        for (ItemStack proj : projectiles.getProjectiles()) {
            for (float angleDeg : angles) {
                double angleRad = Math.toRadians(angleDeg);
                Vec3d rotated = look.multiply(Math.cos(angleRad))
                        .add(right.multiply(Math.sin(angleRad)))
                        .normalize();

                boolean isFirework = proj.isOf(Items.FIREWORK_ROCKET);
                Vec3d velocity = rotated.multiply(isFirework ? 1.6f : 2.5f);
                int maxTicks = 50;

                List<Vec3d> path = predictProjectilePath(startPos, velocity, world, maxTicks,
                        !isFirework, true);

                if (path.isEmpty()) continue;

                // 颜色计算
                float[] color = new float[4];
                color[3] = 0.5f; // 半透明

                if (isFirework) {
                    var fireworkComp = proj.getComponents().get(DataComponentTypes.FIREWORKS);
                    if (fireworkComp != null && !fireworkComp.explosions().isEmpty()) {
                        FireworkExplosionComponent explosion = fireworkComp.explosions().get(0);
                        if (!explosion.colors().isEmpty()) {
                            int c = explosion.colors().getInt(0);
                            color[0] = ((c >> 16) & 0xFF) / 255f;
                            color[1] = ((c >> 8) & 0xFF) / 255f;
                            color[2] = (c & 0xFF) / 255f;
                        }
                        if (!explosion.fadeColors().isEmpty()) {
                            int c = explosion.fadeColors().getInt(0);
                            color[0] = (color[0] + ((c >> 16) & 0xFF) / 255f) / 2f;
                            color[1] = (color[1] + ((c >> 8) & 0xFF) / 255f) / 2f;
                            color[2] = (color[2] + (c & 0xFF) / 255f) / 2f;
                        }
                    } else {
                        color[0] = 1f; color[1] = 0.5f; color[2] = 0f; // 默认橙色
                    }
                } else {
                    color[0] = 1f; color[1] = 1f; color[2] = 0f; // 弩箭黄色
                }

                float beamWidth = isFirework ? 0.05f : 0.1f;

                // 渲染光束
                matrices.push();
                for (int i = (int)Math.floor(path.size() * 0.2); i < path.size() - 1; i++) {
                    Vec3d start = path.get(i).subtract(cameraPos);
                    Vec3d end = path.get(i + 1).subtract(cameraPos);
                    RenderingUtils.renderBeam(matrices, start, end, color, beamWidth);
                }
                matrices.pop();

                // 渲染落点小球
                Vec3d hitPos = path.get(path.size() - 1).subtract(cameraPos);
                float radius = isFirework ? 1.5f : 0.5f;
                matrices.push();
                matrices.translate(hitPos.x, hitPos.y, hitPos.z);
                RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, 16, 16);
                matrices.pop();
            }
        }
    }




    /**
     * 玩家三叉戟预测
     */
    public static void renderPlayerTridentPrediction(PlayerEntity player, ItemStack trident,
                                                     ClientWorld world, MatrixStack matrices,
                                                     Vec3d cameraPos, float tickDelta) {
        Vec3d startPos = player.getEyePos();
        Vec3d velocity = player.getRotationVector().multiply(2.5); // 三叉戟初速度
        List<Vec3d> path = predictProjectilePath(startPos, velocity, world, 50, true, true);
        renderProjectilePath(matrices, path, cameraPos, new float[]{0.5f, 0.5f, 1f, 0.5f}, 0.1, 0.2f);
    }
}

