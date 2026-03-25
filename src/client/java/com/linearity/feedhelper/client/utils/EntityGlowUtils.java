package com.linearity.feedhelper.client.utils;

import com.linearity.colorannoation.annoation.ARGBColor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.boat.ChestRaft;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

public class EntityGlowUtils {



    @ARGBColor
    public static @Nullable Integer getGlowColor(final Entity entity) {
        if (entity instanceof Player) {
            return 0xFF39C5BB;//TODO:Player color rules
        }
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack itemStack = itemEntity.getItem();
            return getItemColor(itemStack);
        }
        if (entity instanceof Display.ItemDisplay itemDisplayEntity){
            return getItemColor(itemDisplayEntity.getItemStack());
        }
        if (entity instanceof ItemFrame itemFrameEntity){
            return getItemColor(itemFrameEntity.getPickResult());
        }
        if (entity instanceof Enemy){
            if (entity instanceof WitherSkeleton
                    || entity instanceof EnderDragon
                    || entity instanceof WitherBoss
                    || entity instanceof Warden
            ){
                return 0xFFFF55FF;
            }
            else {
                return 0xFFE50000;
            }
        }
        if (entity instanceof MinecartChest
                || entity instanceof ChestBoat
                || entity instanceof ChestRaft){
            return 0xFF22CC78;
        }
        if(entity instanceof AbstractBoat){
            return 0xFF8C5A33;
        }
        if (entity instanceof Villager){
            return 0xFFBABA00;
        }
        if (entity instanceof Projectile){
            return 0xFFFF4271;
        }
        if (entity instanceof Wolf wolfEntity) {
            if (!wolfEntity.isTame()) {
                if (wolfEntity.isAngry()) {
                    return 0xFFFF0000;
                }
            }
        }
        if (entity instanceof OwnableEntity) {
            if (entity instanceof TamableAnimal tameable) {
                if (tameable.isTame()) {
                    return 0xFF372FFF;
                } else {
                    return 0xFFA4B0FF;
                }
            }
            if (entity instanceof AbstractHorse horse) {
                if (horse.isTamed()){
                    return 0xFF372FFF;
                }
            }
        }
        if (entity instanceof IronGolem ironGolem){
            if (ironGolem.isPlayerCreated()){
                return 0xFF372FFF;
            }
            else {
                return 0xFFA4B0FF;
            }
        }
        if (entity instanceof SnowGolem || entity instanceof CopperGolem){
            return 0xFF372FFF;
        }
        if (entity instanceof SkeletonHorse){
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
