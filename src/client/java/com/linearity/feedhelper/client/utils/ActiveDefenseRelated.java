package com.linearity.feedhelper.client.utils;

import com.google.common.util.concurrent.AtomicDouble;
import com.linearity.feedhelper.client.FeedhelperClient;
import com.linearity.feedhelper.client.mixin.PiglinEntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.skeleton.*;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.linearity.feedhelper.client.utils.ClientShotPredictor.getTick;
import static com.linearity.feedhelper.client.utils.InventoryUtils.equipToHandIf;
import static com.linearity.feedhelper.client.utils.ShotTracker.lastShotTicks;

public class ActiveDefenseRelated {

    public static interface EntityHitPredicationCalculator<E extends Entity> {
        double calculate(@NotNull E entity,@NotNull LocalPlayer player,@NotNull AtomicBoolean hasHostileShooterFlagProvider);
    }
    //shielding related
    public static final AtomicBoolean forcedShield = new AtomicBoolean(false);
    public static AtomicDouble playerYawStored = new AtomicDouble(0);
    public static AtomicDouble playerPitchStored = new AtomicDouble(0);

    public static Map<Class<? extends Entity>, EntityHitPredicationCalculator<? extends Entity>> entityCalculators = new HashMap<>();

    static {
        EntityHitPredicationCalculator<@NotNull AbstractSkeleton> skeletonPredicationCalculator = (skeleton, player, hasHostileShooter) -> {
            if (skeleton.isUsingItem() && skeleton.getUseItem().getItem() == Items.BOW) {
                hasHostileShooter.set(true);
                int ticksUntilShoot = 16 - skeleton.getTicksUsingItem(); // skeleton.getItemUseTime() = 拉弓已经持续的 tick
                Vec3 skeletonPos = skeleton.getEyePosition();
                Vec3 playerPos = player.position();

                Vec3 dir = playerPos.subtract(skeletonPos).normalize();
                Vec3 velocity = dir.scale(1.6); // 初始速度
                double ticksToHit = getTicksToReachPlayerForSkeletonArrowPrediction(skeletonPos, velocity,player);
                if (!Double.isNaN(ticksToHit)){
                    return ticksUntilShoot + ticksToHit;
                }
            }
            return Double.MAX_VALUE;
        };
        entityCalculators.put(AbstractSkeleton.class,skeletonPredicationCalculator);
        entityCalculators.put(Bogged.class,skeletonPredicationCalculator);
        entityCalculators.put(Parched.class,skeletonPredicationCalculator);
        entityCalculators.put(Skeleton.class,skeletonPredicationCalculator);
        entityCalculators.put(WitherSkeleton.class,skeletonPredicationCalculator);

        EntityHitPredicationCalculator<@NotNull Drowned> drownedPredicationCalculator = (drowned,player,hasHostileShooter) ->
        {
            ItemStack mainHand = drowned.getMainHandItem();
            if (mainHand.is(Items.TRIDENT) && drowned.isUsingItem()) {
                hasHostileShooter.set(true);

                // 获取攻击间隔（ProjectileAttackGoal.attackInterval）
                int attackInterval = 40;//drowned.getAttackCooldown(); // 自定义方法获取，默认通常 40
                long lastShot = ShotTracker.getLastShotTick(drowned); // Guava MapTracker
                long ticksUntilShoot;
                if (lastShot >= 0) {
                    ticksUntilShoot = Math.max(attackInterval - (getTick() - lastShot), 0);
                } else {
                    ticksUntilShoot = attackInterval; // 第一次预测，保守估计
                }

                // 起始位置（眼睛/Trident投掷位置）
                Vec3 startPos = drowned.position().add(0, drowned.getBbHeight() * 0.333, 0);
                Vec3 targetPos = player.position().add(0, player.getBbHeight() * 0.5, 0);

                // 方向向量 + 抛物线修正
                Vec3 delta = targetPos.subtract(startPos);
                double horizontalDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                Vec3 dir = new Vec3(delta.x, delta.y + horizontalDist * 0.2, delta.z).normalize();

                // 初速度
                Vec3 velocity = dir.scale(1.6);

                // 预测飞行时间，每tick模拟阻力和重力
                double ticksToHit = getTicksToReachPlayerForDrownedTrident(startPos, velocity, player);

                if (!Double.isNaN(ticksToHit)) {
                    return ticksUntilShoot + ticksToHit;
                }
            }
            return Double.MAX_VALUE;
        };
        entityCalculators.put(Drowned.class,drownedPredicationCalculator);
        EntityHitPredicationCalculator<? extends CrossbowAttackMob> crossbowAttackMobEntityHitPredicationCalculator = (shooter,player,hasHostileShooter) ->
        {
            LivingEntity target = shooter.getTarget();
            if (target != player) return Double.MAX_VALUE;

            // 判断是否手上持交叉弓
            boolean holdingCrossbow = shooter instanceof LivingEntity le &&
                    le.getMainHandItem().getItem() == Items.CROSSBOW;
            if (!holdingCrossbow) return Double.MAX_VALUE;

            // 获取当前 tick（你客户端可能需要维护 tick）
            long clientTick = getTick(); // 或自己维护的 tick
            long lastTick = lastShotTicks.getOrDefault(shooter, new AtomicLong(clientTick)).get();

            // 蓄力判定：CrossbowUser.isCharging()
            boolean charging = shooter instanceof Piglin piglin ? piglin.getEntityData().get(((PiglinEntityAccessor)piglin).getChargingTrackedData())
                    : shooter instanceof Pillager pillager && pillager.isChargingCrossbow();

            if (charging){
                hasHostileShooter.set(true);
            }
            int attackInterval = 20; // 默认估算，如果你能从 Goal 或属性获取，可改成动态
            double ticksUntilShoot = charging ? attackInterval : Math.max(0, attackInterval - (clientTick - lastTick));

            // 投射物初始位置
            LivingEntity le = (LivingEntity) shooter;
            Vec3 startPos = le.getEyePosition();
            Vec3 targetPos = player.position().add(0, player.getBbHeight() * 0.5, 0);
            Vec3 delta = targetPos.subtract(startPos);
            double horizontalDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            Vec3 dir = new Vec3(delta.x, delta.y + horizontalDist * 0.2, delta.z).normalize();
            Vec3 velocity = dir.scale(1.6); // 交叉弓初速，可调

            // 预测命中时间
            double ticksToHit = getTicksToReachPlayerForCrossbow(startPos, velocity, player);

            if (!Double.isNaN(ticksToHit)) {
                return ticksUntilShoot + ticksToHit;
            }

            // 如果当前刚发射或者释放蓄力，则更新 lastShotTick
            if (!charging) {
                lastShotTicks.get(shooter).set(clientTick);
            }
            return Double.MAX_VALUE;
        };
        entityCalculators.put(Piglin.class,crossbowAttackMobEntityHitPredicationCalculator);
        entityCalculators.put(Pillager.class,crossbowAttackMobEntityHitPredicationCalculator);

        EntityHitPredicationCalculator<? extends Blaze> blazeEntityHitPredicationCalculator = (blaze,player,hasHostileShooter) ->
        {

            boolean burning = blaze.isOnFire();
            if (!burning) {
                return Double.MAX_VALUE;
            }

            hasHostileShooter.set(true);
            long clientTick = getTick();
            long lastTick = ShotTracker.getLastShotTick(blaze);

            final int firstFireballDelay = 60;

            long dt = clientTick - lastTick;   // 已蓄力时间
            long ticksUntilShoot;


            ticksUntilShoot = Math.max(0, firstFireballDelay - dt);

            Vec3 startPos = blaze.getEyePosition();
            Vec3 targetPos = player.position().add(0, player.getBbHeight() * 0.6, 0);

            Vec3 delta = targetPos.subtract(startPos);
            Vec3 dir = delta.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : delta.normalize();
            dir = dir.scale(0.9);

            double ticksToHit = getTicksToReachPlayerForBlazeFireballPrediction(startPos, dir, player);

            if (!Double.isNaN(ticksToHit)) {
                return ticksUntilShoot + ticksToHit;
            }
            return Double.MAX_VALUE;
        };
        entityCalculators.put(Blaze.class,blazeEntityHitPredicationCalculator);
        EntityHitPredicationCalculator<? extends AbstractArrow> arrowHitPredicationCalculator =
                (abstractArrow,player,hasHostileShooter) ->
                        getTicksToReachPlayerForSkeletonArrow(abstractArrow,player);
        entityCalculators.put(AbstractArrow.class,arrowHitPredicationCalculator);
        entityCalculators.put(SpectralArrow.class,arrowHitPredicationCalculator);
        entityCalculators.put(ThrownTrident.class,arrowHitPredicationCalculator);//TODO:ThrownTrident
        EntityHitPredicationCalculator<@NotNull SmallFireball> smallFireballEntityHitPredicationCalculator =
                (smallFireball,player,hasHostileShooter) ->
                        getTicksToReachPlayerForBlazeFireball(smallFireball,player);
        entityCalculators.put(SmallFireball.class,smallFireballEntityHitPredicationCalculator);
    }

