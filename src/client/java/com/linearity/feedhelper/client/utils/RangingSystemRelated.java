package com.linearity.feedhelper.client.utils;

import com.linearity.feedhelper.client.mixin.AbstractArrowAccessor;
import com.linearity.feedhelper.client.mixin.CreeperEntityAccessor;
import com.linearity.feedhelper.client.utils.projectiletrace.IProjectileSimulator;
import com.linearity.feedhelper.client.utils.projectiletrace.ProjectileSimulateContext;
import com.linearity.feedhelper.client.utils.projectiletrace.ProjectileState;
import com.linearity.feedhelper.client.utils.projectiletrace.additionalinfo.FireworkRocketAdditionalInfo;
import com.linearity.feedhelper.client.utils.projectiletrace.impl.AbstractArrowSimulator;
import com.linearity.feedhelper.client.utils.projectiletrace.impl.FireworkSimulator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;

import static com.linearity.feedhelper.client.utils.ProjectilePredictionRenderer.*;
import static com.linearity.feedhelper.client.utils.RenderingUtils.*;
import static com.linearity.feedhelper.client.utils.projectiletrace.impl.AbstractArrowSimulator.arrowInertiaAtPosFunc;
import static com.linearity.feedhelper.config.Configs.Generic.PREDICATE_PROJECTILE_TICKS;
import static com.linearity.feedhelper.config.RangingSystemFeatureToggle.SHOW_CREEPER_RANGE;

public class RangingSystemRelated {

    public static final float beamWidth = 0.2f;
    public static void rangingSystemLoopRendering(Minecraft client,PoseStack matrices,float tickDelta) {
        renderForPredictEntities(client,matrices,tickDelta);
    }

    public static void renderForPredictEntities(Minecraft client, PoseStack matrices, float tickDelta) {
        var world = client.level;
        if (world == null) return;

        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;

        var cameraPos = camera.position();

        for (Entity entity : world.entitiesForRendering()) {
            if (entity instanceof DragonFireball fireball) {
                renderDragonFireballHitRange(client,world,fireball, matrices, tickDelta);
                continue;
            }
            if (entity instanceof WitherSkull skull) {
                renderWitherFiringSkullHitRange(client,world,skull, matrices, tickDelta, cameraPos);
                continue;
            }
            if (entity instanceof Slime slime){
                renderSlimeJumping(client,world,slime, matrices, tickDelta);
                continue;
            }
            if (entity instanceof Creeper creeper) {
                renderCreeperBoomRange(client, creeper, matrices, tickDelta);
                continue;
            }
            if (entity instanceof SmallFireball smallFireball){
                renderSmallFireballHitLocation(client,world,smallFireball, matrices);
                continue;
            }
            if (entity instanceof AbstractArrow arrow){
                renderArrowPredictedPath(client, arrow, matrices);
                continue;
            }
            if (entity instanceof FireworkRocketEntity rocket){
                renderFireworkPrediction(
                        world,
                        matrices,
                        cameraPos,
                        rocket.getItem(),
                        rocket.position(),
                        rocket.getDeltaMovement(),
                        FireworkRocketAdditionalInfo.of(rocket)
                );
                continue;
            }
            if (entity instanceof Player player){
                renderPlayerWeaponPrediction(world,player, matrices, tickDelta);
                continue;
            }
        }
    }

