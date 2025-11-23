package com.linearity.feedhelper.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.linearity.feedhelper.client.FeedhelperClient.LOGGER;
import static com.linearity.feedhelper.client.utils.InventoryUtils.equipToHandIf;
import static com.linearity.feedhelper.config.FeatureToggle.AUTO_CHANGE_CROSSBOW;
import static net.minecraft.item.CrossbowItem.getPullTime;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(
            method = "shootAll",
            at = @At("TAIL")
    )
    public void changeToFilledCrossbow(World world, LivingEntity shooter, Hand hand, ItemStack stack, float speed, float divergence, LivingEntity target, CallbackInfo ci){
        if (AUTO_CHANGE_CROSSBOW.getBooleanValue()){

            equipToHandIf(
                    (invStack) -> {
                        if (invStack.getItem() instanceof CrossbowItem){
                            ChargedProjectilesComponent chargedProjectilesComponent = invStack.get(DataComponentTypes.CHARGED_PROJECTILES);
                            return chargedProjectilesComponent != null && !chargedProjectilesComponent.isEmpty();
                        }
                        return false;
                    }, MinecraftClient.getInstance(),hand
            );
        }
    }
    @Inject(
            method = "usageTick",
            at = @At("TAIL")
    )
    public void changeToEmptyCrossbow(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci){
        float f = (float)(stack.getMaxUseTime(user) - remainingUseTicks) / getPullTime(stack, user);
        if (AUTO_CHANGE_CROSSBOW.getBooleanValue() && f >= 1.F){

            Hand hand = Hand.OFF_HAND;
            if (user.getMainHandStack() == stack){
                hand = Hand.MAIN_HAND;
            }

            equipToHandIf(
                    (invStack) -> {
                        if (invStack.getItem() instanceof CrossbowItem){
                            ChargedProjectilesComponent chargedProjectilesComponent = invStack.get(DataComponentTypes.CHARGED_PROJECTILES);
                            return chargedProjectilesComponent == null || chargedProjectilesComponent.isEmpty();
                        }
                        return false;
                    }, MinecraftClient.getInstance(),hand
            );
        }
    }
}

