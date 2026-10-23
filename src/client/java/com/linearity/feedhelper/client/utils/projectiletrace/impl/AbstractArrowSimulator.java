package com.linearity.feedhelper.client.utils.projectiletrace.impl;

import com.linearity.feedhelper.client.utils.projectiletrace.FloatRange;
import com.linearity.feedhelper.client.utils.projectiletrace.IProjectileSimulator;
import com.linearity.feedhelper.client.utils.projectiletrace.ProjectileSimulateContext;
import com.linearity.feedhelper.client.utils.projectiletrace.ProjectileState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AbstractArrowSimulator implements IProjectileSimulator<@NotNull AbstractArrow> {
    private AbstractArrowSimulator(){}
    public static AbstractArrowSimulator INSTANCE = new AbstractArrowSimulator();
    public static ProjectileSimulateContext.InertiaAtPosFunction arrowInertiaAtPosFunc(Level level,float waterInertia,float airInertia){
        return pos -> level.getFluidState(pos).isEmpty()?airInertia:waterInertia;
    }
    @Override
    public ProjectileState simulateNextTick(ProjectileSimulateContext<?> context, ProjectileState current) {
        var level = context.level();
        var inGround = context.isInGround();
        var hasPhysicsFlag = !context.isNoPhysics();
        var currentAsBlockPos = current.blockPos();
        var currentAtBlockState = level.getBlockState(currentAsBlockPos);
        if (!inGround) {
            if (!currentAtBlockState.isAir() && hasPhysicsFlag) {
                VoxelShape voxelShape = currentAtBlockState.getCollisionShape(level, currentAsBlockPos);
                if (!voxelShape.isEmpty()) {
                    Vec3 vec32 = current.pos();
                    for (AABB aABB : voxelShape.toAabbs()) {
                        if (aABB.move(currentAsBlockPos).contains(vec32)) {
                            inGround = true;
                            break;
                        }
                    }
                }
            }
        }
        if (inGround) {
            return current;
        }

        var nextPos = current.pos();
        var nextVelocity = current.velocity().scale(context.inertiaAtPos().calculate(currentAsBlockPos));


        if (hasPhysicsFlag) {
            nextVelocity = nextVelocity.add(0,-context.gravity(), 0);//oops getGravity is positive
        }
        boolean shouldOnFire = current.onFire();
        nextPos = nextPos.add(nextVelocity);
        var nextBlockPos = new BlockPos(Mth.floor(nextPos.x),Mth.floor(nextPos.y),Mth.floor(nextPos.z));
        if (currentAtBlockState.getFluidState().is(Fluids.LAVA) || currentAtBlockState.getFluidState().is(Fluids.FLOWING_LAVA)) {
            shouldOnFire = true;
        }else if (
                currentAtBlockState.getFluidState().is(Fluids.WATER)
                || currentAtBlockState.getFluidState().is(Fluids.FLOWING_WATER)
                || (level.isRainingAt(nextBlockPos))
        ){
            shouldOnFire = false;
        }

        return new ProjectileState(nextPos,nextVelocity,shouldOnFire,nextBlockPos,current.lifetime()+1,getHitResult(context,current,nextPos));
    }
    public static FloatRange damageSimulate(Level atLevel, boolean isCritArrow, EntityHitResult hitResult, @Nullable ItemStack weaponStack, Vec3 velocity){
        var baseRange = damageSimulateBase(atLevel,isCritArrow,weaponStack,velocity);
        if (!(hitResult.getEntity() instanceof LivingEntity livingEntity)) {
            return baseRange;
        }
        float afterAbsorbMin = simulateDamageAfterMagicAbsorb(
                simulateDamageAfterArmorAbsorb(baseRange.min(),livingEntity,weaponStack),livingEntity
        );
        float afterAbsorbMax = simulateDamageAfterMagicAbsorb(
                simulateDamageAfterArmorAbsorb(baseRange.max(),livingEntity,weaponStack),livingEntity
        );
        return new FloatRange(afterAbsorbMin,afterAbsorbMax);
    }
    private static float simulateDamageAfterArmorAbsorb(float rawDamage, LivingEntity target, @Nullable ItemStack weapon) {
        float armor = (float) target.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        float i = 2.0F + toughness / 4.0F;
        float j = Mth.clamp(armor - rawDamage / i, armor * 0.2F, 20.0F);
        float k = j / 25.0F;

        float l = k;
        if (weapon != null) {
            // 尝试获取穿刺附魔等级 (Breach)
            // 注意：这里需要根据你的映射环境查找 Breach 的注册名

            int breachLevel = 0;//EnchantmentHelper.getItemEnchantmentLevel(/* 穿刺附魔的 Holder */, weapon);
            var enchantment = target.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.BREACH).orElse(null);
            if (enchantment != null){
                breachLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment,weapon);
            }
            if (breachLevel > 0) {
                // 穿刺每级减少 15% 的护甲有效性
                l = Mth.clamp(k * (1.0F - breachLevel * 0.15F), 0.0F, 1.0F);
            }
        }

        // 3. 计算最终伤害
        return rawDamage * (1.0F - l);
    }
    private static float simulateDamageAfterMagicAbsorb(float damageAfterArmor, LivingEntity target) {
        float f = damageAfterArmor;

        // 1. 模拟抗性提升
        if (target.hasEffect(MobEffects.RESISTANCE)) {
            var effect = target.getEffect(MobEffects.RESISTANCE);
            if (effect != null) {
                int amp = effect.getAmplifier() + 1;
                f *= Math.max(0, 1.0F - (amp * 0.2F));
            }
        }

        // 2. 模拟附魔 (EPF)
        // 注意：客户端无法直接调 getDamageProtection，你需要自己遍历 target.getArmorSlots()
        // 然后累加弹射物保护的等级
        float epf = getClientSideEPF(target);
        epf = Math.min(20.0F, epf);

        f *= (1.0F - epf / 25.0F);

        return f;
    }
    private static float getClientSideEPF(LivingEntity target) {
        float totalEPF = 0;
        var atLevel = target.level();
        var enchantmentProjectileProtection = atLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.PROJECTILE_PROTECTION).orElse(null);
        var enchantmentProtection = atLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.PROTECTION).orElse(null);
        for (var equipSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var equipStack = target.getItemBySlot(equipSlot);
            if (equipStack.isEmpty()) continue;

            // 1. 检查弹射物保护 (1.21 获取方式)
            if (enchantmentProjectileProtection != null){
                int enchLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantmentProjectileProtection, equipStack);
                totalEPF += enchLevel * 2.0F;
            }
            if (enchantmentProtection != null){
                int enchLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantmentProtection, equipStack);
                totalEPF += enchLevel * 2.0F;
            }
        }

        // 3. 应用 20 点上限限制
        return Math.min(20.0F, totalEPF);
    }
    private static FloatRange damageSimulateBase(Level atLevel, boolean isCritArrow, @Nullable ItemStack weaponStack, Vec3 velocity) {
        double velocityMultiplier = velocity.length(); // 箭的速度
        double damage = 2;//this.baseDamage; // 基础伤害（默认通常是 2.0）

        if (weaponStack != null) {
            var enchantment = atLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.POWER).orElse(null);
            if (enchantment != null) {
                var powerLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment,weaponStack);
                damage += (0.5 * (powerLevel + 1));
            }
        }
        int i = Mth.ceil(Mth.clamp(velocityMultiplier * damage, 0.0, 2.147483647E9));
        if (isCritArrow) {
            return new FloatRange(i+2,(i*1.5F) + 2);
        }
        return new FloatRange(i,i);
    }
}