    public static void activeDefenseLoop(Minecraft client) {

        Level world = client.level;
        LocalPlayer player = client.player;
        MultiPlayerGameMode interactionManager = client.gameMode;
        if (player == null) return;
        if (world == null) return;
        if (interactionManager == null) return;

        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

        int blockRange = (int) Math.floor(reach);
        for (int x=-blockRange; x<=blockRange; x++){
            for (int y=-blockRange; y<=blockRange; y++){
                for (int z=-blockRange; z<=blockRange; z++){
                    BlockPos blockPos = player.blockPosition().offset(x, y, z);
                    if (blockPos.closerToCenterThan(player.position(), reach)) {
                        BlockState state = world.getBlockState(player.blockPosition().offset(x, y, z));
                        if (state.getBlock() instanceof FireBlock) {
                            interactionManager.startDestroyBlock(blockPos,player.getDirection());
                        }
                    }
                }
            }
        }

        reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        AABB box = player.getBoundingBox().inflate(reach);
        var toAttack = world.getEntitiesOfClass(Entity.class, box,
                s -> s instanceof LargeFireball
                        || s instanceof ShulkerBullet
                        || s instanceof WindCharge
        );

        for (var beingAttack : toAttack) {
            interactionManager.attack(player, beingAttack);
        }
    }

