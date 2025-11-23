package com.linearity.feedhelper.client.utils;

import com.linearity.feedhelper.client.mixin.CreeperEntityAccessor;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.linearity.feedhelper.client.utils.ProjectilePredictionRenderer.*;
import static com.linearity.feedhelper.client.utils.RenderingUtils.*;

public class RangingSystemRelated {

    public static final float beamWidth = 0.2f;
    public static void rangingSystemLoopRendering(MinecraftClient client,MatrixStack matrices,float tickDelta) {
        renderForPredictEntities(client,matrices,tickDelta);
    }
    public static void rangingSystemLoopRenderingOnHUD(MinecraftClient client, DrawContext context, float tickDelta) {

        renderArrowHitTargetIfExists(client,context,tickDelta);

    }

    public static void renderArrowHitTargetIfExists(MinecraftClient client, DrawContext context, float tickDelta) {
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) return;

        List<Vec3d> arrowPositions = new ArrayList<>();
        List<Vec3d> velocityVectors = new ArrayList<>();

        // 玩家拉弓中
        if (player.isUsingItem() && (player.getActiveItem().getItem() instanceof BowItem)) {
            int useTicks = player.getItemUseTime();
            float f = useTicks / 20.0f;
            float velocity = (f * f + f * 2.0f) / 3.0f;
            velocity = Math.min(velocity, 1.0f);

            Vec3d startPos = player.getEyePos();
            Vec3d velocityVec = player.getRotationVec(tickDelta).multiply(velocity * 3.0);

            arrowPositions.add(startPos);
            velocityVectors.add(velocityVec);
        }

