package com.linearity.feedhelper.client.utils;

import com.linearity.feedhelper.client.mixin.PiglinEntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.Items;

public class ClientShotPredictor {

    public static long clientTickCounter = 0;

    public static long getTick() {
        return clientTickCounter;
    }

    public static void onClientTick(Minecraft client) {
        clientTickCounter++;

        LocalPlayer player = client.player;
        if (player == null) return;
        var level = client.level;
        if (level == null) return;

        for (Entity e : level.entitiesForRendering()) {
            // Skeleton 拉弓
            if (e instanceof AbstractSkeleton skeleton && skeleton.isUsingItem() && skeleton.getUseItem().getItem() == Items.BOW) {
                if (!ShotTracker.hasLastShotTick(skeleton)) {
                    ShotTracker.setLastShotTick(skeleton, getTick() - skeleton.getTicksUsingItem());
                }
            }
            // Drowned 蓄力三叉戟
            else if (e instanceof Drowned drowned && drowned.isUsingItem() && drowned.getUseItem().getItem() == Items.TRIDENT) {
                if (!ShotTracker.hasLastShotTick(drowned)) {
                    ShotTracker.setLastShotTick(drowned, getTick() - drowned.getTicksUsingItem());
                }
            }
            // Blaze 发射火球
            else if (e instanceof Blaze blaze) {
                boolean burning = blaze.isOnFire();
                boolean hadLastTick = ShotTracker.hasLastShotTick(blaze);

                // 刚从不着火 → 变成着火 = 正在进入攻击前摇阶段
                if (burning && !hadLastTick) {
                    ShotTracker.setLastShotTick(blaze, getTick());
                }

                // 如果又熄火，说明完成一次攻击循环，清理记录
                if (!burning && hadLastTick) {
                    ShotTracker.remove(blaze);
                }
            }
            // Piglin 蓄力十字弩
            else if (e instanceof Piglin piglin) {
                boolean charging = piglin.getEntityData().get(((PiglinEntityAccessor)piglin).getChargingTrackedData()); // 通过 Mixin 公开或 Accessor
                if (charging && !ShotTracker.hasLastShotTick(piglin)) {
                    ShotTracker.setLastShotTick(piglin, getTick());
                }
            }
            // Pillager 蓄力十字弩
            else if (e instanceof Pillager pillager) {
                boolean charging = pillager.isChargingCrossbow(); // 公开方法
                if (charging && !ShotTracker.hasLastShotTick(pillager)) {
                    ShotTracker.setLastShotTick(pillager, getTick());
                }
            }
            else {
                // 重置状态
                ShotTracker.remove(e);
            }
        }
    }

    public static long getTicksSinceLastShot(Entity entity) {
        long lastTick = ShotTracker.getLastShotTick(entity);
        if (lastTick < 0) return -1;
        return getTick() - lastTick;
    }
}