    public static void activeDefenseShieldingLoop(Minecraft client) {
        var world = client.level;
        var player = client.player;
        var interactionManager = client.gameMode;
        if (player == null || world == null || interactionManager == null) return;


        boolean hasShield;
        InteractionHand shieldHand = InteractionHand.OFF_HAND;
        if (player.getMainHandItem().getItem() instanceof ShieldItem){
            shieldHand = InteractionHand.MAIN_HAND;
            hasShield = true;
        }else {
            hasShield = player.getOffhandItem().getItem() instanceof ShieldItem;
            if (!hasShield) {
                equipToHandIf((stack -> stack.getItem() instanceof ShieldItem),client,InteractionHand.OFF_HAND);
            }
            hasShield = player.getOffhandItem().getItem() instanceof ShieldItem;
        }
        if (hasShield){

            AABB box = player.getBoundingBox().inflate(30);
            List<Tuple<Double, Entity>> projectilesNearbyCanReachPlayer = new ArrayList<>();
            AtomicReference<Double> minPredicatedTicksRef = new AtomicReference<>(Double.MAX_VALUE);
            AtomicReference<Entity> minPredicatedEntityRef = new AtomicReference<>();
            AtomicBoolean hasHostileShooter = new AtomicBoolean(false);
            //iterate through
            world.getEntitiesOfClass(Entity.class, box,
                    e -> {
                        if (!e.isAlive()){return false;}

                        @SuppressWarnings("unchecked")
                        var calculator = (EntityHitPredicationCalculator<@NotNull Entity>)entityCalculators.get(e.getClass());
                        if (calculator != null) {
                            var predicatedCurrent = calculator.calculate(e,player,hasHostileShooter);
                            if (predicatedCurrent < minPredicatedTicksRef.get()) {
                                minPredicatedTicksRef.set(predicatedCurrent);
                                minPredicatedEntityRef.set(e);
                            }

                        }
                        return false;
                    }
            );

            var minPredicatedEntity = minPredicatedEntityRef.get();
            if (minPredicatedEntity != null) {
                var minPredicatedTicks = minPredicatedTicksRef.get();
                assert minPredicatedTicks != null;
                if (minPredicatedTicks <= 8.){
                    if (!forcedShield.get() && !player.isUsingItem()) {
                        forcedShield.set(true);
                        playerYawStored.set(player.getYRot());
                        playerPitchStored.set(player.getXRot());
                        interactionManager.useItem(player, shieldHand);
                        player.startUsingItem(shieldHand);
                        Minecraft.getInstance().options.keyUse.setDown(true);
                    }
                    faceEntity(player, minPredicatedEntity);
                }
            }
            else if (forcedShield.get()){
                long currentTimemillis = System.currentTimeMillis();
                long considerAt = FeedhelperClient.considerReleaseShieldTimestamp.get();
                if (considerAt == Long.MIN_VALUE){
                    FeedhelperClient.considerReleaseShieldTimestamp.set(currentTimemillis);
                }
                if (currentTimemillis - considerAt > 100 && !hasHostileShooter.get()) {
                    forcedShield.set(false);
                    player.setYRot((float) playerYawStored.get());
                    player.setXRot((float) playerPitchStored.get());
                    interactionManager.releaseUsingItem(player);
                    Minecraft.getInstance().options.keyUse.setDown(false);
                    FeedhelperClient.considerReleaseShieldTimestamp.set(Long.MIN_VALUE);
                }
            }
        }

    }

