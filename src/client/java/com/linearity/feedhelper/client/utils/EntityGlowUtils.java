package com.linearity.feedhelper.client.utils;

import com.linearity.colorannoation.annoation.ARGBColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.ChestRaftEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Rarity;
import org.jetbrains.annotations.Nullable;

public class EntityGlowUtils {



    @ARGBColor
    public static @Nullable Integer getGlowColor(final Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return 0xFF39C5BB;
        }
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack itemStack = itemEntity.getStack();
            return getItemColor(itemStack);
        }
        if (entity instanceof DisplayEntity.ItemDisplayEntity itemDisplayEntity){
            return getItemColor(itemDisplayEntity.getItemStack());
        }
        if (entity instanceof ItemFrameEntity itemFrameEntity){
            return getItemColor(itemFrameEntity.getPickBlockStack());
        }
        if (entity instanceof Monster){
            if (entity instanceof WitherSkeletonEntity
                    || entity instanceof EnderDragonEntity
                    || entity instanceof WitherEntity
                    || entity instanceof WardenEntity
            ){
                return 0xFFFF55FF;
            }
            else {
                return 0xFFE50000;
            }
        }
        if (entity instanceof ChestMinecartEntity
                || entity instanceof ChestBoatEntity
                || entity instanceof ChestRaftEntity){
            return 0xFF22CC78;
        }
        if(entity instanceof AbstractBoatEntity){
            return 0xFF8C5A33;
        }
        if (entity instanceof VillagerEntity){
            return 0xFFBABA00;
        }
        if (entity instanceof ProjectileEntity){
            return 0xFFFF4271;
        }
        if (entity instanceof WolfEntity wolfEntity) {
            if (!wolfEntity.isTamed()) {
                if (wolfEntity.hasAngerTime()) {
                    return 0xFFFF0000;
                }
            }
        }
        if (entity instanceof Tameable) {
            if (entity instanceof TameableEntity tameable) {
                if (tameable.isTamed()) {
                    return 0xFF372FFF;
                } else {
                    return 0xFFA4B0FF;
                }
            }
            if (entity instanceof AbstractHorseEntity horse) {
                if (horse.isTame()){
                    return 0xFF372FFF;
                }
            }
        }
        if (entity instanceof IronGolemEntity ironGolem){
            if (ironGolem.isPlayerCreated()){
                return 0xFF372FFF;
            }
            else {
                return 0xFFA4B0FF;
            }
        }
        if (entity instanceof SnowGolemEntity || entity instanceof CopperGolemEntity){
            return 0xFF372FFF;
        }
        if (entity instanceof SkeletonHorseEntity){
            return 0xFF990000;
        }

        return null;
    }
    @ARGBColor
    public static int getItemColor(final ItemStack itemStack) {
        if (itemStack == null) {
            return 0xFF888888;
        }
        Rarity rarity = itemStack.getRarity();
        return switch (rarity) {
            case COMMON -> 0xFF888888;
            case UNCOMMON -> 0xFFFFFF55;
            case RARE -> 0xFF55FFFF;
            case EPIC -> 0xFFFF55FF;
        };
    }
}
