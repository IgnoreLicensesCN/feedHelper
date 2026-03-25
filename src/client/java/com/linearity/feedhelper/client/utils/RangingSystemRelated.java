package com.linearity.feedhelper.client.utils;

import com.linearity.feedhelper.client.mixin.CreeperEntityAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.linearity.feedhelper.client.utils.ProjectilePredictionRenderer.*;
import static com.linearity.feedhelper.client.utils.RenderingUtils.*;

public class RangingSystemRelated {

    public static final float beamWidth = 0.2f;
    public static void rangingSystemLoopRendering(Minecraft client,PoseStack matrices,float tickDelta) {
        renderForPredictEntities(client,matrices,tickDelta);
    }
    public static void rangingSystemLoopRenderingOnHUD(Minecraft client, GuiGraphics context, float tickDelta) {

        renderArrowHitTargetIfExists(client,context,tickDelta);

    }

    public static void renderArrowHitTargetIfExists(Minecraft client, GuiGraphics context, float tickDelta) {
        var player = client.player;
        var world = client.level;
        if (player == null || world == null) return;

        List<Vec3> arrowPositions = new ArrayList<>();
        List<Vec3> velocityVectors = new ArrayList<>();

        // 玩家拉弓中
        if (player.isUsingItem() && (player.getUseItem().getItem() instanceof BowItem)) {
            int useTicks = player.getTicksUsingItem();
            float f = useTicks / 20.0f;
            float velocity = (f * f + f * 2.0f) / 3.0f;
            velocity = Math.min(velocity, 1.0f);

            Vec3 startPos = player.getEyePosition();
            Vec3 velocityVec = player.getViewVector(tickDelta).scale(velocity * 3.0);

            arrowPositions.add(startPos);
            velocityVectors.add(velocityVec);
        }

        // 检查主手和副手的弩
        List<ItemStack> crossbows = Arrays.asList(player.getMainHandItem(), player.getOffhandItem());
        for (ItemStack stack : crossbows) {
            if (!(stack.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(stack)) continue;

            ChargedProjectiles projectiles = stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            AtomicBoolean hasRocketFlag = new AtomicBoolean(false);
            for (ItemStack proj : projectiles.getItems()) {
                if (proj.is(Items.FIREWORK_ROCKET)) {
                    hasRocketFlag.set(true);
                    break;
                }
            }
            if (hasRocketFlag.get()){
                continue;
            }
            AtomicBoolean hasMultishot = new AtomicBoolean(false);
            stack.getEnchantments().keySet().forEach(e -> e.unwrapKey().ifPresent(key -> {
                if (key == Enchantments.MULTISHOT) hasMultishot.set(true);
            }));

            // 偏移角度
            float[] angles = hasMultishot.get() ? new float[]{0f, 10f, -10f} : new float[]{0f};
            float speed = 3.15f;

            for (float angle : angles) {
                Vec3 startPos = player.getEyePosition();
                // 基于玩家旋转和角度偏移
                float yaw = player.getViewYRot(tickDelta);
                float pitch = player.getViewXRot(tickDelta);
                double yawRad = Math.toRadians(-yaw + angle); // yaw 偏移
                double pitchRad = Math.toRadians(-pitch);

                Vec3 vel = new Vec3(
                        Math.sin(yawRad) * Math.cos(pitchRad),
                        Math.sin(pitchRad),
                        Math.cos(yawRad) * Math.cos(pitchRad)
                ).normalize().scale(speed);

                arrowPositions.add(startPos);
                velocityVectors.add(vel);
            }
        }

        if (arrowPositions.isEmpty()) return;

        // 模拟每条箭的轨迹
        for (int idx = 0; idx < arrowPositions.size(); idx++) {
            Vec3 arrowPos = arrowPositions.get(idx);
            Vec3 velocityVec = velocityVectors.get(idx);

            Entity targetEntity = null;
            BlockHitResult targetBlock = null;
            Vec3 hitPos = null;
            float tickStep = 0f;

            // 获取附近实体列表
            List<Entity> entities = new LinkedList<>();
            world.entitiesForRendering().forEach(entity -> {
                if (entity != player && entity.isAlive() && !entity.isSpectator()) entities.add(entity);
            });

            // 模拟箭矢轨迹
            for (int i = 0; i < 200; i++) {
                Vec3 nextPos = arrowPos.add(velocityVec);

                // 方块碰撞
                BlockHitResult blockHit = world.clip(new ClipContext(
                        arrowPos, nextPos, ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.ANY, player
                ));
                if (blockHit.getType() != HitResult.Type.MISS) {
                    targetBlock = blockHit;
                    hitPos = new Vec3(
                            blockHit.getBlockPos().getX() + 0.5,
                            blockHit.getBlockPos().getY() + 0.5,
                            blockHit.getBlockPos().getZ() + 0.5
                    );
                    break;
                }

                // 实体碰撞
                for (Entity entity : entities) {
                    Vec3 predictedPos = entity.position().add(entity.getDeltaMovement().scale(tickStep));
                    AABB predictedBox = entity.getBoundingBox().move(predictedPos.subtract(entity.position()));
                    Optional<Vec3> intersect = predictedBox.clip(arrowPos, nextPos);
                    if (intersect.isPresent()) {
                        targetEntity = entity;
                        hitPos = intersect.get();
                        break;
                    }
                }
                if (hitPos != null) break;

                // 更新箭矢位置和速度
                arrowPos = nextPos;
                velocityVec = velocityVec.scale(0.99).add(0, -0.05, 0);
                tickStep += 1f;
            }

            if (hitPos == null) continue;

            Vec3 screen = RenderingUtils.worldToScreen(client, hitPos);
            if (screen == null) continue;

            int x = (int) screen.x;
            int y = (int) screen.y;
            RenderingUtils.drawRect(context, x - RenderingUtils.rectSize, y - RenderingUtils.rectSize, x + RenderingUtils.rectSize, y + RenderingUtils.rectSize, 0xFF39C5BB);

            String name = targetEntity != null ? targetEntity.getName().getString()
                    : world.getBlockState(targetBlock.getBlockPos()).getBlock().getName().getString();
            if (name == null) continue;

            String printText = name + " " + String.format("x=%.3f,y=%.3f,z=%.3f", hitPos.x(), hitPos.y(), hitPos.z());
            context.drawString(client.font, printText, x - RenderingUtils.rectSize, y + RenderingUtils.rectSize, 0xFF39C5BB, false);
        }
    }


    public static void renderForPredictEntities(Minecraft client, PoseStack matrices, float tickDelta) {
        var world = client.level;
        if (world == null) return;

        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;

        var cameraPos = camera.position();

        for (Entity entity : world.entitiesForRendering()) {

            // Dragon fireball
            if (entity instanceof DragonFireball fireball) {
                renderDragonFireballHitRange(client,world,fireball, matrices, tickDelta);
                continue;
            }

            // Wither skull
            if (entity instanceof WitherSkull skull) {
                renderWitherFiringSkullHitRange(client,world,skull, matrices, tickDelta, cameraPos);
                continue;
            }
            if (entity instanceof Slime slime){
                renderSlimeJumping(client,world,slime, matrices, tickDelta);
                continue;
            }
            if (entity instanceof Creeper creeper) {
                renderCreeperBoomRange(client,world,creeper, matrices, tickDelta);
                continue;
            }
            if (entity instanceof SmallFireball smallFireball){
                renderSmallFireballHitLocation(client,world,smallFireball, matrices, tickDelta);
                continue;
            }
            if (entity instanceof Arrow arrow){
                renderArrowPredictedPath(client, world, arrow, matrices, tickDelta);
                continue;
            }
            if (entity instanceof Player player){
                renderPlayerWeaponPrediction(client, world,player, matrices, tickDelta);
                continue;
            }
        }
    }
    public static void renderSmallFireballHitLocation(Minecraft client,
                                                      ClientLevel world,
                                                      SmallFireball fireball,
                                                      PoseStack matrices,
                                                      float tickDelta) {
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.position();

        // 预测火球落点
        Vec3 hitPos = predictFireballHitPos(fireball.position(), fireball.getDeltaMovement(), world);

        // 火球影响范围半径（小火球一般较小）
        float radius = 1.5f;

        // 保存矩阵状态
        matrices.pushPose();

        // 平移到落点相对于摄像机的位置
        matrices.translate(hitPos.x - cameraPos.x,
                hitPos.y - cameraPos.y,
                hitPos.z - cameraPos.z);

        // 球体颜色，可以选择橙红色渐变
        float[] color = new float[]{1f, 0.5f, 0f, 0.5f}; // 半透明橙色

        int latDivisions = 16;
        int lonDivisions = 16;

        // 渲染半透明球
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color,
                latDivisions, lonDivisions);

        matrices.popPose();

        // 渲染光束，从火球当前位置指向落点
        Vec3 start = fireball.position().subtract(cameraPos);
        Vec3 end = hitPos.subtract(cameraPos);
        float beamWidth = 0.05f; // 光束宽度
        RenderingUtils.renderBeam(matrices, start, end, color, beamWidth);
    }

