package com.linearity.feedhelper.client.utils;

import com.google.common.util.concurrent.AtomicDouble;
import com.linearity.feedhelper.client.FeedhelperClient;
import com.linearity.feedhelper.client.mixin.PiglinEntityAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.linearity.feedhelper.client.utils.ClientShotPredictor.getTick;
import static com.linearity.feedhelper.client.utils.InventoryUtils.equipToHandIf;
import static com.linearity.feedhelper.client.utils.ShotTracker.lastShotTicks;

public class ActiveDefenseRelated {

    //shielding related
    public static final AtomicBoolean forcedShield = new AtomicBoolean(false);
    public static AtomicDouble playerYawStored = new AtomicDouble(0);
    public static AtomicDouble playerPitchStored = new AtomicDouble(0);

    public static void activeDefenseLoop(MinecraftClient client) {

        World world = client.world;
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (player == null) return;
        if (world == null) return;
        if (interactionManager == null) return;

        double reach = player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);

        int blockRange = (int) Math.floor(reach);
        for (int x=-blockRange; x<=blockRange; x++){
            for (int y=-blockRange; y<=blockRange; y++){
                for (int z=-blockRange; z<=blockRange; z++){
                    BlockPos blockPos = player.getBlockPos().add(x, y, z);
                    if (blockPos.isWithinDistance(player.getEntityPos(), reach)) {
                        BlockState state = world.getBlockState(player.getBlockPos().add(x, y, z));
                        if (state.getBlock() instanceof FireBlock) {
                            interactionManager.attackBlock(blockPos,player.getHorizontalFacing());
                        }
                    }
                }
            }
        }

        reach = player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
        Box box = player.getBoundingBox().expand(reach);
        var toAttack = world.getEntitiesByClass(Entity.class, box,
                s -> s instanceof FireballEntity
                        || s instanceof ShulkerBulletEntity
                        || s instanceof WindChargeEntity
        );

