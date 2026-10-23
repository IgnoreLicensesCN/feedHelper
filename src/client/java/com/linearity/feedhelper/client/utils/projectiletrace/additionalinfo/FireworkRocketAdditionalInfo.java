package com.linearity.feedhelper.client.utils.projectiletrace.additionalinfo;

import com.linearity.feedhelper.client.mixin.FireworkRocketEntityAccessor;
import com.linearity.feedhelper.client.utils.projectiletrace.FloatRange;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.linearity.feedhelper.client.utils.ProjectilePredictionRenderer.submitHitResultOnScreen;

public record FireworkRocketAdditionalInfo(
        boolean shootAtAngle,
        @Nullable FireworkRocketEntity entity,
        int lifetimeLimit,
        @Nullable ItemStack fireworkStack,
        float radius,
        float maxDamage,
        List<FireworkExplosion> explosions
) {

    public static FireworkRocketAdditionalInfo of(@NotNull FireworkRocketEntity rocket){
        var accessor = (FireworkRocketEntityAccessor) rocket;
        var explosions = accessor.feedhelper$getExplosions();
        float radius = 0;
        float explodeDamage = 0;
        if (!explosions.isEmpty()){
            radius = 5;
            explodeDamage = 5.0F + explosions.size() * 2;
        }

        var canHoldTicks = accessor.feedhelper$getLifetime();
        var duration = 1;
        var fireworkStack = rocket.getItem();
        Fireworks fireworks = fireworkStack.get(DataComponents.FIREWORKS);
        if (fireworks != null) {
            duration += fireworks.flightDuration();
        }
        canHoldTicks = Math.max(10 * duration,canHoldTicks);


        return new FireworkRocketAdditionalInfo(
                rocket.isShotAtAngle(),
                rocket,
                canHoldTicks,
                rocket.getItem(),
                radius,
                explodeDamage,
                explosions
                );
    }
    @SuppressWarnings("unchecked")
    public static FireworkRocketAdditionalInfo of(boolean shootAtAngle, ItemStack fireworkStack) {
        int i = 1;
        float radius = 0;
        float explodeDamage = 0;
        Fireworks fireworks = fireworkStack.get(DataComponents.FIREWORKS);
        if (fireworks != null) {
            i += fireworks.flightDuration();
            radius = 5;
            explodeDamage = 5.0F + fireworks.explosions()
                    .size() * 2;
        }


        return new FireworkRocketAdditionalInfo(
                shootAtAngle,
                null,
                10 * i
                        + ThreadLocalRandom.current()
                        .nextInt(6) + ThreadLocalRandom.current()
                        .nextInt(7),
                fireworkStack,
                radius,
                explodeDamage,
                fireworks == null ? Collections.EMPTY_LIST : fireworks.explosions()
        );
    }

    public void submitExplodeHitEntityResults(Level atLevel, Vec3 pos) {
        if (radius() <= 0){return;}
        float fireworkBaseExplodeDamage = 0.0F;
        List<FireworkExplosion> list = explosions();
        if (!list.isEmpty()) {
            fireworkBaseExplodeDamage = 5.0F + list.size() * 2;
        }

        if (fireworkBaseExplodeDamage > 0.0F) {
//            if (this.attachedToEntity != null) {
//                this.attachedToEntity.hurtServer(
//                        serverLevel, this.damageSources()
//                                .fireworks(this, this.getOwner()), 5.0F + list.size() * 2
//                );
//            }
            int counter = 5;
            for (LivingEntity livingEntity : atLevel.getEntitiesOfClass(
                            LivingEntity.class, EntityType.FIREWORK_ROCKET.getSpawnAABB(pos.x,pos.y,pos.z)
                                    .inflate(5.0)
                    )) {
                if (counter < 0){break;}//avoid too much
                if (!(livingEntity.distanceToSqr(pos) > 25.0)) {
                    boolean bl = false;
                    HitResult hitResult = null;
                    for (int i = 0; i < 2; i++) {
                        Vec3 vec32 = new Vec3(livingEntity.getX(), livingEntity.getY(0.5 * i), livingEntity.getZ());
                        hitResult = atLevel
                                .clip(new ClipContext(
                                        pos, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                                        CollisionContext.empty()
                                ));
                        if (hitResult.getType() == HitResult.Type.MISS) {
                            bl = true;
                            break;
                        }
                    }

                    if (bl) {
                        counter -= 1;
                        float fireworkDamageDistanced = fireworkBaseExplodeDamage * fastSqrt((5.0f - fastSqrt(
                                (float) livingEntity.distanceToSqr(pos))) / 5.0f);
//                        livingEntity.hurtServer(
//                                serverLevel, this.damageSources()
//                                        .fireworks(this, this.getOwner()), g
//                        );

                        var currentHealthLine = "";
                        var expectHealthLine = "";


                        currentHealthLine = "%.2f/%.2f".formatted(livingEntity.getHealth(),livingEntity.getMaxHealth());
                        var absorptionAmount = livingEntity.getAbsorptionAmount();
                        if (absorptionAmount > 0){
                            currentHealthLine = "(+%.2f)".formatted(absorptionAmount) + currentHealthLine;
                        }
                        if (livingEntity.canBeHitByProjectile()){
                            var dmgRange = FireworkRocketAdditionalInfo.explodeDamageSimulate(
                                    livingEntity,fireworkDamageDistanced);
                            expectHealthLine = "%.2f".formatted(dmgRange.min());
                        }
                        hitResult = new EntityHitResult(livingEntity,hitResult.getLocation());
                        submitHitResultOnScreen(atLevel,hitResult,currentHealthLine,expectHealthLine);
                    }
                }
            }
        }

    }

    private static float fastSqrt(float x) {
        int i = Float.floatToIntBits(x);
        i = 0x1fbd1df5 + (i >> 1); // 经典的近似偏移量
        return Float.intBitsToFloat(i);
    }

    public static FloatRange explodeDamageSimulate(LivingEntity livingEntity, float fireworkDamageDistanced)
    {
        float afterAbsorbMin = simulateDamageAfterMagicAbsorb(
                simulateDamageAfterArmorAbsorb(fireworkDamageDistanced,livingEntity),livingEntity
        );
        return new FloatRange(afterAbsorbMin,afterAbsorbMin);
    }
    private static float simulateDamageAfterArmorAbsorb(float rawDamage, LivingEntity target) {
        float armor = (float) target.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        float i = 2.0F + toughness / 4.0F;
        float j = Mth.clamp(armor - rawDamage / i, armor * 0.2F, 20.0F);
        float k = j / 25.0F;
        // 3. 计算最终伤害
        return rawDamage * (1.0F - k);
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
        var enchantmentBlastProtection = atLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.BLAST_PROTECTION).orElse(null);
        var enchantmentProtection = atLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.PROTECTION).orElse(null);
        for (var equipSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var equipStack = target.getItemBySlot(equipSlot);
            if (equipStack.isEmpty()) continue;

            // 1. 检查弹射物保护 (1.21 获取方式)
            if (enchantmentBlastProtection != null){
                int enchLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantmentBlastProtection, equipStack);
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
}