    public static void renderSmallFireballHitLocation(Minecraft client,
                                                      ClientLevel world,
                                                      SmallFireball fireball,
                                                      PoseStack matrices
    ) {
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

    public static List<ProjectileState> predictArrowPath(AbstractArrow arrow, int maxTicks) {
        ProjectileState state = new ProjectileState(arrow.position(),arrow.getDeltaMovement(),arrow.isOnFire(),arrow.blockPosition(),0,new HitResult(arrow.position()) {
            @Override
            public @NotNull Type getType() {
                return Type.MISS;
            }
        });
        var accessor = (AbstractArrowAccessor) arrow;
        var context = new ProjectileSimulateContext<>(
                (ClientLevel) arrow.level(),
                accessor.feedhelper$isInGround(),
                arrowInertiaAtPosFunc(arrow.level(),accessor.feedhelper$getWaterInertia(),accessor.feedhelper$getInertia()),
                arrow.isNoPhysics(),
                arrow.getGravity(),
                arrow.getType().getSpawnAABB(0,0,0),
                null
        );
        return predictArrowPath(context, state, maxTicks);
    }

    public static List<ProjectileState> predictFireworkPath(ProjectileSimulateContext<?> context, ProjectileState startState){
        return predictFireworkPath(context,startState,PREDICATE_PROJECTILE_TICKS.getIntegerValue());
    }
    public static List<ProjectileState> predictFireworkPath(ProjectileSimulateContext<?> context, ProjectileState startState, int maxTicks){
        return predictPath(context,startState,maxTicks, FireworkSimulator.INSTANCE);
    }
    public static List<ProjectileState> predictArrowPath(ProjectileSimulateContext<?> context, ProjectileState startState){
        return predictArrowPath(context,startState,PREDICATE_PROJECTILE_TICKS.getIntegerValue());
    }
    public static List<ProjectileState> predictArrowPath(ProjectileSimulateContext<?> context, ProjectileState startState, int maxTicks){
        return predictPath(context,startState,maxTicks,AbstractArrowSimulator.INSTANCE);
    }
    public static List<ProjectileState> predictPath(ProjectileSimulateContext<?> context, ProjectileState startState, int maxTicks, IProjectileSimulator<?> simulator){
        List<ProjectileState> path = new LinkedList<>();
        ProjectileState state = startState;
        for (int tick = 0; tick < maxTicks; tick++) {
            if (state.hitResult().getType() != HitResult.Type.MISS) {
                break;
            }
            var nextState = simulator.simulateNextTick(context, state);
            if (Objects.equals(state,nextState)){
                break;
            }
            path.add(nextState);
            state = nextState;
        }
        return path;
    }

    public static void renderPlayerWeaponPrediction(
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
                renderPlayerBowPrediction(player, stack, world, matrices, cameraPos);
            } else if (stack.getItem() instanceof CrossbowItem) {
                renderPlayerCrossbowPrediction(player, stack, world, matrices, cameraPos,tickDelta);
            } else if (stack.getItem() instanceof TridentItem) {
                renderPlayerTridentPrediction(player, stack, world, matrices, cameraPos);
            }
        }
    }


    public static void renderArrowPredictedPath(
            Minecraft client,
            AbstractArrow arrow,//we resolved trident
            PoseStack matrices
    ) {
        var camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        var cameraPos = camera.position();

        if (arrow.getDeltaMovement().lengthSqr() < 1e-6) return;

        // 按 tick 预测箭的路径，最多 50 tick（可调）
        var path = predictArrowPath(arrow, PREDICATE_PROJECTILE_TICKS.getIntegerValue());
        if (path.size() < 2) return;
        var pathIterator = path.iterator();
        var currentState = pathIterator.next();
        var nextState = currentState;

        // 颜色和宽度
        final float[] color = new float[]{1f, 1f, 1f, 0.5f};
        final float[] firedColor = new float[]{1f, 0x3E/255.f, 0, 0.5f};
        float beamWidth = 0.5f;

        // 遍历路径渲染每一段光束
        matrices.pushPose();
        while (pathIterator.hasNext()){
            nextState = pathIterator.next();
            RenderingUtils.renderBeam(
                    matrices,
                    currentState.pos().subtract(cameraPos),
                    nextState.pos().subtract(cameraPos),
                    nextState.onFire()?firedColor:color,
                    beamWidth);
            currentState = nextState;
        }
        matrices.popPose();

        // 可选：在终点渲染小球表示撞击点
        Vec3 hitPos = currentState.pos().subtract(cameraPos);
        float radius = 0.5f; // 小球半径
        matrices.pushPose();
        matrices.translate(hitPos.x, hitPos.y, hitPos.z);
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, radius, color, 8, 8);
        matrices.popPose();
    }

    public static void renderCreeperBoomRange(Minecraft client,
                                              Creeper creeper,
                                              PoseStack matrices,
                                              float tickDelta) {
        if (!SHOW_CREEPER_RANGE.getBooleanValue()) {
            return;
        }
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

    @Deprecated(forRemoval = true)
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

    @Deprecated(forRemoval = true)
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

    @Deprecated(forRemoval = true)
    public static Vec3 predictFireballHitPos(Vec3 startPos, Vec3 velocity, ClientLevel world) {
        Vec3 pos = startPos;

        double maxDistance = 100.0; // 最远预测距离
        double step = 0.1; // 每次移动步长
        double traveled = 0;

        while (traveled < maxDistance) {
            Vec3 nextPos = pos.add(velocity.scale(step));

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
            traveled += velocity.length() * step;

            // 可选：考虑引力/阻力等
            // vel = vel.add(0, -0.03, 0); // 如果模拟小火球下坠
        }

        // 没有撞击，返回终点
        return pos;
    }
}
