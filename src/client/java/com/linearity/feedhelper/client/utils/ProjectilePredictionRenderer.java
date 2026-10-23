package com.linearity.feedhelper.client.utils;

import com.linearity.feedhelper.client.mixin.FireworkRocketEntityAccessor;
import com.linearity.feedhelper.client.utils.projectiletrace.HitResultFrameInfo;
import com.linearity.feedhelper.client.utils.projectiletrace.ProjectileSimulateContext;
import com.linearity.feedhelper.client.utils.projectiletrace.ProjectileState;
import com.linearity.feedhelper.client.utils.projectiletrace.additionalinfo.FireworkRocketAdditionalInfo;
import com.linearity.feedhelper.client.utils.projectiletrace.impl.AbstractArrowSimulator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

import static com.linearity.feedhelper.client.FeedhelperClient.hitResultInfo;
import static com.linearity.feedhelper.client.utils.RangingSystemRelated.predictArrowPath;
import static com.linearity.feedhelper.client.utils.RangingSystemRelated.predictFireworkPath;
import static com.linearity.feedhelper.client.utils.projectiletrace.impl.AbstractArrowSimulator.arrowInertiaAtPosFunc;
import static com.linearity.feedhelper.config.Configs.Generic.PREDICATE_PROJECTILE_TICKS;
import static com.linearity.feedhelper.config.RangingSystemFeatureToggle.SHOW_ARROW_HIT_RESULT;

public class ProjectilePredictionRenderer {
    public static void renderProjectilePath(
            Level world,
            PoseStack matrices, List<ProjectileState> path, Vec3 cameraPos,
            float[] color, double beamWidth, float sphereRadius
    ) {
        if (path.isEmpty()) return;
        if (path.size() < 2) return;
        var pathIterator = path.iterator();
        var currentState = pathIterator.next();
        var nextState = currentState;
        matrices.pushPose();
        while (pathIterator.hasNext()) {
            nextState = pathIterator.next();
            Vec3 start = currentState.pos()
                    .subtract(cameraPos);
            Vec3 end = nextState.pos()
                    .subtract(cameraPos);
            RenderingUtils.renderBeam(matrices, start, end, color, (float) beamWidth);
            currentState = nextState;
        }

        // 渲染终点小球
        Vec3 hitPos = nextState.pos()
                .subtract(cameraPos);
        renderFireworkSphere(matrices, hitPos, sphereRadius, color, 8);
    }

