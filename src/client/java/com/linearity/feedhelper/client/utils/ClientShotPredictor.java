package com.linearity.feedhelper.client.utils;

import com.linearity.feedhelper.client.mixin.PiglinEntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.item.Items;

import java.util.concurrent.atomic.AtomicLong;

public class ClientShotPredictor {

    private final MinecraftClient client;
    public static long clientTickCounter = 0;

    public ClientShotPredictor(MinecraftClient client) {
        this.client = client;
    }

    public static long getTick() {
        return clientTickCounter;
    }

    public void onClientTick() {
        clientTickCounter++;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        for (Entity e : client.world.getEntities()) {
            // Skeleton 拉弓
            if (e instanceof AbstractSkeletonEntity skeleton && skeleton.isUsingItem() && skeleton.getActiveItem().getItem() == Items.BOW) {
                if (!ShotTracker.hasLastShotTick(skeleton)) {
                    ShotTracker.setLastShotTick(skeleton, getTick() - skeleton.getItemUseTime());
                }
            }
            // Drowned 蓄力三叉戟
            else if (e instanceof DrownedEntity drowned && drowned.isUsingItem() && drowned.getActiveItem().getItem() == Items.TRIDENT) {
                if (!ShotTracker.hasLastShotTick(drowned)) {
                    ShotTracker.setLastShotTick(drowned, getTick() - drowned.getItemUseTime());
                }
            }
            // Blaze 发射火球
            else if (e instanceof BlazeEntity blaze) {
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
            else if (e instanceof PiglinEntity piglin) {
                boolean charging = piglin.getDataTracker().get(((PiglinEntityAccessor)piglin).getChargingTrackedData()); // 通过 Mixin 公开或 Accessor
                if (charging && !ShotTracker.hasLastShotTick(piglin)) {
                    ShotTracker.setLastShotTick(piglin, getTick());
                }
            }
            // Pillager 蓄力十字弩
            else if (e instanceof PillagerEntity pillager) {
                boolean charging = pillager.isCharging(); // 公开方法
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