        // 检查主手和副手的弩
        List<ItemStack> crossbows = Arrays.asList(player.getMainHandStack(), player.getOffHandStack());
        for (ItemStack stack : crossbows) {
            if (!(stack.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(stack)) continue;

            ChargedProjectilesComponent projectiles = stack.getOrDefault(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
            AtomicBoolean hasRocketFlag = new AtomicBoolean(false);
            for (ItemStack proj : projectiles.getProjectiles()) {
                if (proj.isOf(Items.FIREWORK_ROCKET)) {
                    hasRocketFlag.set(true);
                    break;
                }
            }
            if (hasRocketFlag.get()){
                continue;
            }
            AtomicBoolean hasMultishot = new AtomicBoolean(false);
            stack.getEnchantments().getEnchantments().forEach(e -> e.getKey().ifPresent(key -> {
                if (key == Enchantments.MULTISHOT) hasMultishot.set(true);
            }));

            // 偏移角度
            float[] angles = hasMultishot.get() ? new float[]{0f, 10f, -10f} : new float[]{0f};
            float speed = 3.15f;

            for (float angle : angles) {
                Vec3d startPos = player.getEyePos();
                // 基于玩家旋转和角度偏移
                float yaw = player.getYaw(tickDelta);
                float pitch = player.getPitch(tickDelta);
                double yawRad = Math.toRadians(-yaw + angle); // yaw 偏移
                double pitchRad = Math.toRadians(-pitch);

                Vec3d vel = new Vec3d(
                        Math.sin(yawRad) * Math.cos(pitchRad),
                        Math.sin(pitchRad),
                        Math.cos(yawRad) * Math.cos(pitchRad)
                ).normalize().multiply(speed);

                arrowPositions.add(startPos);
                velocityVectors.add(vel);
            }
        }

        if (arrowPositions.isEmpty()) return;

        // 模拟每条箭的轨迹
        for (int idx = 0; idx < arrowPositions.size(); idx++) {
            Vec3d arrowPos = arrowPositions.get(idx);
            Vec3d velocityVec = velocityVectors.get(idx);

            Entity targetEntity = null;
            BlockHitResult targetBlock = null;
            Vec3d hitPos = null;
            float tickStep = 0f;

            // 获取附近实体列表
            List<Entity> entities = new LinkedList<>();
            world.getEntities().forEach(entity -> {
                if (entity != player && entity.isAlive() && !entity.isSpectator()) entities.add(entity);
            });

            // 模拟箭矢轨迹
            for (int i = 0; i < 200; i++) {
                Vec3d nextPos = arrowPos.add(velocityVec);

                // 方块碰撞
                BlockHitResult blockHit = world.raycast(new RaycastContext(
                        arrowPos, nextPos, RaycastContext.ShapeType.OUTLINE,
                        RaycastContext.FluidHandling.ANY, player
                ));
                if (blockHit.getType() != HitResult.Type.MISS) {
                    targetBlock = blockHit;
                    hitPos = new Vec3d(
                            blockHit.getBlockPos().getX() + 0.5,
                            blockHit.getBlockPos().getY() + 0.5,
                            blockHit.getBlockPos().getZ() + 0.5
                    );
                    break;
                }

                // 实体碰撞
                for (Entity entity : entities) {
                    Vec3d predictedPos = entity.getEntityPos().add(entity.getVelocity().multiply(tickStep));
                    Box predictedBox = entity.getBoundingBox().offset(predictedPos.subtract(entity.getEntityPos()));
                    Optional<Vec3d> intersect = predictedBox.raycast(arrowPos, nextPos);
                    if (intersect.isPresent()) {
                        targetEntity = entity;
                        hitPos = intersect.get();
                        break;
                    }
                }
                if (hitPos != null) break;

                // 更新箭矢位置和速度
                arrowPos = nextPos;
                velocityVec = velocityVec.multiply(0.99).add(0, -0.05, 0);
                tickStep += 1f;
            }

            if (hitPos == null) continue;

            Vec3d screen = RenderingUtils.worldToScreen(client, hitPos);
            if (screen == null) continue;

            int x = (int) screen.x;
            int y = (int) screen.y;
            RenderingUtils.drawRect(context, x - RenderingUtils.rectSize, y - RenderingUtils.rectSize, x + RenderingUtils.rectSize, y + RenderingUtils.rectSize, 0xFF39C5BB);

            String name = targetEntity != null ? targetEntity.getName().getString()
                    : world.getBlockState(targetBlock.getBlockPos()).getBlock().getName().getString();
            if (name == null) continue;

            String printText = name + " " + String.format("x=%.3f,y=%.3f,z=%.3f", hitPos.getX(), hitPos.getY(), hitPos.getZ());
            context.drawText(client.textRenderer, printText, x - RenderingUtils.rectSize, y + RenderingUtils.rectSize, 0xFF39C5BB, false);
        }
    }


    public static void renderForPredictEntities(MinecraftClient client, MatrixStack matrices, float tickDelta) {
        var world = client.world;
        if (world == null) return;

        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;

        var cameraPos = camera.getPos();

        for (Entity entity : world.getEntities()) {

            // Dragon fireball
            if (entity instanceof DragonFireballEntity fireball) {
                renderDragonFireballHitRange(client,world,fireball, matrices, tickDelta);
                continue;
            }

            // Wither skull
            if (entity instanceof WitherSkullEntity skull) {
                renderWitherFiringSkullHitRange(client,world,skull, matrices, tickDelta, cameraPos);
                continue;
            }
            if (entity instanceof SlimeEntity slime){
                renderSlimeJumping(client,world,slime, matrices, tickDelta);
                continue;
            }
            if (entity instanceof CreeperEntity creeper) {
                renderCreeperBoomRange(client,world,creeper, matrices, tickDelta);
                continue;
            }
            if (entity instanceof SmallFireballEntity smallFireball){
                renderSmallFireballHitLocation(client,world,smallFireball, matrices, tickDelta);
                continue;
            }
            if (entity instanceof ArrowEntity arrow){
                renderArrowPredictedPath(client, world, arrow, matrices, tickDelta);
                continue;
            }
            if (entity instanceof PlayerEntity player){
                renderPlayerWeaponPrediction(client, world,player, matrices, tickDelta);
                continue;
            }
        }
    }
    public static void renderSmallFireballHitLocation(MinecraftClient client,
                                                      ClientWorld world,
                                                      SmallFireballEntity fireball,
                                                      MatrixStack matrices,
                                                      float tickDelta) {
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.getPos();

        // 预测火球落点
        Vec3d hitPos = predictFireballHitPos(fireball.getEntityPos(), fireball.getVelocity(), world);

        // 火球影响范围半径（小火球一般较小）
        float radius = 1.5f;

        // 保存矩阵状态
        matrices.push();

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

        matrices.pop();

        // 渲染光束，从火球当前位置指向落点
        Vec3d start = fireball.getEntityPos().subtract(cameraPos);
        Vec3d end = hitPos.subtract(cameraPos);
        float beamWidth = 0.05f; // 光束宽度
        RenderingUtils.renderBeam(matrices, start, end, color, beamWidth);
    }

    public static List<Vec3d> predictArrowPath(Vec3d startPos, Vec3d velocity, ClientWorld world, int maxTicks) {
        List<Vec3d> path = new ArrayList<>();
        Vec3d pos = startPos;
        Vec3d vel = velocity;

        float gravity = 0.05f;
        float dragAir = 0.99f;
        float dragWater = 0.6f;

        for (int tick = 0; tick < maxTicks; tick++) {
            path.add(pos);

            Vec3d nextPos = pos.add(vel);

            // 射线检测方块/实体
            var hit = world.raycast(new RaycastContext(
                    pos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.SOURCE_ONLY, // 可以检测水
                    ShapeContext.absent()
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                path.add(hit.getPos());
                break;
            }

            // 更新位置
            pos = nextPos;

            // 判断是否在水中减速
            if (world.getFluidState(BlockPos.ofFloored(pos)).isIn(FluidTags.WATER)) {
                vel = vel.multiply(dragWater);
            } else {
                vel = vel.multiply(dragAir);
            }

            // 受重力影响
            vel = vel.add(0, -gravity, 0);

            // 速度过小提前停止
            if (vel.lengthSquared() < 1e-6) break;
        }

        return path;
    }

    public static void renderPlayerWeaponPrediction(MinecraftClient client,
                                                    ClientWorld world,
                                                    PlayerEntity player,
                                                    MatrixStack matrices,
                                                    float tickDelta) {
        if (player == null || world == null) return;

        var camera = MinecraftClient.getInstance().getEntityRenderDispatcher().camera;
        if (camera == null) return;
        Vec3d cameraPos = camera.getPos();

        // 遍历主手和副手
        List<ItemStack> hands = Arrays.asList(player.getMainHandStack(), player.getOffHandStack());
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

    private static void renderFireworkPrediction(PlayerEntity player,
                                                 ItemStack rocket,
                                                 ClientWorld world,
                                                 MatrixStack matrices,
                                                 Vec3d cameraPos,
                                                 float[] angles) {
        var fireworkComp = rocket.getComponents().get(DataComponentTypes.FIREWORKS);
        if (fireworkComp == null) return;

        int flight = fireworkComp.flightDuration();
        List<FireworkExplosionComponent> explosions = fireworkComp.explosions();

        Vec3d startPos = player.getEyePos();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);

        Vec3d look = new Vec3d(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        Vec3d right = new Vec3d(Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();

        for (float angleDeg : angles) {
            double angleRad = Math.toRadians(angleDeg);
            Vec3d rotated = look.multiply(Math.cos(angleRad))
                    .add(right.multiply(Math.sin(angleRad))).normalize();

            Vec3d velocity = rotated.multiply(1.6f);
            Vec3d endPos = startPos.add(velocity.multiply(flight * 10.0));

            // 射线检测碰撞方块
            RaycastContext ctx = new RaycastContext(startPos, endPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player);
            BlockHitResult result = world.raycast(ctx);
            Vec3d hitPos = result.getType() != HitResult.Type.MISS ? result.getPos() : endPos;

            // 渲染每个爆炸效果
            for (FireworkExplosionComponent explosion : explosions) {
                float radius = switch (explosion.shape()) {
                    case SMALL_BALL -> 2.0f;
                    case LARGE_BALL -> 3.0f;
                    case STAR -> 3.5f;
                    case CREEPER -> 3.0f;
                    case BURST -> 2.5f;
                };
                float[] color = new float[]{1f, 0.9f, 0.1f, 0.5f};
                matrices.push();
                matrices.translate(hitPos.x - cameraPos.x, hitPos.y - cameraPos.y, hitPos.z - cameraPos.z);
                RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, 32, 32);
                matrices.pop();
            }
        }
    }

    private static void renderArrowPrediction(PlayerEntity player,
                                              ItemStack arrow,
                                              ClientWorld world,
                                              MatrixStack matrices,
                                              Vec3d cameraPos,
                                              float[] angles) {

        Vec3d startPos = player.getEyePos();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);

        Vec3d look = new Vec3d(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        Vec3d right = new Vec3d(Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();

        for (float angleDeg : angles) {
            double angleRad = Math.toRadians(angleDeg);
            Vec3d rotated = look.multiply(Math.cos(angleRad))
                    .add(right.multiply(Math.sin(angleRad))).normalize();

            double speed = 3.0; // 初速度
            Vec3d velocity = rotated.multiply(speed);

            List<Vec3d> path = predictProjectilePath(startPos, velocity, world, 50,true,true); // 50 tick 外推

            renderProjectilePath(matrices, path, cameraPos, new float[]{1f, 0.9f, 0.1f, 0.5f}, 0.03f, 0.2f);
        }
    }

    private static void renderProjectilePath(MatrixStack matrices,
                                             List<Vec3d> path,
                                             Vec3d cameraPos,
                                             float[] color,
                                             float beamWidth,
                                             float hitRadius) {
        if (path.isEmpty()) return;

        matrices.push();
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3d start = path.get(i).subtract(cameraPos);
            Vec3d end = path.get(i + 1).subtract(cameraPos);
            RenderingUtils.renderBeam(matrices, start, end, color, beamWidth);
        }

        Vec3d hitPos = path.get(path.size() - 1).subtract(cameraPos);
        matrices.push();
        matrices.translate(hitPos.x, hitPos.y, hitPos.z);
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, hitRadius, color, 32, 32);
        matrices.pop();
        matrices.pop();
    }



    public static List<Vec3d> predictPlayerArrowPath(PlayerEntity player, int maxTicks, ClientWorld world) {
        List<Vec3d> path = new ArrayList<>();

        // 玩家手持物
        ItemStack stack = player.getActiveItem();
        if (!(stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem)) return path;

        // 拉弓进度
        float charge = 1f; // 默认为满拉弓，或者根据 getItemUseTime()/maxUseTime() 计算
        if (stack.getItem() instanceof BowItem) {
            int useTicks = player.getItemUseTime();
            int maxUse = stack.getMaxUseTime(player);
            charge = Math.min(useTicks / 20f, 1f);
        }

        // 初速度
        double maxSpeed = 3.0; // Minecraft 默认箭初速度
        Vec3d velocity = player.getRotationVector().multiply(charge * maxSpeed);

        // 初始位置
        Vec3d pos = player.getEyePos();

        float gravity = 0.05f;
        float dragAir = 0.99f;
        float dragWater = 0.6f;

        for (int tick = 0; tick < maxTicks; tick++) {
            path.add(pos);

            Vec3d nextPos = pos.add(velocity);

            // 射线检测方块
            var hit = world.raycast(new RaycastContext(
                    pos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.SOURCE_ONLY,
                    ShapeContext.absent()
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                path.add(hit.getPos());
                break;
            }

            pos = nextPos;

            if (world.getFluidState(BlockPos.ofFloored(pos)).isIn(FluidTags.WATER)) {
                velocity = velocity.multiply(dragWater);
            } else {
                velocity = velocity.multiply(dragAir);
            }

            velocity = velocity.add(0, -gravity, 0);

            if (velocity.lengthSquared() < 1e-6) break;
        }

        return path;
    }

    public static void renderArrowPredictedPath(MinecraftClient client,
                                                ClientWorld world,
                                                ArrowEntity arrow,
                                                MatrixStack matrices,
                                                float tickDelta) {
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.getPos();

        if (arrow.getVelocity().lengthSquared() < 1e-6) return;

        // 按 tick 预测箭的路径，最多 50 tick（可调）
        List<Vec3d> path = predictArrowPath(arrow.getEntityPos(), arrow.getVelocity(), world, 50);

        if (path.isEmpty()) return;

        // 颜色和宽度
        float[] color = new float[]{1f, 1f, 1f, 0.5f}; // 半透明黄色
        float beamWidth = .1f;

        // 遍历路径渲染每一段光束
        matrices.push();
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3d start = path.get(i).subtract(cameraPos);
            Vec3d end = path.get(i + 1).subtract(cameraPos);
            RenderingUtils.renderBeam(matrices, start, end, color, beamWidth);
        }
        matrices.pop();

        // 可选：在终点渲染小球表示撞击点
        Vec3d hitPos = path.getLast().subtract(cameraPos);
        float radius = 0.5f; // 小球半径
        matrices.push();
        matrices.translate(hitPos.x, hitPos.y, hitPos.z);
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, 8, 8);
        matrices.pop();
    }

    public static void renderCreeperBoomRange(MinecraftClient client,
                                              ClientWorld world,
                                              CreeperEntity creeper,
                                              MatrixStack matrices,
                                              float tickDelta) {
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.getPos();
        float radius = ((CreeperEntityAccessor)creeper).getExplosionRadius() * (creeper.isCharged() ? 2f : 1f);

        // 颜色渐变逻辑：正在蓄力则红色，否则灰色
        float fuseProgress = creeper.getLerpedFuseTime(tickDelta); // 0~1
        float r = 0.5f + (1f - 0.5f) * fuseProgress; // 0.5 -> 1
        float g = 0.5f * (1f - fuseProgress);         // 0.5 -> 0
        float b = 0.5f * (1f - fuseProgress);         // 0.5 -> 0
        float a = 0.5f;                               // 半透明
        float[] color = new float[]{r, g, b, a};

        Vec3d pos = creeper.getEntityPos();

        matrices.push();
        matrices.translate(
                pos.x - cameraPos.x,
                pos.y - cameraPos.y,
                pos.z - cameraPos.z
        );

        int latDivisions = 16;
        int lonDivisions = 16;
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, latDivisions, lonDivisions);
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius*2, color, latDivisions, lonDivisions);
        matrices.pop();
    }


    public static void renderSlimeJumping(MinecraftClient client,
                                          ClientWorld world,
                                          SlimeEntity slime,
                                          MatrixStack matrices,
                                          float tickDelta) {

        float[] color = slime instanceof MagmaCubeEntity
                ? new float[]{1.f, 0xa5 / 255.f, 0x50 / 255.f, 1f}
                : new float[]{0f, 1f, 0f, 1f};

        VertexConsumerProvider.Immediate provider = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer consumer = provider.getBuffer(RenderLayer.getLines());

        Vec3d startPos = slime.getEntityPos();
        Vec3d velocity = slime.getVelocity();

        double gravity = 0.08;
        double drag = 0.98;
        int maxTicks = 40; // 2秒预测

        Vec3d pos = startPos;
        Vec3d vel = velocity;
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.getPos();

        List<Vec3d> trajectory = new ArrayList<>();
        trajectory.add(pos);

        for (int i = 0; i < maxTicks; i++) {
            // 预测下一位置
            Vec3d nextPos = pos.add(vel);

            // 检查方块碰撞（近似，只检测垂直）
            BlockPos blockBelow = new BlockPos(
                    (int)Math.floor(nextPos.x),
                    (int)Math.floor(nextPos.y - 0.01),
                    (int)Math.floor(nextPos.z)
            );
            if (!world.isAir(blockBelow)) {
                // 落地，停止预测
                nextPos = new Vec3d(nextPos.x, Math.ceil(nextPos.y), nextPos.z);
                trajectory.add(nextPos);
                break;
            }

            trajectory.add(nextPos);

            // 更新速度
            vel = new Vec3d(vel.x * drag, vel.y - gravity, vel.z * drag);

            pos = nextPos;
        }

        if (trajectory.size() < 2){return;}
        // 渲染轨迹
        for (int i = 0; i < trajectory.size() - 1; i++) {
            Vec3d from = trajectory.get(i).subtract(cameraPos);
            Vec3d to = trajectory.get(i + 1).subtract(cameraPos);
            putLine(consumer, matrices.peek().getPositionMatrix(), 0, 0, 0,
                    new Vector3f((float) from.x, (float) from.y, (float) from.z),
                    new Vector3f((float) to.x, (float) to.y, (float) to.z),
                    color);
        }

        // 落点Box
        Vec3d landingPos = trajectory.getLast();
        Box landingBox = slime.getBoundingBox().offset(landingPos.subtract(slime.getEntityPos()));
        renderBoxLines(landingBox, matrices, consumer, color, cameraPos);

        provider.drawCurrentLayer();
    }

    public static void renderDragonFireballHitRange(MinecraftClient client,
                                                    ClientWorld world,DragonFireballEntity fireball,
                                                    MatrixStack matrices,
                                                    float tickDelta) {
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.getPos();
        Vec3d hitPos = predictDragonFireballHitPos(fireball.getEntityPos(), fireball.getVelocity(), world);
        float radius = 3.0f; // 范围半径

        // 开始渲染球
        matrices.push();

        // 平移到落点
        matrices.translate(hitPos.x - cameraPos.x,
                hitPos.y - cameraPos.y,
                hitPos.z - cameraPos.z);
        float[] color = ENDER_DRAGON_PURPLE;

        int latDivisions = 16;  // 纬度分割数
        int lonDivisions = 16;  // 经度分割数
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color,
                latDivisions, lonDivisions);

        matrices.pop();

        RenderingUtils.renderBeam(matrices,
                fireball.getEntityPos().subtract(cameraPos),
                hitPos.subtract(cameraPos),
                color,
                beamWidth);
    }

    public static void renderWitherFiringSkullHitRange(MinecraftClient client,
                                                       ClientWorld world,
                                                       WitherSkullEntity skull,
                                                       MatrixStack matrices,
                                                       float tickDelta,
                                                       Vec3d cameraPos) {
        Vec3d startPos = skull.getEntityPos().subtract(cameraPos);
        Vec3d velocity = skull.getVelocity();

        boolean charged = skull.isCharged();
        Vec3d hitPos = predictWitherSkullHitPos(skull.getEntityPos(), velocity, world, charged)
                .subtract(cameraPos);

        float radius = 2.0f;   // WitherSkull 爆炸范围

        // 配色：带Charged的我们做紫色些
        float[] color = charged ?
                new float[]{1f, 0.15f, 0.75f, 0.5f} :
                new float[]{0.8f, 0.3f, 0.3f, 0.45f};

        // 绘制落点球体
        matrices.push();
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
        matrices.pop();

        // 光束：从 Skulll 当前到落点位置
        RenderingUtils.renderBeam(
                matrices,
                startPos,
                hitPos,
                color,
                0.05f
        );
    }


    public static Vec3d predictWitherSkullHitPos(Vec3d startPos, Vec3d velocity, ClientWorld world, boolean charged) {
        Vec3d pos = startPos;
        Vec3d vel = velocity;
        float drag = charged ? 0.73f : 0.99f;   // WitherSkull 的阻力设定

        for (int i = 0; i < 200; i++) {
            Vec3d nextPos = pos.add(vel);

            // 方块碰撞
            BlockHitResult blockHit = world.raycast(new RaycastContext(
                    pos, nextPos,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.ANY,
                    ShapeContext.absent()
            ));

            if (blockHit.getType() != HitResult.Type.MISS) {
                BlockPos bp = blockHit.getBlockPos();
                return new Vec3d(
                        bp.getX() + 0.5,
                        bp.getY() + 0.5,
                        bp.getZ() + 0.5
                );
            }

            // 实体检测（可选，凋灵弹接触生物就爆）
            for (Entity e : world.getEntities()) {
                if (!(e instanceof LivingEntity)) continue;

                Optional<Vec3d> hit = e.getBoundingBox().raycast(pos, nextPos);
                if (hit.isPresent()) {
                    return hit.get();
                }
            }

            // 更新
            pos = nextPos;
            vel = vel.multiply(drag);
        }

        // 没撞到，认为飞到这里结束
        return pos;
    }

    public static void renderCrossbowRocketBoomRange(MinecraftClient client, MatrixStack matrices, float tickDelta) {
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) return;

        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.getPos();

        List<ItemStack> crossbows = Arrays.asList(player.getMainHandStack(), player.getOffHandStack());

        for (ItemStack stack : crossbows) {
            if (!(stack.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(stack)) continue;

            ChargedProjectilesComponent projectiles = stack.getOrDefault(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);

            // 多重射击检测
            AtomicBoolean multishot = new AtomicBoolean(false);
            stack.getEnchantments().getEnchantments().forEach(enchantmentRegistryEntry -> {
                enchantmentRegistryEntry.getKey().ifPresent(key -> {
                    if (key == Enchantments.MULTISHOT){
                        multishot.set(true);
                    }
                });
            });

            // 多重射击角度偏移
            float[] angles = multishot.get() ? new float[]{0f, 10f, -10f} : new float[]{0f};

            for (ItemStack proj : projectiles.getProjectiles()) {
                if (!proj.isOf(Items.FIREWORK_ROCKET)) continue;

                var fireworkComp = proj.getComponents().get(DataComponentTypes.FIREWORKS);
                if (fireworkComp == null) continue;

                int flight = fireworkComp.flightDuration();
                List<FireworkExplosionComponent> explosions = fireworkComp.explosions();

                Vec3d startPos = player.getEyePos();

                float yaw = player.getYaw(tickDelta);
                float pitch = player.getPitch(tickDelta);
                double yawRad = Math.toRadians(-yaw);
                double pitchRad = Math.toRadians(-pitch);

// 玩家视线方向向量
                Vec3d look = new Vec3d(
                        Math.sin(yawRad) * Math.cos(pitchRad),
                        Math.sin(pitchRad),
                        Math.cos(yawRad) * Math.cos(pitchRad)
                ).normalize();

// 本地右向量 (水平)
                Vec3d right = new Vec3d(
                        Math.cos(yawRad),
                        0,
                        -Math.sin(yawRad)
                ).normalize();


                for (float angleDeg : angles) {
                    // 将 yaw 偏移应用在玩家本地坐标系
                    double angleRad = Math.toRadians(angleDeg);

                    // 绕 upLocal 旋转 look
                    Vec3d rotated = look.multiply(Math.cos(angleRad))
                            .add(right.multiply(Math.sin(angleRad)))
                            .normalize();

                    Vec3d velocity = rotated.multiply(1.6f);

                    // 飞行距离按 flight 时间乘速度
                    Vec3d endPos = startPos.add(velocity.multiply(flight * 10.0));

                    // 射线检测碰撞方块
                    RaycastContext ctx = new RaycastContext(startPos, endPos,
                            RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE,
                            player);
                    BlockHitResult result = world.raycast(ctx);
                    Vec3d hitPos = result.getType() != HitResult.Type.MISS ? result.getPos() : endPos;

                    // 渲染每个爆炸效果
                    for (FireworkExplosionComponent explosion : explosions) {
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

                        matrices.push();
                        Vec3d renderBeamStart = startPos.subtract(cameraPos);
                        Vec3d renderBeamEnd = hitPos.subtract(cameraPos);
                        Vec3d renderBeamVector = renderBeamEnd.subtract(renderBeamStart);
                        Vec3d renderBeamCut = renderBeamVector.multiply(0.2);

                        renderBeamStart = renderBeamStart.add(renderBeamCut);
                        RenderingUtils.renderBeam(
                                matrices,
                                renderBeamStart,
                                renderBeamEnd,
                                color,
                                beamWidth
                        );
                        matrices.pop();

                        // 渲染爆炸球
                        matrices.push();
                        matrices.translate(hitPos.x - cameraPos.x,
                                hitPos.y - cameraPos.y,
                                hitPos.z - cameraPos.z);
                        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, 32, 32);
                        matrices.pop();
                    }
                }
            }
        }
    }


    // 简单旋转Y轴
    private static Vec3d rotateVectorYaw(Vec3d vec, float deg) {
        double rad = Math.toRadians(deg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = vec.x * cos - vec.z * sin;
        double z = vec.x * sin + vec.z * cos;
        return new Vec3d(x, vec.y, z);
    }


    public static Vec3d predictDragonFireballHitPos(Vec3d startPos, Vec3d velocity, World world) {
        Vec3d endPos = startPos.add(velocity.multiply(200)); // 远距离延长射线
        RaycastContext context = new RaycastContext(
                startPos,
                endPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                ShapeContext.absent()
        );

        BlockHitResult result = world.raycast(context);
        if (result.getType() != HitResult.Type.MISS) {
            return result.getPos();
        }
        else {
            return endPos; // 如果射线没有撞到方块，就延长到最大范围
        }
    }

    public static Vec3d predictFireballHitPos(Vec3d startPos, Vec3d velocity, ClientWorld world) {
        Vec3d pos = startPos;
        Vec3d vel = velocity;

        double maxDistance = 100.0; // 最远预测距离
        double step = 0.1; // 每次移动步长
        double traveled = 0;

        while (traveled < maxDistance) {
            Vec3d nextPos = pos.add(vel.multiply(step));

            // 射线检测方块
            var hit = world.raycast(new RaycastContext(
                    pos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    ShapeContext.absent()
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getPos();
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
