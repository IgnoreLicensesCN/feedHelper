package com.linearity.feedhelper.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.linearity.feedhelper.client.utils.InventoryUtils.equipToHandIf;
import static com.linearity.feedhelper.config.FeatureToggle.AUTO_CHANGE_CROSSBOW;
import static net.minecraft.world.item.CrossbowItem.getChargeDuration;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(
            method = "performShooting",
            at = @At("TAIL")
    )
    public void changeToFilledCrossbow(Level world, LivingEntity shooter, InteractionHand hand, ItemStack stack, float speed, float divergence, LivingEntity target, CallbackInfo ci){
        if (AUTO_CHANGE_CROSSBOW.getBooleanValue()){

            equipToHandIf(
                    (invStack) -> {
                        if (invStack.getItem() instanceof CrossbowItem){
                            ChargedProjectiles chargedProjectilesComponent = invStack.get(DataComponents.CHARGED_PROJECTILES);
                            return chargedProjectilesComponent != null && !chargedProjectilesComponent.isEmpty();
                        }
                        return false;
                    }, Minecraft.getInstance(),hand
            );
        }
    }
    @Inject(
            method = "onUseTick",
            at = @At("TAIL")
    )
    public void changeToEmptyCrossbow(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci){
        float f = (float)(stack.getUseDuration(user) - remainingUseTicks) / getChargeDuration(stack, user);
        if (AUTO_CHANGE_CROSSBOW.getBooleanValue() && f >= 1.F){

            InteractionHand hand = InteractionHand.OFF_HAND;
            if (user.getMainHandItem() == stack){
                hand = InteractionHand.MAIN_HAND;
            }

            equipToHandIf(
                    (invStack) -> {
                        if (invStack.getItem() instanceof CrossbowItem){
                            ChargedProjectiles chargedProjectilesComponent = invStack.get(DataComponents.CHARGED_PROJECTILES);
                            return chargedProjectilesComponent == null || chargedProjectilesComponent.isEmpty();
                        }
                        return false;
                    }, Minecraft.getInstance(),hand
            );
        }
    }
}

