package com.linearity.feedhelper.client.utils;

import com.google.common.collect.MapMaker;
import net.minecraft.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ShotTracker {

    // 弱引用实体 → 上次射击 tick
    static final Map<Entity, AtomicLong> lastShotTicks = new WeakHashMap<>();

    public static boolean hasLastShotTick(Entity entity) {
        return lastShotTicks.containsKey(entity);
    }

    public static void setLastShotTick(Entity entity, long tick) {
        lastShotTicks.put(entity, new AtomicLong(tick));
    }

    public static long getLastShotTick(Entity entity) {
        AtomicLong tick = lastShotTicks.get(entity);
        return tick == null ? -1 : tick.get();
    }

    public static void remove(Entity entity) {
        lastShotTicks.remove(entity);
    }

    public static void updateLastShotTick(Entity entity, long tick) {
        lastShotTicks.computeIfAbsent(entity, e -> new AtomicLong(tick)).set(tick);
    }
}