    public static void faceEntity(LocalPlayer player, Entity target) {
        Vec3 playerPos = player.position().add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3 targetPos = target.position().add(0, target.getBbHeight(), 0);

        Vec3 diff = targetPos.subtract(playerPos);

        double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float yaw = (float)(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(diff.y, distXZ)));

        player.setYRot(yaw);
        player.setXRot(pitch);

        // 同步更新头部方向（不加的话第一人称可能不变化）
        player.setYHeadRot(yaw);
    }

    public static double getTicksToReachPlayerForSkeletonArrow(AbstractArrow arrow,Player player){
        return getTicksToReachPlayerForSkeletonArrow(arrow.position(),arrow.getDeltaMovement(),player);
    }
    public static double getTicksToReachPlayerForSkeletonArrow(Vec3 projectilePos, Vec3 projectileVelocity,Player player) {
        if (player == null) return Double.NaN;

        Vec3 pos = projectilePos;
        Vec3 vel = projectileVelocity;
        if (projectileVelocity.lengthSqr() < 1e-6) {
            return Double.NaN;
        }

        final double drag = 0.99; // PersistentProjectileEntity 默认阻力
        final double gravity = 0.05; // ArrowEntity 默认重力
        final int maxTicks = 200;
        final double hitRadius = 0; // 玩家碰撞箱宽容值，减小 NaN 出现概率

        AABB box = player.getBoundingBox();

        for (int t = 0; t < maxTicks; t++) {
            // 检测是否进入玩家碰撞箱
            if (pos.x >= box.minX - hitRadius && pos.x <= box.maxX + hitRadius &&
                    pos.y >= box.minY - hitRadius && pos.y <= box.maxY + hitRadius &&
                    pos.z >= box.minZ - hitRadius && pos.z <= box.maxZ + hitRadius) {
                return t;
            }

            // 更新速度和位置
            vel = vel.scale(drag).add(0, -gravity, 0); // 先应用阻力，再加重力
            pos = pos.add(vel);
        }

        return Double.NaN; // 超过最大 tick 仍未击中
    }
    public static double getTicksToReachPlayerForSkeletonArrowPrediction(Vec3 projectilePos, Vec3 projectileVelocity, Player player) {
        if (player == null) return Double.NaN;
        if (projectileVelocity.lengthSqr() < 1e-6) return Double.NaN;

        Vec3 target = player.position().add(0, player.getBbHeight() * 0.5, 0);

        Vec3 delta = target.subtract(projectilePos);
        final double drag = 0.99;
        final double gravity = 0.05;

        // 水平距离
        Vec3 deltaXZ = new Vec3(delta.x, 0, delta.z);
        double vXZ = new Vec3(projectileVelocity.x, 0, projectileVelocity.z).length();
        double distanceXZ = deltaXZ.length();

        if (vXZ < 1e-6) return Double.NaN;

        double tXZ = Math.log(1 - (1 - drag) * distanceXZ / vXZ) / Math.log(drag);
        if (Double.isNaN(tXZ) || tXZ < 0) return Double.NaN;

        // Y 方向近似
        double vY0 = projectileVelocity.y;
        double yDistance = delta.y;
        double tY = tXZ; // 先用 tXZ 做初值
        for (int i = 0; i < 3; i++) { // 简单迭代修正 Y
            double traveledY = vY0 * (1 - Math.pow(drag, tY)) / (1 - drag) - gravity * tY * (tY - 1) / 2.0;
            tY = tY * yDistance / traveledY;
            if (Double.isNaN(tY) || tY < 0) return Double.NaN;
        }

        return tY;
    }
    public static double getTicksToReachPlayerForDrownedTrident(Vec3 start, Vec3 velocity, Player player) {
        Vec3 pos = start;
        Vec3 vel = velocity;
        final double drag = 0.99; // 阻力
        final double gravity = 0.05; // 重力，三叉戟略小可调
        int maxTicks = 40; // 最大预测 tick 数

        for (int t = 0; t < maxTicks; t++) {
            pos = pos.add(vel);
            vel = vel.scale(drag).add(0, -gravity, 0);

            // 玩家 bounding box 判断
            Vec3 playerPos = player.position();
            double px = playerPos.x;
            double py = playerPos.y + player.getBbHeight() * 0.5;
            double pz = playerPos.z;
            double radius = 0.5; // 宽容半径

            if (Math.abs(pos.x - px) < radius &&
                    Math.abs(pos.y - py) < radius &&
                    Math.abs(pos.z - pz) < radius) {
                return t;
            }
        }
        return Double.NaN; // 超过最大 tick 未命中
    }
    public static double getTicksToReachPlayerForBlazeFireball(
            SmallFireball fireball, Player player
    ){
        return getTicksToReachPlayerForBlazeFireball(fireball.position(),fireball.getDeltaMovement(),player);
    }
    public static double getTicksToReachPlayerForBlazeFireball(
            Vec3 pos,Vec3 vel, Player player
    ) {

        final double drag = 0.91; // 阻力
        final double gravity = 0.0; // 火球基本不受重力
        final double accelerationPower = 0.1;

        int maxTicks = 200;
        double hitRadius = 1; // 宽容半径

        for (int t = 0; t < maxTicks; t++) {
            // 模拟当前位置与玩家 hitbox 判断
            Vec3 targetPos = player.position().add(0, player.getBbHeight() * 0.6, 0); // 瞄准头部
            if (pos.distanceToSqr(targetPos) <= hitRadius * hitRadius) {
                return t;
            }

            // 按火球逻辑更新速度和位置
            vel = vel.add(vel.normalize().scale(accelerationPower)).scale(drag);
            pos = pos.add(vel);
        }

        return Double.NaN; // 超过最大 tick 仍未击中
    }
    public static double getTicksToReachPlayerForBlazeFireballPrediction(
            Vec3 start, Vec3 velocity, Player player
    ) {
        Vec3 pos = start;
        Vec3 vel = velocity;

        final double drag = 0.91;
        final double gravity = 0.0;
        final double accelerationPower = 0.1; // Blaze 默认加速

        Vec3 targetPos = player.position().add(0, player.getBbHeight() * 0.6, 0);
        double targetDistance = pos.distanceTo(targetPos);

        double traveled = 0.0;
        int maxTicks = 200;

        for (int t = 0; t < maxTicks; t++) {
            // 累加当前 tick 飞行距离
            traveled += vel.length();
            if (traveled >= targetDistance) {
                return t;
            }

            // 按 AbstractFireballEntity 计算下一 tick 速度
            vel = vel.add(vel.normalize().scale(accelerationPower)).scale(drag);
            pos = pos.add(vel);
        }

        return Double.NaN; // 超过最大 tick 仍未到
    }
    public static double getTicksToReachPlayerForCrossbow(Vec3 start, Vec3 velocity, Player player) {
        Vec3 pos = start;
        Vec3 vel = velocity;
        final double drag = 0.99; // 阻力
        final double gravity = 0.05; // 重力，可调
        int maxTicks = 40; // 最大预测 tick 数

        for (int t = 0; t < maxTicks; t++) {
            pos = pos.add(vel);
            vel = vel.scale(drag).add(0, -gravity, 0);

            // 玩家 bounding box 判断
            Vec3 playerPos = player.position();
            double px = playerPos.x;
            double py = playerPos.y + player.getBbHeight() * 0.5;
            double pz = playerPos.z;
            double radius = 0.5; // 宽容半径

            if (Math.abs(pos.x - px) < radius &&
                    Math.abs(pos.y - py) < radius &&
                    Math.abs(pos.z - pz) < radius) {
                return t;
            }
        }

        return Double.NaN; // 超过最大 tick 未命中
    }
}
