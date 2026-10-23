package com.linearity.feedhelper.client.utils.projectiletrace.impl;

import com.linearity.feedhelper.client.utils.BlockPosUtils;
import com.linearity.feedhelper.client.utils.projectiletrace.IProjectileSimulator;
import com.linearity.feedhelper.client.utils.projectiletrace.ProjectileSimulateContext;
import com.linearity.feedhelper.client.utils.projectiletrace.ProjectileState;
import com.linearity.feedhelper.client.utils.projectiletrace.ProjectileUtil;
import com.linearity.feedhelper.client.utils.projectiletrace.additionalinfo.FireworkRocketAdditionalInfo;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FireworkSimulator implements IProjectileSimulator<@NotNull FireworkRocketEntity> {
    private FireworkSimulator(){}
    public static FireworkSimulator INSTANCE = new FireworkSimulator();
    @Override
    public ProjectileState simulateNextTick(ProjectileSimulateContext<?> context, ProjectileState previousProjectileState) {
        var additionalInfoNotCasted = context.additionalInfo();
        HitResult hitResult = null;
        var nextVelocity = previousProjectileState.velocity();
        if (additionalInfoNotCasted instanceof FireworkRocketAdditionalInfo(
                boolean shootAtAngle,
                @Nullable FireworkRocketEntity rocketEntity,
                int lifetimeLimit,
                @Nullable ItemStack fireworkStack,
                float radius,
                float maxDamage,
                List<FireworkExplosion> explosions
        )) {
            if (previousProjectileState.lifetime() > lifetimeLimit) {
                return previousProjectileState;
            }
            if (!shootAtAngle){
                double horizontalSpeedMultiplier = this.getHorizontalCollision(
                        rocketEntity,
                        0.F,
                        context.isInGround(),
                        IProjectileSimulator.defaultEntityCanCollideWith(
                                rocketEntity),context,previousProjectileState
                        ) ? 1.0 : 1.15;
                nextVelocity = nextVelocity.multiply(
                        horizontalSpeedMultiplier,
                        1.0,
                        horizontalSpeedMultiplier
                ).add(0.0, 0.04, 0.0);
            }
//            Vec3 vec33 = nextVelocity;
            if (!context.isNoPhysics()){
                hitResult = ProjectileUtil.getHitResultOnMoveVector(
                        context.level(),
                        nextVelocity,
                        previousProjectileState.pos(),
                        previousProjectileState.lifetime(),
                        IProjectileSimulator.defaultCanHitEntity(rocketEntity),
                        CollisionContext.empty(),
                        context.boundingBoxAtZeroPos()
                                .move(previousProjectileState.pos())
                );
            }
//            this.move(MoverType.SELF, vec33);
//            this.applyEffectsFromBlocks();
//            this.setDeltaMovement(vec33);
        }

        var nextPos = previousProjectileState.pos().add(nextVelocity);
        return new ProjectileState(
                previousProjectileState.pos().add(nextVelocity),
                nextVelocity,
                previousProjectileState.onFire(),
                BlockPosUtils.fromVec3(nextPos),
                previousProjectileState.lifetime() + 1,
                hitResult
        );
    }
}