        for (var beingAttack : toAttack) {
            interactionManager.attackEntity(player, beingAttack);
        }
    }

    public static void activeDefenseShieldingLoop(MinecraftClient client) {
        var world = client.world;
        var player = client.player;
        var interactionManager = client.interactionManager;
        if (player == null || world == null || interactionManager == null) return;


        boolean hasShield;
        Hand shieldHand = Hand.OFF_HAND;
        if (player.getMainHandStack().getItem() instanceof ShieldItem){
            shieldHand = Hand.MAIN_HAND;
            hasShield = true;
        }else {
            hasShield = player.getOffHandStack().getItem() instanceof ShieldItem;
            if (!hasShield) {
                equipToHandIf((stack -> stack.getItem() instanceof ShieldItem),client,Hand.OFF_HAND);
            }
            hasShield = player.getOffHandStack().getItem() instanceof ShieldItem;
        }
        if (hasShield){

            Box box = player.getBoundingBox().expand(30);
            List<Pair<Double, Entity>> projectilesNearbyCanReachPlayer = new ArrayList<>();
            AtomicBoolean hasHostileShooter = new AtomicBoolean(false);
            //iterate through
            world.getEntitiesByClass(Entity.class, box,
                    e -> {
                        if (!e.isAlive()){return false;}
                        if (!(e instanceof SmallFireballEntity || e instanceof PersistentProjectileEntity)) {
                            if (e instanceof RangedAttackMob){
                                if (e instanceof AbstractSkeletonEntity skeleton) {//Illusioner?什么B玩意，先不写，TODO:Illusioner
                                    if (skeleton.isUsingItem() && skeleton.getActiveItem().getItem() == Items.BOW) {
                                        hasHostileShooter.set(true);
                                        int ticksUntilShoot = 16 - skeleton.getItemUseTime(); // skeleton.getItemUseTime() = 拉弓已经持续的 tick
                                        Vec3d skeletonPos = skeleton.getEyePos();
                                        Vec3d playerPos = player.getEntityPos();

                                        Vec3d dir = playerPos.subtract(skeletonPos).normalize();
                                        Vec3d velocity = dir.multiply(1.6); // 初始速度
                                        double ticksToHit = getTicksToReachPlayerForSkeletonArrowPrediction(skeletonPos, velocity,player);
                                        if (!Double.isNaN(ticksToHit)){
                                            projectilesNearbyCanReachPlayer.add(new Pair<>(ticksUntilShoot + ticksToHit, e));
                                        }
                                        //                                        System.out.println(ticksUntilShoot + ticksToHit + " " + ticksUntilShoot + " " + ticksToHit);
                                    }
                                }
                                else if (e instanceof DrownedEntity drowned){
                                    ItemStack mainHand = drowned.getMainHandStack();
                                    if (mainHand.isOf(Items.TRIDENT) && drowned.isUsingItem()) {
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
                                        Vec3d startPos = drowned.getEntityPos().add(0, drowned.getHeight() * 0.333, 0);
                                        Vec3d targetPos = player.getEntityPos().add(0, player.getHeight() * 0.5, 0);

                                        // 方向向量 + 抛物线修正
                                        Vec3d delta = targetPos.subtract(startPos);
                                        double horizontalDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                                        Vec3d dir = new Vec3d(delta.x, delta.y + horizontalDist * 0.2, delta.z).normalize();

                                        // 初速度
                                        Vec3d velocity = dir.multiply(1.6);

                                        // 预测飞行时间，每tick模拟阻力和重力
                                        double ticksToHit = getTicksToReachPlayerForDrownedTrident(startPos, velocity, player);

                                        if (!Double.isNaN(ticksToHit)) {
                                            projectilesNearbyCanReachPlayer.add(new Pair<>(ticksUntilShoot + ticksToHit, e));
                                        }
                                    }

                                }
                                else if (e instanceof CrossbowUser shooter) {


                                    // 目标必须存在
                                    LivingEntity target = shooter.getTarget();
                                    if (target == null || target != player) return false;

                                    // 判断是否手上持交叉弓
                                    boolean holdingCrossbow = shooter instanceof LivingEntity le &&
                                            le.getMainHandStack().getItem() == Items.CROSSBOW;
                                    if (!holdingCrossbow) return false;

                                    // 获取当前 tick（你客户端可能需要维护 tick）
                                    long clientTick = getTick(); // 或自己维护的 tick
                                    long lastTick = lastShotTicks.getOrDefault(e, new AtomicLong(clientTick)).get();

                                    // 蓄力判定：CrossbowUser.isCharging()
                                    boolean charging = shooter instanceof PiglinEntity piglin ? piglin.getDataTracker().get(((PiglinEntityAccessor)piglin).getChargingTrackedData())
                                            : shooter instanceof PillagerEntity pillager && pillager.isCharging();

                                    if (charging){
                                        hasHostileShooter.set(true);
                                    }
                                    int attackInterval = 20; // 默认估算，如果你能从 Goal 或属性获取，可改成动态
                                    double ticksUntilShoot = charging ? attackInterval : Math.max(0, attackInterval - (clientTick - lastTick));

                                    // 投射物初始位置
                                    LivingEntity le = (LivingEntity) shooter;
                                    Vec3d startPos = le.getEyePos();
                                    Vec3d targetPos = player.getEntityPos().add(0, player.getHeight() * 0.5, 0);
                                    Vec3d delta = targetPos.subtract(startPos);
                                    double horizontalDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                                    Vec3d dir = new Vec3d(delta.x, delta.y + horizontalDist * 0.2, delta.z).normalize();
                                    Vec3d velocity = dir.multiply(1.6); // 交叉弓初速，可调

                                    // 预测命中时间
                                    double ticksToHit = getTicksToReachPlayerForCrossbow(startPos, velocity, player);

                                    if (!Double.isNaN(ticksToHit)) {
                                        projectilesNearbyCanReachPlayer.add(new Pair<>(ticksUntilShoot + ticksToHit, e));
                                    }

                                    // 如果当前刚发射或者释放蓄力，则更新 lastShotTick
                                    if (!charging) {
                                        lastShotTicks.get(e).set(clientTick);
                                    }
                                }
                                else {
                                    System.out.println("mob not predicted:" + e.getName());
                                }
                            }
                            else if (e instanceof BlazeEntity blaze) {

                                boolean burning = blaze.isOnFire();
                                if (!burning) {
                                    return false;
                                }

                                hasHostileShooter.set(true);
                                long clientTick = getTick();
                                long lastTick = ShotTracker.getLastShotTick(blaze);

                                final int firstFireballDelay = 60;

                                long dt = clientTick - lastTick;   // 已蓄力时间
                                long ticksUntilShoot;


                                ticksUntilShoot = Math.max(0, firstFireballDelay - dt);

                                Vec3d startPos = blaze.getEyePos();
                                Vec3d targetPos = player.getEntityPos().add(0, player.getHeight() * 0.6, 0);

                                Vec3d delta = targetPos.subtract(startPos);
                                Vec3d dir = delta.lengthSquared() < 1e-6 ? new Vec3d(0, 0, 1) : delta.normalize();
                                dir = dir.multiply(0.9);

                                double ticksToHit = getTicksToReachPlayerForBlazeFireballPrediction(startPos, dir, player);

                                if (!Double.isNaN(ticksToHit)) {
                                    projectilesNearbyCanReachPlayer.add(new Pair<>(ticksUntilShoot + ticksToHit, e));
                                }

                            }


                            return false;
                        }
                        var velocity = e.getVelocity();
                        var pos = e.getEntityPos();
                        double ticksToHit;
                        if (e instanceof PersistentProjectileEntity) {
                            ticksToHit  = getTicksToReachPlayerForSkeletonArrow(pos, velocity,player);
                        }else {
                            ticksToHit = getTicksToReachPlayerForBlazeFireball(pos,velocity,player);
                        }
                        if (Double.isNaN(ticksToHit)) {
                            return false;
                        }
                        projectilesNearbyCanReachPlayer.add(new Pair<>(ticksToHit, e));
                        return false;
                    }
            );
            projectilesNearbyCanReachPlayer.sort(Comparator.comparingDouble(Pair::getLeft));

            if (!projectilesNearbyCanReachPlayer.isEmpty()) {
                Pair<Double,Entity> projectileOrLauncher = projectilesNearbyCanReachPlayer.getFirst();

                if (projectileOrLauncher.getLeft() <= 8.){

//                    System.out.println(projectileOrLauncher.getLeft() + " " + projectileOrLauncher.getRight() + " " + projectileOrLauncher.getRight().getVelocity().length());

                    if (!forcedShield.get() && !player.isUsingItem()) {
                        forcedShield.set(true);
                        playerYawStored.set(player.getYaw());
                        playerPitchStored.set(player.getPitch());
                        interactionManager.interactItem(player, shieldHand);
                        player.setCurrentHand(shieldHand);
                        MinecraftClient.getInstance().options.useKey.setPressed(true);
                    }
                    faceEntity(player, projectileOrLauncher.getRight());
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
                    player.setYaw((float) playerYawStored.get());
                    player.setPitch((float) playerPitchStored.get());
                    interactionManager.stopUsingItem(player);
                    MinecraftClient.getInstance().options.useKey.setPressed(false);
//                    System.out.println("stop using item");
                    FeedhelperClient.considerReleaseShieldTimestamp.set(Long.MIN_VALUE);
                }
            }
        }

    }

    public static void faceEntity(ClientPlayerEntity player, Entity target) {
        Vec3d playerPos = player.getEntityPos().add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3d targetPos = target.getEntityPos().add(0, target.getHeight(), 0);

        Vec3d diff = targetPos.subtract(playerPos);

        double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float yaw = (float)(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(diff.y, distXZ)));

        player.setYaw(yaw);
        player.setPitch(pitch);

        // 同步更新头部方向（不加的话第一人称可能不变化）
        player.setHeadYaw(yaw);
    }

    public static double getTicksToReachPlayerForSkeletonArrow(Vec3d projectilePos, Vec3d projectileVelocity,PlayerEntity player) {
        if (player == null) return Double.NaN;

        Vec3d pos = projectilePos;
        Vec3d vel = projectileVelocity;
        if (projectileVelocity.lengthSquared() < 1e-6) {
            return Double.NaN;
        }

        final double drag = 0.99; // PersistentProjectileEntity 默认阻力
        final double gravity = 0.05; // ArrowEntity 默认重力
        final int maxTicks = 200;
        final double hitRadius = 0; // 玩家碰撞箱宽容值，减小 NaN 出现概率

        Box box = player.getBoundingBox();

        for (int t = 0; t < maxTicks; t++) {
            // 检测是否进入玩家碰撞箱
            if (pos.x >= box.minX - hitRadius && pos.x <= box.maxX + hitRadius &&
                    pos.y >= box.minY - hitRadius && pos.y <= box.maxY + hitRadius &&
                    pos.z >= box.minZ - hitRadius && pos.z <= box.maxZ + hitRadius) {
                return t;
            }

            // 更新速度和位置
            vel = vel.multiply(drag).add(0, -gravity, 0); // 先应用阻力，再加重力
            pos = pos.add(vel);
        }

        return Double.NaN; // 超过最大 tick 仍未击中
    }
    public static double getTicksToReachPlayerForSkeletonArrowPrediction(Vec3d projectilePos, Vec3d projectileVelocity, PlayerEntity player) {
        if (player == null) return Double.NaN;
        if (projectileVelocity.lengthSquared() < 1e-6) return Double.NaN;

        Vec3d start = projectilePos;
        Vec3d target = player.getEntityPos().add(0, player.getHeight() * 0.5, 0);

        Vec3d delta = target.subtract(start);
        final double drag = 0.99;
        final double gravity = 0.05;

        // 水平距离
        Vec3d deltaXZ = new Vec3d(delta.x, 0, delta.z);
        double vXZ = new Vec3d(projectileVelocity.x, 0, projectileVelocity.z).length();
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
    public static double getTicksToReachPlayerForDrownedTrident(Vec3d start, Vec3d velocity, PlayerEntity player) {
        Vec3d pos = start;
        Vec3d vel = velocity;
        final double drag = 0.99; // 阻力
        final double gravity = 0.05; // 重力，三叉戟略小可调
        int maxTicks = 40; // 最大预测 tick 数

        for (int t = 0; t < maxTicks; t++) {
            pos = pos.add(vel);
            vel = vel.multiply(drag).add(0, -gravity, 0);

            // 玩家 bounding box 判断
            Vec3d playerPos = player.getEntityPos();
            double px = playerPos.x;
            double py = playerPos.y + player.getHeight() * 0.5;
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
            Vec3d pos,Vec3d vel, PlayerEntity player
    ) {

        final double drag = 0.91; // 阻力
        final double gravity = 0.0; // 火球基本不受重力
        final double accelerationPower = 0.1;

        int maxTicks = 200;
        double hitRadius = 1; // 宽容半径

        for (int t = 0; t < maxTicks; t++) {
            // 模拟当前位置与玩家 hitbox 判断
            Vec3d targetPos = player.getEntityPos().add(0, player.getHeight() * 0.6, 0); // 瞄准头部
            if (pos.squaredDistanceTo(targetPos) <= hitRadius * hitRadius) {
                return t;
            }

            // 按火球逻辑更新速度和位置
            vel = vel.add(vel.normalize().multiply(accelerationPower)).multiply(drag);
            pos = pos.add(vel);
        }

        return Double.NaN; // 超过最大 tick 仍未击中
    }
    public static double getTicksToReachPlayerForBlazeFireballPrediction(
            Vec3d start, Vec3d velocity, PlayerEntity player
    ) {
        Vec3d pos = start;
        Vec3d vel = velocity;

        final double drag = 0.91;
        final double gravity = 0.0;
        final double accelerationPower = 0.1; // Blaze 默认加速

        Vec3d targetPos = player.getEntityPos().add(0, player.getHeight() * 0.6, 0);
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
            vel = vel.add(vel.normalize().multiply(accelerationPower)).multiply(drag);
            pos = pos.add(vel);
        }

        return Double.NaN; // 超过最大 tick 仍未到
    }
    public static double getTicksToReachPlayerForCrossbow(Vec3d start, Vec3d velocity, PlayerEntity player) {
        Vec3d pos = start;
        Vec3d vel = velocity;
        final double drag = 0.99; // 阻力
        final double gravity = 0.05; // 重力，可调
        int maxTicks = 40; // 最大预测 tick 数

        for (int t = 0; t < maxTicks; t++) {
            pos = pos.add(vel);
            vel = vel.multiply(drag).add(0, -gravity, 0);

            // 玩家 bounding box 判断
            Vec3d playerPos = player.getEntityPos();
            double px = playerPos.x;
            double py = playerPos.y + player.getHeight() * 0.5;
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