    private static boolean hasFlame(ClientLevel level, ItemStack stack) {
        // 通过 level 的 registryAccess 查找附魔注册表
        return level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT) // 找到附魔表
                .get(Enchantments.FLAME)              // 用 ResourceKey 换取 Holder
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0)
                .orElse(false);
    }
    public static void renderPlayerAbstractArrowPrediction(
            Player player,
            @Nullable ItemStack weaponStack,
            ClientLevel world, PoseStack matrices,
            float[] color, double beamWidth, float sphereRadius,
            Vec3 cameraPos,
            Vec3 startPos,
            Vec3 velocity,
            boolean startWithFire,
            AABB aabbAtPosZero,
            ProjectileSimulateContext.InertiaAtPosFunction inertiaAtPosFunc
            ){

        var startState = new ProjectileState(
                startPos, velocity, startWithFire, BlockPosUtils.fromVec3(startPos), 0, new HitResult(startPos) {
            @Override
            public @NotNull Type getType() {
                return Type.MISS;
            }
        }
        );
        var context = new ProjectileSimulateContext<>(
                world,
                false,
                inertiaAtPosFunc,
                false,
                0.05,
                aabbAtPosZero,
                null
        );
        var path = predictArrowPath(context, startState);

        renderProjectilePath(world,matrices, path, cameraPos,color,beamWidth,sphereRadius);


        var hitResult = path.getLast().hitResult();
        var currentHealthLine = "";
        var expectHealthLine = "";
        if (hitResult instanceof EntityHitResult entityHitResult){
            var entityHit = entityHitResult.getEntity();
            if (entityHit instanceof LivingEntity living){
                currentHealthLine = "%.2f/%.2f".formatted(living.getHealth(),living.getMaxHealth());
                var absorptionAmount = living.getAbsorptionAmount();
                if (absorptionAmount > 0){
                    currentHealthLine = "(+%.2f)".formatted(absorptionAmount) + currentHealthLine;
                }
                if (living.canBeHitByProjectile()){
                    var dmgRange = AbstractArrowSimulator.damageSimulate(world,player.fallDistance>0,entityHitResult,
                            weaponStack,path.getLast().velocity());
                    expectHealthLine = "%.2f~%.2f".formatted(dmgRange.min(),dmgRange.max());
                }
            }
        }
        submitHitResultOnScreen(world,hitResult,currentHealthLine,expectHealthLine);
    }
    public static void renderPlayerBowPrediction(
            Player player, ItemStack bow,
            ClientLevel world, PoseStack matrices,
            Vec3 cameraPos
    ) {
        int useTicks = player.getTicksUsingItem();
        float charge = BowItem.getPowerForTime(useTicks);

        Vec3 startPos = player.getEyePosition();
        Vec3 velocity = player.getLookAngle()
                .scale(charge * 3.0); // Minecraft 弓初速度

        boolean startWithFire = hasFlame(world, bow);

        renderPlayerAbstractArrowPrediction(
                player,
                bow,
                world,
                matrices,
                new float[]{1f, 0.9f, 0.1f, 0.5f},
                0.1,
                0.5f,
                cameraPos,
                startPos,
                velocity,
                startWithFire,
                EntityType.ARROW.getSpawnAABB(0,0,0),
                arrowInertiaAtPosFunc(world,0.6F,0.99F)
        );
    }

    public static void renderPlayerCrossbowPrediction(
            Player player,
            ItemStack weaponStack,
            ClientLevel world,
            PoseStack matrices,
            Vec3 cameraPos,
            float tickDelta
    ) {
        if (!(weaponStack.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(weaponStack)) return;

        ChargedProjectiles projectiles = weaponStack.getOrDefault(
                DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);

        boolean multishot = weaponStack.getEnchantments()
                .keySet()
                .stream()
                .anyMatch(e -> e.unwrapKey()
                        .map(k -> k == Enchantments.MULTISHOT)
                        .orElse(false));

        float[] angles = multishot ? new float[]{0f, 10f, -10f} : new float[]{0f};

        Vec3 startPos = player.getEyePosition();
        float yaw = player.getViewYRot(tickDelta);
        float pitch = player.getViewXRot(tickDelta);
        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);

        Vec3 look = new Vec3(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        Vec3 right = new Vec3(
                Math.cos(yawRad),
                0,
                -Math.sin(yawRad)
        ).normalize();

        for (ItemStack proj : projectiles.getItems()) {
            for (float angleDeg : angles) {
                double angleRad = Math.toRadians(angleDeg);
                Vec3 rotated = look.scale(Math.cos(angleRad))
                        .add(right.scale(Math.sin(angleRad)))
                        .normalize();

                boolean isFirework = proj.is(Items.FIREWORK_ROCKET);
                Vec3 velocity = rotated.scale(isFirework ? 1.6f : 2.5f);

                if (!isFirework) {
                    renderPlayerAbstractArrowPrediction(
                            player,
                            weaponStack,
                            world,
                            matrices,
                            new float[]{1f, 0.9f, 0.1f, 0.5f},
                            0.1,
                            0.5f,
                            cameraPos,
                            startPos,
                            velocity,
                            hasFlame(world, weaponStack),
                            EntityType.ARROW.getSpawnAABB(0,0,0),
                            arrowInertiaAtPosFunc(world,0.6F,0.99F)
                    );
                } else {
                    renderFireworkPrediction(world, matrices, cameraPos, proj, startPos, velocity, FireworkRocketAdditionalInfo.of(true, proj));
                }
            }
        }
    }

    public static void renderFireworkPrediction(
            ClientLevel world,
            PoseStack matrices,
            Vec3 cameraPos,
            ItemStack proj,
            Vec3 startPos,
            Vec3 velocity,
            FireworkRocketAdditionalInfo info
    ) {
        var lifetime = 0;
        var context = new ProjectileSimulateContext<>(
                world,
                false,
                _any -> 1.F,
                false,
                0,
                EntityType.FIREWORK_ROCKET.getSpawnAABB(0, 0, 0),
                info
        );
        var canHoldTicks = info.lifetimeLimit();
        if (info.entity() instanceof FireworkRocketEntityAccessor accessor) {
            var duration = 1;
            var fireworkStack = info.entity().getItem();
            Fireworks fireworks = fireworkStack.get(DataComponents.FIREWORKS);
            if (fireworks != null) {
                duration += fireworks.flightDuration();
            }
            lifetime = accessor.feedhelper$getLife();
            canHoldTicks = Math.max(10 * duration,accessor.feedhelper$getLifetime()) - lifetime;
        }
        var startState = new ProjectileState(
                startPos, velocity, false, BlockPosUtils.fromVec3(startPos), lifetime, new HitResult(startPos) {
            @Override
            public @NotNull Type getType() {
                return Type.MISS;
            }
        }
        );
        int pathLimit = PREDICATE_PROJECTILE_TICKS.getIntegerValue();
        int pathSize = Math.min(pathLimit,canHoldTicks);
        var path = predictFireworkPath(context, startState,pathSize);

        float[] color = getFireworkBeamColor(proj);

        float beamWidth = 0.05f;
        if (path.size() < 2) {
            return;
        }
        var pathIterator = path.iterator();
        var currentState = pathIterator.next();
        var nextState = currentState;
        matrices.pushPose();
        while (pathIterator.hasNext()) {
            nextState = pathIterator.next();
            Vec3 start = currentState.pos()
                    .subtract(cameraPos);
            Vec3 end = nextState.pos()
                    .subtract(cameraPos);
            RenderingUtils.renderBeam(matrices, start, end, color, beamWidth);
            currentState = nextState;
        }
        Vec3 hitPos = currentState.pos()
                .subtract(cameraPos);
        renderFireworkSphere(matrices, hitPos, info.radius(), color, 16);
        info.submitExplodeHitEntityResults(world,currentState.hitResult().getLocation());

        renderAdditionalFireworkPrediction(world, matrices, cameraPos, info, context, currentState, color, beamWidth);
        matrices.popPose();
    }

    private static void renderAdditionalFireworkPrediction(ClientLevel world, PoseStack matrices, Vec3 cameraPos, FireworkRocketAdditionalInfo info, ProjectileSimulateContext<FireworkRocketAdditionalInfo> context, ProjectileState currentState, float[] color, float beamWidth) {
        ProjectileState nextState;
        Iterator<ProjectileState> pathIterator;
        Vec3 hitPos;
        var addPath = predictFireworkPath(
                new ProjectileSimulateContext<>(
                        context.level(),
                        context.isInGround(),
                        context.inertiaAtPos(),
                        context.isNoPhysics(),
                        context.gravity(),
                        context.boundingBoxAtZeroPos(),
                        new FireworkRocketAdditionalInfo(
                                info.shootAtAngle(),
                                info.entity(),
                                info.lifetimeLimit() + 11,
                                info.fireworkStack(),
                                info.radius(),
                                info.maxDamage(),
                                info.explosions()
                        )
                ), currentState,11);
        if (!addPath.isEmpty()) {
            pathIterator = addPath.iterator();
            while (pathIterator.hasNext()) {
                nextState = pathIterator.next();
                Vec3 start = currentState.pos()
                        .subtract(cameraPos);
                Vec3 end = nextState.pos()
                        .subtract(cameraPos);
                RenderingUtils.renderBeam(matrices, start, end, color, beamWidth);
                currentState = nextState;
            }
            hitPos = currentState.pos().subtract(cameraPos);
            renderFireworkSphere(matrices, hitPos, info.radius(), color, 16);
            info.submitExplodeHitEntityResults(world, currentState.hitResult().getLocation());
        }
    }

    private static void renderFireworkSphere(PoseStack matrices, Vec3 hitPos, float info, float[] color, int latDivisions) {
        matrices.pushPose();
        matrices.translate(hitPos.x, hitPos.y, hitPos.z);
        RenderingUtils.renderTransparentSphere(matrices, 0, 0, 0, info, color, latDivisions, latDivisions);
        matrices.popPose();
    }


    public static void submitHitResultOnScreen(Level level, HitResult hitResult,String currentHealthLine,String expectHitHealthLine) {
        if (!SHOW_ARROW_HIT_RESULT.getBooleanValue()){
            return;
        }
//        if (hitResult.getType() == HitResult.Type.MISS) {
//            return;
//        }
        var client = Minecraft.getInstance();
        var hitPos = hitResult.getLocation();
        Vec3 screen = RenderingUtils.worldToScreen(client, hitPos);
        if (screen == null) return;

        int x = (int) screen.x;
        int y = (int) screen.y;


        var name = "";
        if (hitResult instanceof EntityHitResult entityHitResult) {
            name = entityHitResult.getEntity()
                    .getName()
                    .getString();
        }
        if (hitResult instanceof BlockHitResult blockHitResult) {
            name = level.getBlockState(blockHitResult.getBlockPos())
                    .getBlock()
                    .getName()
                    .getString();
        }
        String printText = name + " " + String.format("x=%.3f,y=%.3f,z=%.3f", hitPos.x(), hitPos.y(), hitPos.z());
        hitResultInfo.add(new HitResultFrameInfo(printText,0xFF39C5BB,x,y,currentHealthLine,expectHitHealthLine));
    }

    private static final float[] NULL_FIREWORK_COLOR = {1,1,1,.5F};
    private static float[] getFireworkBeamColor(@Nullable ItemStack proj) {
        if (proj == null){return NULL_FIREWORK_COLOR;}
        float[] color = new float[]{1,1,1,.5F};

        var fireworkComp = proj.getComponents()
                .get(DataComponents.FIREWORKS);
        if (fireworkComp != null && !fireworkComp.explosions()
                .isEmpty()) {
            FireworkExplosion explosion = fireworkComp.explosions()
                    .getFirst();
            if (!explosion.colors()
                    .isEmpty()) {
                int c = explosion.colors()
                        .getInt(0);
                color[0] = ((c >> 16) & 0xFF) / 255f;
                color[1] = ((c >> 8) & 0xFF) / 255f;
                color[2] = (c & 0xFF) / 255f;
            }
            if (!explosion.fadeColors()
                    .isEmpty()) {
                int c = explosion.fadeColors()
                        .getInt(0);
                color[0] = (color[0] + ((c >> 16) & 0xFF) / 255f) / 2f;
                color[1] = (color[1] + ((c >> 8) & 0xFF) / 255f) / 2f;
                color[2] = (color[2] + (c & 0xFF) / 255f) / 2f;
            }
        } else {
            color[0] = 1f;
            color[1] = 0.5f;
            color[2] = 0f; // 默认橙色
        }
        return color;
    }

    /**
     * 玩家三叉戟预测
     */
    public static void renderPlayerTridentPrediction(
            Player player, ItemStack trident,
            ClientLevel world, PoseStack matrices,
            Vec3 cameraPos
    ) {
        Vec3 startPos = player.getEyePosition();
        Vec3 velocity = player.getLookAngle()
                .scale(2.5);
        renderPlayerAbstractArrowPrediction(
                player,
                trident,
                world,
                matrices,
                new float[]{1f, 0.9f, 0.1f, 0.5f},
                0.1,
                0.5f,
                cameraPos,
                startPos,
                velocity,
                false,
                EntityType.ARROW.getSpawnAABB(0,0,0),
                _any -> 0.99F
        );
    }
}

