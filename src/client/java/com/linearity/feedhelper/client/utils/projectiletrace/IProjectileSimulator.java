package com.linearity.feedhelper.client.utils.projectiletrace;


import com.google.common.collect.ImmutableList;
import com.linearity.feedhelper.client.mixin.ProjectileAccessor;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface IProjectileSimulator<E extends Entity> {
    default HitResult getHitResult(
            ProjectileSimulateContext<?> context,
            ProjectileState current,
            Vec3 nextPos
    ){
        var level = context.level();
        HitResult hitResult = null;
        BlockHitResult blockHit = level.clip(new ClipContext(
                current.pos(), nextPos, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY, CollisionContext.empty()
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            hitResult = blockHit;
        }

        if (hitResult == null){
            List<Entity> entities = new LinkedList<>();
            level.entitiesForRendering().forEach(entity -> {
                if (entity.isAlive() && !entity.isSpectator()) entities.add(entity);
            });
            for (Entity entity : entities) {
                Vec3 predictedPos = entity.position()
                        .add(entity.getDeltaMovement()
                                .scale(current.lifetime()));
                AABB predictedBox = entity.getBoundingBox()
                        .move(predictedPos.subtract(entity.position()));
                Optional<Vec3> intersect = predictedBox.clip(current.pos(), nextPos);
                if (intersect.isPresent()) {
                    hitResult = new EntityHitResult(entity, intersect.get());
                    break;
                }
            }
        }
        if (hitResult == null){
            hitResult = new HitResult(nextPos) {
                @Override
                public Type getType() {
                    return Type.MISS;
                }
            };
        }
        return hitResult;
    }

    static Predicate<Entity> defaultEntityCanCollideWith(@Nullable Entity self){
        if (self == null){
            return _ignored -> true;
        }
        return entity -> entity.canBeCollidedWith(self) && !self.isPassengerOfSameVehicle(entity);
    }
    static Predicate<Entity> defaultCanHitEntity(@Nullable Projectile self){
        if (self == null){
            return Entity::canBeHitByProjectile;
        }
        return entity -> {
            if (!entity.canBeHitByProjectile()) {
                return false;
            } else {
                Entity entity2 = self.getOwner();
                return entity2 == null || ((ProjectileAccessor)self).feedhelper$getLeftOwner() || !entity2.isPassengerOfSameVehicle(entity);
            }
        };
    }
    ProjectileState simulateNextTick(ProjectileSimulateContext<?> context, ProjectileState previousProjectileState);


    default boolean getHorizontalCollision(
            @Nullable E entity,
            float maxUpStep,
            boolean onGround,
            Predicate<Entity> selfEntityCanCollideWith,
            ProjectileSimulateContext<?> context,
            ProjectileState previousProjectileState
    ){
        if (context.isNoPhysics()){
            return false;
        }
        Vec3 currentVelocity = previousProjectileState.velocity();
        Vec3 collideVelocity = collide(entity,maxUpStep,onGround,selfEntityCanCollideWith,context,previousProjectileState);
        boolean bl = !Mth.equal(collideVelocity.x, currentVelocity.x);
        boolean bl2 = !Mth.equal(collideVelocity.z, currentVelocity.z);
        return bl || bl2;
    }

    default boolean getHorizontalCollision(
            float maxUpStep,
            boolean onGround,
            CollisionContext selfEntityCollisionContext,
            Predicate<Entity> isSelfEntity,
            Predicate<Entity> selfEntityCanCollideWith,
            ProjectileSimulateContext<?> context,
            ProjectileState previousProjectileState
    ){
        if (context.isNoPhysics()){
            return false;
        }
        Vec3 currentVelocity = previousProjectileState.velocity();
        Vec3 collideVelocity = collide(selfEntityCollisionContext,maxUpStep,onGround,isSelfEntity,selfEntityCanCollideWith,context,previousProjectileState);
        boolean bl = !Mth.equal(collideVelocity.x, currentVelocity.x);
        boolean bl2 = !Mth.equal(collideVelocity.z, currentVelocity.z);
        return bl || bl2;
    }
    static Vec3 collide(
            @Nullable Entity selfEntity,
            float maxUpStep,
            boolean onGround,
            Predicate<Entity> selfEntityCanCollideWith,
            ProjectileSimulateContext<?> context,
            ProjectileState previousProjectileState){
        return collide(
                selfEntity==null?CollisionContext.empty():CollisionContext.of(selfEntity),
                maxUpStep,
                onGround,
                e -> e==selfEntity,
                selfEntityCanCollideWith,
                context,
                previousProjectileState
        );
    }
    static Vec3 collide(
            CollisionContext collisionContextOfEntity,
            float maxUpStep,
            boolean onGround,
            Predicate<Entity> isSelfEntity,
            Predicate<Entity> selfEntityCanCollideWith,
            ProjectileSimulateContext<?> context,
            ProjectileState previousProjectileState
    ) {
        Vec3 currentVelocity = previousProjectileState.velocity();
        Vec3 currentPos = previousProjectileState.pos();
        AABB aABB = context.boundingBoxAtZeroPos().move(currentPos);
        List<VoxelShape> list = getEntityCollisions(
                context.level(),
                isSelfEntity,selfEntityCanCollideWith,
                aABB.expandTowards(currentVelocity)
        );
        Vec3 vec32 =
                currentVelocity.lengthSqr() == 0.0
                        ? currentVelocity
                        : collideBoundingBox(
                                collisionContextOfEntity,
                                currentVelocity,
                                aABB,
                                context.level(),
                                list,context,previousProjectileState
                );
        boolean bl = currentVelocity.x != vec32.x;
        boolean bl2 = currentVelocity.y != vec32.y;
        boolean bl3 = currentVelocity.z != vec32.z;
        boolean bl4 = bl2 && currentVelocity.y < 0.0;
        if (maxUpStep > 0.0F && (bl4 || onGround) && (bl || bl3)) {
            AABB aABB2 = bl4 ? aABB.move(0.0, vec32.y, 0.0) : aABB;
            AABB aABB3 = aABB2.expandTowards(currentVelocity.x, maxUpStep, currentVelocity.z);
            if (!bl4) {
                aABB3 = aABB3.expandTowards(0.0, -1.0E-5F, 0.0);
            }

            List<VoxelShape> list2 = collectColliders(
                    collisionContextOfEntity,
                    context.level(),
                    list,
                    aABB3,
                    context,
                    previousProjectileState
            );
            float f = (float)vec32.y;
            float[] fs = collectCandidateStepUpHeights(aABB2, list2, maxUpStep, f);

            for (float g : fs) {
                Vec3 vec33 = collideWithShapes(new Vec3(currentVelocity.x, g, currentVelocity.z), aABB2, list2);
                if (vec33.horizontalDistanceSqr() > vec32.horizontalDistanceSqr()) {
                    double d = aABB.minY - aABB2.minY;
                    return vec33.subtract(0.0, d, 0.0);
                }
            }
        }

        return vec32;
    }

    static List<VoxelShape> getEntityCollisions(
            Level entityAtLevel,
            Predicate<Entity> isSelfEntity,
            @Nullable Predicate<Entity> selfEntityCanCollideWith,
            AABB aABB
    ) {
        if (aABB.getSize() < 1.0E-7) {
            return List.of();
        } else {
            Predicate<Entity> predicate = selfEntityCanCollideWith == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(selfEntityCanCollideWith);
            List<Entity> list = entityAtLevel.getEntities((Entity) null, aABB.inflate(1.0E-7), predicate);
            list.removeIf(isSelfEntity);
            if (list.isEmpty()) {
                return List.of();
            } else {
                ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(list.size());

                for (Entity entity2 : list) {
                    builder.add(Shapes.create(entity2.getBoundingBox()));
                }

                return builder.build();
            }
        }
    }
    static float[] collectCandidateStepUpHeights(AABB aABB, List<VoxelShape> list, float f, float g) {
        FloatSet floatSet = new FloatArraySet(4);

        for (VoxelShape voxelShape : list) {
            for (double d : voxelShape.getCoords(Direction.Axis.Y)) {
                float h = (float)(d - aABB.minY);
                if (!(h < 0.0F) && h != g) {
                    if (h > f) {
                        break;
                    }

                    floatSet.add(h);
                }
            }
        }

        float[] fs = floatSet.toFloatArray();
        FloatArrays.unstableSort(fs);
        return fs;
    }

    static boolean isInsideCloseToBorder(WorldBorder border,double entityX,double entityZ, AABB aABB) {
        double d = Math.max(Mth.absMax(aABB.getXsize(), aABB.getZsize()), 1.0);
        return border.getDistanceToBorder(entityX,entityZ) < d * 2.0 && border.isWithinBounds(entityX, entityZ, d);
    }
    static Vec3 collideBoundingBox(
            CollisionContext collisionContextOfEntity, Vec3 vec3, AABB aABB, Level level, List<VoxelShape> list,
            ProjectileSimulateContext<?> context,
            ProjectileState previousProjectileState
    ) {
        List<VoxelShape> list2 = collectColliders(
                collisionContextOfEntity,
                level,
                list,
                aABB.expandTowards(vec3),
                context,previousProjectileState
        );
        return collideWithShapes(vec3, aABB, list2);
    }
    private static List<VoxelShape> collectColliders(
            CollisionContext collisionContextOfEntity,
            Level level,
            List<VoxelShape> list,
            AABB aABB,
            ProjectileSimulateContext<?> context,
            ProjectileState previousProjectileState
    ) {
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(list.size() + 1);
        if (!list.isEmpty()) {
            builder.addAll(list);
        }

        WorldBorder worldBorder = level.getWorldBorder();
        boolean bl = isInsideCloseToBorder(worldBorder,previousProjectileState.pos().x,previousProjectileState.pos().z, aABB);
        if (bl) {
            builder.add(worldBorder.getCollisionShape());
        }

        //#getBlockCollisions
        //#getBlockCollisionsFromContext
        builder.addAll(
                () -> new BlockCollisions<>(level, collisionContextOfEntity, aABB, false,
                        (mutableBlockPos, voxelShape) -> voxelShape)
        );
        return builder.build();
    }
    private static Vec3 collideWithShapes(Vec3 vec3, AABB aABB, List<VoxelShape> list) {
        if (list.isEmpty()) {
            return vec3;
        } else {
            Vec3 vec32 = Vec3.ZERO;

            for (Direction.Axis axis : Direction.axisStepOrder(vec3)) {
                double d = vec3.get(axis);
                if (d != 0.0) {
                    double e = Shapes.collide(axis, aABB.move(vec32), list, d);
                    vec32 = vec32.with(axis, e);
                }
            }

            return vec32;
        }
    }
}
