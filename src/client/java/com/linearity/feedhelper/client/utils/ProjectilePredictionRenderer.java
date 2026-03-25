package com.linearity.feedhelper.client.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

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
    public static List<Vec3> predictProjectilePath(Vec3 startPos, Vec3 initialVelocity,
                                                    ClientLevel world, int maxTicks,
                                                    boolean affectedByGravity, boolean affectedByWater) {
        List<Vec3> path = new ArrayList<>();
        Vec3 pos = startPos;
        Vec3 vel = initialVelocity;

        path.add(pos);

        // Minecraft 内部物理参数（可调）
        double gravity = affectedByGravity ? -0.05 : 0.0;
        double dragAir = 0.99;
        double dragWater = 0.8;
        double tickLength = 1.0; // 每 tick 模拟一次

        for (int tick = 0; tick < maxTicks; tick++) {
            Vec3 nextPos = pos.add(vel.scale(tickLength));

            // 射线检测方块
            ClipContext ctx = new ClipContext(pos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY,
                    CollisionContext.empty()
            );
            BlockHitResult hit = world.clip(ctx);
            if (hit.getType() != HitResult.Type.MISS) {
                path.add(hit.getLocation());
                break;
            }

            pos = nextPos;
            path.add(pos);

            // 水中阻力
            boolean inWater = affectedByWater && world.getBlockState(BlockPos.containing(pos)).getFluidState().is(FluidTags.WATER);
            vel = vel.scale(inWater ? dragWater : dragAir);

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
    public static void renderProjectilePath(PoseStack matrices, List<Vec3> path, Vec3 cameraPos,
                                            float[] color, double beamWidth, float sphereRadius) {
        if (path.isEmpty()) return;

        matrices.pushPose();

        // 渲染光束
        for (int i = ((int)Math.floor(path.size()*0.15)); i < path.size() - 1; i++) {
            Vec3 start = path.get(i).subtract(cameraPos);
            Vec3 end = path.get(i + 1).subtract(cameraPos);
            RenderingUtils.renderBeam(matrices, start, end, color, (float) beamWidth);
        }

        // 渲染终点小球
        Vec3 hitPos = path.get(path.size() - 1).subtract(cameraPos);
        matrices.pushPose();
        matrices.translate(hitPos.x, hitPos.y, hitPos.z);
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, sphereRadius, color, 8, 8);
        matrices.popPose();

    }

    /**
     * 玩家拉弓预测
     */
    public static void renderPlayerBowPrediction(Player player, ItemStack bow,
                                                 ClientLevel world, PoseStack matrices,
                                                 Vec3 cameraPos, float tickDelta) {
        int useTicks = player.getTicksUsingItem();
        float charge = BowItem.getPowerForTime(useTicks);

        Vec3 startPos = player.getEyePosition();
        Vec3 velocity = player.getLookAngle().scale(charge * 3.0); // Minecraft 弓初速度

        List<Vec3> path = predictProjectilePath(startPos, velocity, world, 50, true, true);
        renderProjectilePath(matrices, path, cameraPos, new float[]{1f, 0.9f, 0.1f, 0.5f}, 0.1, 0.5f);
    }

    public static void renderPlayerCrossbowPrediction(Player player,
                                                       ItemStack stack,
                                                       ClientLevel world,
                                                       PoseStack matrices,
                                                       Vec3 cameraPos,
                                                       float tickDelta) {
        if (!(stack.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(stack)) return;

        ChargedProjectiles projectiles = stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);

        boolean multishot = stack.getEnchantments().keySet().stream()
                .anyMatch(e -> e.unwrapKey().map(k -> k == Enchantments.MULTISHOT).orElse(false));

        float[] angles = multishot ? new float[]{0f, 10f, -10f} : new float[]{0f};

        Vec3 startPos = player.getEyePosition();
        float yaw = player.getViewYRot(tickDelta);
        float pitch = player.getViewXRot(tickDelta);
        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);

        Vec3 look = new Vec3(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        Vec3 right = new Vec3(
                Math.cos(yawRad),
                0,
                -Math.sin(yawRad)
        ).normalize();

        for (ItemStack proj : projectiles.getItems()) {
            for (float angleDeg : angles) {
                double angleRad = Math.toRadians(angleDeg);
                Vec3 rotated = look.scale(Math.cos(angleRad))
                        .add(right.scale(Math.sin(angleRad)))
                        .normalize();

                boolean isFirework = proj.is(Items.FIREWORK_ROCKET);
                Vec3 velocity = rotated.scale(isFirework ? 1.6f : 2.5f);
                int maxTicks = 50;

                List<Vec3> path = predictProjectilePath(startPos, velocity, world, maxTicks,
                        !isFirework, true);

                if (path.isEmpty()) continue;

                // 颜色计算
                float[] color = new float[4];
                color[3] = 0.5f; // 半透明

                if (isFirework) {
                    var fireworkComp = proj.getComponents().get(DataComponents.FIREWORKS);
                    if (fireworkComp != null && !fireworkComp.explosions().isEmpty()) {
                        FireworkExplosion explosion = fireworkComp.explosions().get(0);
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
                matrices.pushPose();
                for (int i = (int)Math.floor(path.size() * 0.2); i < path.size() - 1; i++) {
                    Vec3 start = path.get(i).subtract(cameraPos);
                    Vec3 end = path.get(i + 1).subtract(cameraPos);
                    RenderingUtils.renderBeam(matrices, start, end, color, beamWidth);
                }
                matrices.popPose();

                // 渲染落点小球
                Vec3 hitPos = path.get(path.size() - 1).subtract(cameraPos);
                float radius = isFirework ? 1.5f : 0.5f;
                matrices.pushPose();
                matrices.translate(hitPos.x, hitPos.y, hitPos.z);
                RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, 16, 16);
                matrices.popPose();
            }
        }
    }




    /**
     * 玩家三叉戟预测
     */
    public static void renderPlayerTridentPrediction(Player player, ItemStack trident,
                                                     ClientLevel world, PoseStack matrices,
                                                     Vec3 cameraPos, float tickDelta) {
        Vec3 startPos = player.getEyePosition();
        Vec3 velocity = player.getLookAngle().scale(2.5); // 三叉戟初速度
        List<Vec3> path = predictProjectilePath(startPos, velocity, world, 50, true, true);
        renderProjectilePath(matrices, path, cameraPos, new float[]{0.5f, 0.5f, 1f, 0.5f}, 0.1, 0.2f);
    }
}