    public static List<Vec3> predictArrowPath(Vec3 startPos, Vec3 velocity, ClientLevel world, int maxTicks) {
        List<Vec3> path = new ArrayList<>();
        Vec3 pos = startPos;
        Vec3 vel = velocity;

        float gravity = 0.05f;
        float dragAir = 0.99f;
        float dragWater = 0.6f;

        for (int tick = 0; tick < maxTicks; tick++) {
            path.add(pos);

            Vec3 nextPos = pos.add(vel);

            // 射线检测方块/实体
            var hit = world.clip(new ClipContext(
                    pos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, // 可以检测水
                    CollisionContext.empty()
            ));

            if (hit.getType() != HitResult.Type.MISS && world.getBlockState(hit.getBlockPos()).getFluidState().isEmpty()) {
                path.add(hit.getLocation());
                break;
            }

            // 更新位置
            pos = nextPos;

            // 判断是否在水中减速
            if (world.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER)) {
                vel = vel.scale(dragWater);
            } else {
                vel = vel.scale(dragAir);
            }

            // 受重力影响
            vel = vel.add(0, -gravity, 0);

            // 速度过小提前停止
            if (vel.lengthSqr() < 1e-6) break;
        }

        return path;
    }

    public static void renderPlayerWeaponPrediction(Minecraft client,
                                                    ClientLevel world,
                                                    Player player,
                                                    PoseStack matrices,
                                                    float tickDelta) {
        if (player == null || world == null) return;

        var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        if (camera == null) return;
        Vec3 cameraPos = camera.position();

        // 遍历主手和副手
        List<ItemStack> hands = Arrays.asList(player.getMainHandItem(), player.getOffhandItem());
        for (ItemStack stack : hands) {
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof BowItem) {
                renderPlayerBowPrediction(player, stack, world, matrices, cameraPos,tickDelta);
            } else if (stack.getItem() instanceof CrossbowItem) {
                renderPlayerCrossbowPrediction(player, stack, world, matrices, cameraPos,tickDelta);
            } else if (stack.getItem() instanceof TridentItem) {
                renderPlayerTridentPrediction(player, stack, world, matrices, cameraPos,tickDelta);
            }
        }
    }

    private static void renderFireworkPrediction(Player player,
                                                 ItemStack rocket,
                                                 ClientLevel world,
                                                 PoseStack matrices,
                                                 Vec3 cameraPos,
                                                 float[] angles) {
        var fireworkComp = rocket.getComponents().get(DataComponents.FIREWORKS);
        if (fireworkComp == null) return;

        int flight = fireworkComp.flightDuration();
        List<FireworkExplosion> explosions = fireworkComp.explosions();

        Vec3 startPos = player.getEyePosition();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);

        Vec3 look = new Vec3(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        Vec3 right = new Vec3(Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();

        for (float angleDeg : angles) {
            double angleRad = Math.toRadians(angleDeg);
            Vec3 rotated = look.scale(Math.cos(angleRad))
                    .add(right.scale(Math.sin(angleRad))).normalize();

            Vec3 velocity = rotated.scale(1.6f);
            Vec3 endPos = startPos.add(velocity.scale(flight * 10.0));

            // 射线检测碰撞方块
            ClipContext ctx = new ClipContext(startPos, endPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            );
            BlockHitResult result = world.clip(ctx);
            Vec3 hitPos = result.getType() != HitResult.Type.MISS ? result.getLocation() : endPos;

            // 渲染每个爆炸效果
            for (FireworkExplosion explosion : explosions) {
                float radius = switch (explosion.shape()) {
                    case SMALL_BALL -> 2.0f;
                    case LARGE_BALL -> 3.0f;
                    case STAR -> 3.5f;
                    case CREEPER -> 3.0f;
                    case BURST -> 2.5f;
                };
                float[] color = new float[]{1f, 0.9f, 0.1f, 0.5f};
                matrices.pushPose();
                matrices.translate(hitPos.x - cameraPos.x, hitPos.y - cameraPos.y, hitPos.z - cameraPos.z);
                RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, 32, 32);
                matrices.popPose();
            }
        }
    }

    private static void renderArrowPrediction(Player player,
                                              ItemStack arrow,
                                              ClientLevel world,
                                              PoseStack matrices,
                                              Vec3 cameraPos,
                                              float[] angles) {

        Vec3 startPos = player.getEyePosition();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);

        Vec3 look = new Vec3(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        Vec3 right = new Vec3(Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();

        for (float angleDeg : angles) {
            double angleRad = Math.toRadians(angleDeg);
            Vec3 rotated = look.scale(Math.cos(angleRad))
                    .add(right.scale(Math.sin(angleRad))).normalize();

            double speed = 3.0; // 初速度
            Vec3 velocity = rotated.scale(speed);

            List<Vec3> path = predictProjectilePath(startPos, velocity, world, 50,true,true); // 50 tick 外推

            renderProjectilePath(matrices, path, cameraPos, new float[]{1f, 0.9f, 0.1f, 0.5f}, 0.03f, 0.2f);
        }
    }

    private static void renderProjectilePath(PoseStack matrices,
                                             List<Vec3> path,
                                             Vec3 cameraPos,
                                             float[] color,
                                             float beamWidth,
                                             float hitRadius) {
        if (path.isEmpty()) return;

        matrices.pushPose();
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 start = path.get(i).subtract(cameraPos);
            Vec3 end = path.get(i + 1).subtract(cameraPos);
            RenderingUtils.renderBeam(matrices, start, end, color, beamWidth);
        }

        Vec3 hitPos = path.get(path.size() - 1).subtract(cameraPos);
        matrices.pushPose();
        matrices.translate(hitPos.x, hitPos.y, hitPos.z);
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, hitRadius, color, 32, 32);
        matrices.popPose();
        matrices.popPose();
    }



    public static List<Vec3> predictPlayerArrowPath(Player player, int maxTicks, ClientLevel world) {
        List<Vec3> path = new ArrayList<>();

        // 玩家手持物
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem)) return path;

        // 拉弓进度
        float charge = 1f; // 默认为满拉弓，或者根据 getItemUseTime()/maxUseTime() 计算
        if (stack.getItem() instanceof BowItem) {
            int useTicks = player.getTicksUsingItem();
            int maxUse = stack.getUseDuration(player);
            charge = Math.min(useTicks / 20f, 1f);
        }

        // 初速度
        double maxSpeed = 3.0; // Minecraft 默认箭初速度
        Vec3 velocity = player.getLookAngle().scale(charge * maxSpeed);

        // 初始位置
        Vec3 pos = player.getEyePosition();

        float gravity = 0.05f;
        float dragAir = 0.99f;
        float dragWater = 0.6f;

        for (int tick = 0; tick < maxTicks; tick++) {
            path.add(pos);

            Vec3 nextPos = pos.add(velocity);

            // 射线检测方块
            var hit = world.clip(new ClipContext(
                    pos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.SOURCE_ONLY,
                    CollisionContext.empty()
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                path.add(hit.getLocation());
                break;
            }

            pos = nextPos;

            if (world.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER)) {
                velocity = velocity.scale(dragWater);
            } else {
                velocity = velocity.scale(dragAir);
            }

            velocity = velocity.add(0, -gravity, 0);

            if (velocity.lengthSqr() < 1e-6) break;
        }

        return path;
    }

    public static void renderArrowPredictedPath(Minecraft client,
                                                ClientLevel world,
                                                Arrow arrow,
                                                PoseStack matrices,
                                                float tickDelta) {
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.position();

        if (arrow.getDeltaMovement().lengthSqr() < 1e-6) return;

        // 按 tick 预测箭的路径，最多 50 tick（可调）
        List<Vec3> path = predictArrowPath(arrow.position(), arrow.getDeltaMovement(), world, 50);

        if (path.isEmpty()) return;

        // 颜色和宽度
        float[] color = new float[]{1f, 1f, 1f, 0.5f}; // 半透明黄色
        float beamWidth = .1f;

        // 遍历路径渲染每一段光束
        matrices.pushPose();
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 start = path.get(i).subtract(cameraPos);
            Vec3 end = path.get(i + 1).subtract(cameraPos);
            RenderingUtils.renderBeam(matrices, start, end, color, beamWidth);
        }
        matrices.popPose();

        // 可选：在终点渲染小球表示撞击点
        Vec3 hitPos = path.getLast().subtract(cameraPos);
        float radius = 0.5f; // 小球半径
        matrices.pushPose();
        matrices.translate(hitPos.x, hitPos.y, hitPos.z);
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, 8, 8);
        matrices.popPose();
    }

    public static void renderCreeperBoomRange(Minecraft client,
                                              ClientLevel world,
                                              Creeper creeper,
                                              PoseStack matrices,
                                              float tickDelta) {
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.position();
        float radius = ((CreeperEntityAccessor)creeper).getExplosionRadius() * (creeper.isPowered() ? 2f : 1f);

        // 颜色渐变逻辑：正在蓄力则红色，否则灰色
        float fuseProgress = creeper.getSwelling(tickDelta); // 0~1
        float r = 0.5f + (1f - 0.5f) * fuseProgress; // 0.5 -> 1
        float g = 0.5f * (1f - fuseProgress);         // 0.5 -> 0
        float b = 0.5f * (1f - fuseProgress);         // 0.5 -> 0
        float a = 0.5f;                               // 半透明
        float[] color = new float[]{r, g, b, a};

        Vec3 pos = creeper.position();

        matrices.pushPose();
        matrices.translate(
                pos.x - cameraPos.x,
                pos.y - cameraPos.y,
                pos.z - cameraPos.z
        );

        int latDivisions = 16;
        int lonDivisions = 16;
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, latDivisions, lonDivisions);
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius*2, color, latDivisions, lonDivisions);
        matrices.popPose();
    }


    public static void renderSlimeJumping(Minecraft client,
                                          ClientLevel world,
                                          Slime slime,
                                          PoseStack matrices,
                                          float tickDelta) {

        float[] color = slime instanceof MagmaCube
                ? new float[]{1.f, 0xa5 / 255.f, 0x50 / 255.f, 1f}
                : new float[]{0f, 1f, 0f, 1f};

        MultiBufferSource.BufferSource provider = client.renderBuffers().bufferSource();
        VertexConsumer consumer = provider.getBuffer(RenderTypes.lines());

        Vec3 startPos = slime.position();
        Vec3 velocity = slime.getDeltaMovement();

        double gravity = 0.08;
        double drag = 0.98;
        int maxTicks = 40; // 2秒预测

        Vec3 pos = startPos;
        Vec3 vel = velocity;
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.position();

        List<Vec3> trajectory = new ArrayList<>();
        trajectory.add(pos);

        for (int i = 0; i < maxTicks; i++) {
            // 预测下一位置
            Vec3 nextPos = pos.add(vel);

            // 检查方块碰撞（近似，只检测垂直）
            BlockPos blockBelow = new BlockPos(
                    (int)Math.floor(nextPos.x),
                    (int)Math.floor(nextPos.y - 0.01),
                    (int)Math.floor(nextPos.z)
            );
            if (!world.isEmptyBlock(blockBelow)) {
                // 落地，停止预测
                nextPos = new Vec3(nextPos.x, Math.ceil(nextPos.y), nextPos.z);
                trajectory.add(nextPos);
                break;
            }

            trajectory.add(nextPos);

            // 更新速度
            vel = new Vec3(vel.x * drag, vel.y - gravity, vel.z * drag);

            pos = nextPos;
        }

        if (trajectory.size() < 2){return;}
        // 渲染轨迹
        for (int i = 0; i < trajectory.size() - 1; i++) {
            Vec3 from = trajectory.get(i).subtract(cameraPos);
            Vec3 to = trajectory.get(i + 1).subtract(cameraPos);
            putLine(consumer, matrices.last().pose(), 0, 0, 0,
                    new Vector3f((float) from.x, (float) from.y, (float) from.z),
                    new Vector3f((float) to.x, (float) to.y, (float) to.z),
                    color);
        }

        // 落点Box
        Vec3 landingPos = trajectory.getLast();
        AABB landingBox = slime.getBoundingBox().move(landingPos.subtract(slime.position()));
        renderBoxLines(landingBox, matrices, consumer, color, cameraPos);

        provider.endLastBatch();
    }

    public static void renderDragonFireballHitRange(Minecraft client,
                                                    ClientLevel world,DragonFireball fireball,
                                                    PoseStack matrices,
                                                    float tickDelta) {
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.position();
        Vec3 hitPos = predictDragonFireballHitPos(fireball.position(), fireball.getDeltaMovement(), world);
        float radius = 3.0f; // 范围半径

        // 开始渲染球
        matrices.pushPose();

        // 平移到落点
        matrices.translate(hitPos.x - cameraPos.x,
                hitPos.y - cameraPos.y,
                hitPos.z - cameraPos.z);
        float[] color = ENDER_DRAGON_PURPLE;

        int latDivisions = 16;  // 纬度分割数
        int lonDivisions = 16;  // 经度分割数
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color,
                latDivisions, lonDivisions);

        matrices.popPose();

        RenderingUtils.renderBeam(matrices,
                fireball.position().subtract(cameraPos),
                hitPos.subtract(cameraPos),
                color,
                beamWidth);
    }

    public static void renderWitherFiringSkullHitRange(Minecraft client,
                                                       ClientLevel world,
                                                       WitherSkull skull,
                                                       PoseStack matrices,
                                                       float tickDelta,
                                                       Vec3 cameraPos) {
        Vec3 startPos = skull.position().subtract(cameraPos);
        Vec3 velocity = skull.getDeltaMovement();

        boolean charged = skull.isDangerous();
        Vec3 hitPos = predictWitherSkullHitPos(skull.position(), velocity, world, charged)
                .subtract(cameraPos);

        float radius = 2.0f;   // WitherSkull 爆炸范围

        // 配色：带Charged的我们做紫色些
        float[] color = charged ?
                new float[]{1f, 0.15f, 0.75f, 0.5f} :
                new float[]{0.8f, 0.3f, 0.3f, 0.45f};

        // 绘制落点球体
        matrices.pushPose();
        matrices.translate(hitPos.x, hitPos.y, hitPos.z);
        RenderingUtils.renderTransparentSphere(
                matrices,
                0, 0, 0,
                radius,
                color,
                16, 16
        );
        RenderingUtils.renderTransparentSphere(
                matrices,
                0, 0, 0,
                radius*2,
                color,
                16, 16
        );
        matrices.popPose();

        // 光束：从 Skulll 当前到落点位置
        RenderingUtils.renderBeam(
                matrices,
                startPos,
                hitPos,
                color,
                0.05f
        );
    }


    public static Vec3 predictWitherSkullHitPos(Vec3 startPos, Vec3 velocity, ClientLevel world, boolean charged) {
        Vec3 pos = startPos;
        Vec3 vel = velocity;
        float drag = charged ? 0.73f : 0.99f;   // WitherSkull 的阻力设定

        for (int i = 0; i < 200; i++) {
            Vec3 nextPos = pos.add(vel);

            // 方块碰撞
            BlockHitResult blockHit = world.clip(new ClipContext(
                    pos, nextPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY,
                    CollisionContext.empty()
            ));

            if (blockHit.getType() != HitResult.Type.MISS) {
                BlockPos bp = blockHit.getBlockPos();
                return new Vec3(
                        bp.getX() + 0.5,
                        bp.getY() + 0.5,
                        bp.getZ() + 0.5
                );
            }

            // 实体检测（可选，凋灵弹接触生物就爆）
            for (Entity e : world.entitiesForRendering()) {
                if (!(e instanceof LivingEntity)) continue;

                Optional<Vec3> hit = e.getBoundingBox().clip(pos, nextPos);
                if (hit.isPresent()) {
                    return hit.get();
                }
            }

            // 更新
            pos = nextPos;
            vel = vel.scale(drag);
        }

        // 没撞到，认为飞到这里结束
        return pos;
    }

    public static void renderCrossbowRocketBoomRange(Minecraft client, PoseStack matrices, float tickDelta) {
        var player = client.player;
        var world = client.level;
        if (player == null || world == null) return;

        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.position();

        List<ItemStack> crossbows = Arrays.asList(player.getMainHandItem(), player.getOffhandItem());

        for (ItemStack stack : crossbows) {
            if (!(stack.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(stack)) continue;

            ChargedProjectiles projectiles = stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);

            // 多重射击检测
            AtomicBoolean multishot = new AtomicBoolean(false);
            stack.getEnchantments().keySet().forEach(enchantmentRegistryEntry -> {
                enchantmentRegistryEntry.unwrapKey().ifPresent(key -> {
                    if (key == Enchantments.MULTISHOT){
                        multishot.set(true);
                    }
                });
            });

            // 多重射击角度偏移
            float[] angles = multishot.get() ? new float[]{0f, 10f, -10f} : new float[]{0f};

            for (ItemStack proj : projectiles.getItems()) {
                if (!proj.is(Items.FIREWORK_ROCKET)) continue;

                var fireworkComp = proj.getComponents().get(DataComponents.FIREWORKS);
                if (fireworkComp == null) continue;

                int flight = fireworkComp.flightDuration();
                List<FireworkExplosion> explosions = fireworkComp.explosions();

                Vec3 startPos = player.getEyePosition();

                float yaw = player.getViewYRot(tickDelta);
                float pitch = player.getViewXRot(tickDelta);
                double yawRad = Math.toRadians(-yaw);
                double pitchRad = Math.toRadians(-pitch);

// 玩家视线方向向量
                Vec3 look = new Vec3(
                        Math.sin(yawRad) * Math.cos(pitchRad),
                        Math.sin(pitchRad),
                        Math.cos(yawRad) * Math.cos(pitchRad)
                ).normalize();

// 本地右向量 (水平)
                Vec3 right = new Vec3(
                        Math.cos(yawRad),
                        0,
                        -Math.sin(yawRad)
                ).normalize();


                for (float angleDeg : angles) {
                    // 将 yaw 偏移应用在玩家本地坐标系
                    double angleRad = Math.toRadians(angleDeg);

                    // 绕 upLocal 旋转 look
                    Vec3 rotated = look.scale(Math.cos(angleRad))
                            .add(right.scale(Math.sin(angleRad)))
                            .normalize();

                    Vec3 velocity = rotated.scale(1.6f);

                    // 飞行距离按 flight 时间乘速度
                    Vec3 endPos = startPos.add(velocity.scale(flight * 10.0));

                    // 射线检测碰撞方块
                    ClipContext ctx = new ClipContext(startPos, endPos,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            player);
                    BlockHitResult result = world.clip(ctx);
                    Vec3 hitPos = result.getType() != HitResult.Type.MISS ? result.getLocation() : endPos;

                    // 渲染每个爆炸效果
                    for (FireworkExplosion explosion : explosions) {
                        float radius = switch (explosion.shape()) {
                            case SMALL_BALL -> 2.0f;
                            case LARGE_BALL -> 3.0f;
                            case STAR -> 3.5f;
                            case CREEPER -> 3.0f;
                            case BURST -> 2.5f;
                        };

                        float[] color = new float[4]; // RGBA
                        color[3] = 0.5f;
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

                        matrices.pushPose();
                        Vec3 renderBeamStart = startPos.subtract(cameraPos);
                        Vec3 renderBeamEnd = hitPos.subtract(cameraPos);
                        Vec3 renderBeamVector = renderBeamEnd.subtract(renderBeamStart);
                        Vec3 renderBeamCut = renderBeamVector.scale(0.2);

                        renderBeamStart = renderBeamStart.add(renderBeamCut);
                        RenderingUtils.renderBeam(
                                matrices,
                                renderBeamStart,
                                renderBeamEnd,
                                color,
                                beamWidth
                        );
                        matrices.popPose();

                        // 渲染爆炸球
                        matrices.pushPose();
                        matrices.translate(hitPos.x - cameraPos.x,
                                hitPos.y - cameraPos.y,
                                hitPos.z - cameraPos.z);
                        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, 32, 32);
                        matrices.popPose();
                    }
                }
            }
        }
    }


    // 简单旋转Y轴
    private static Vec3 rotateVectorYaw(Vec3 vec, float deg) {
        double rad = Math.toRadians(deg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = vec.x * cos - vec.z * sin;
        double z = vec.x * sin + vec.z * cos;
        return new Vec3(x, vec.y, z);
    }


    public static Vec3 predictDragonFireballHitPos(Vec3 startPos, Vec3 velocity, Level world) {
        Vec3 endPos = startPos.add(velocity.scale(200)); // 远距离延长射线
        ClipContext context = new ClipContext(
                startPos,
                endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        );

        BlockHitResult result = world.clip(context);
        if (result.getType() != HitResult.Type.MISS) {
            return result.getLocation();
        }
        else {
            return endPos; // 如果射线没有撞到方块，就延长到最大范围
        }
    }

    public static Vec3 predictFireballHitPos(Vec3 startPos, Vec3 velocity, ClientLevel world) {
        Vec3 pos = startPos;
        Vec3 vel = velocity;

        double maxDistance = 100.0; // 最远预测距离
        double step = 0.1; // 每次移动步长
        double traveled = 0;

        while (traveled < maxDistance) {
            Vec3 nextPos = pos.add(vel.scale(step));

            // 射线检测方块
            var hit = world.clip(new ClipContext(
                    pos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getLocation();
            }

            pos = nextPos;
            traveled += vel.length() * step;

            // 可选：考虑引力/阻力等
            // vel = vel.add(0, -0.03, 0); // 如果模拟小火球下坠
        }

        // 没有撞击，返回终点
        return pos;
    }
}
