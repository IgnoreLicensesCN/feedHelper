package com.linearity.feedhelper.client;

import com.linearity.feedhelper.client.event.ClientTickHandler;
import com.linearity.feedhelper.client.event.InputHandler;
import com.linearity.feedhelper.client.event.RenderHandler;
import com.linearity.feedhelper.client.mixin.AbstractBlockAccessor;
import com.linearity.feedhelper.client.utils.InventoryUtils;
import com.linearity.feedhelper.config.Callbacks;
import com.linearity.feedhelper.config.Configs;
import com.linearity.feedhelper.config.FeatureToggle;
import com.linearity.feedhelper.gui.GuiConfigs;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.*;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;

import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.linearity.feedhelper.client.utils.InventoryUtils.equipToHandIf;
import static com.linearity.feedhelper.client.utils.RangingSystemRelated.*;

public class FeedhelperClient implements ClientModInitializer , IInitializationHandler {
    public static final String MOD_ID = "feedhelper";
    public static final String MOD_NAME = "feedHelper";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final AtomicBoolean forceSneakingFlag = new AtomicBoolean(false);
    public static final Map<BlockPos,Item> placesToPlantWithItems = new ConcurrentHashMap<>();

    public static final Queue<Runnable> runOnNextTickQueue = new ConcurrentLinkedQueue<>();


    @Override
    public void onInitializeClient() {

        InitializationHandler.getInstance().registerInitializationHandler(this);



        WorldRenderEvents.BEFORE_TRANSLUCENT.register((context) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            MatrixStack matrices = context.matrices();
            float delta = client.getRenderTickCounter().getDynamicDeltaTicks();

            if (FeatureToggle.RANGING_SYSTEM.getBooleanValue()){
                rangingSystemLoopRendering(client, matrices, delta);
            }
        });
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.INFO_BAR,
                Identifier.of("feedhelper", "debug_rect"),
                (drawContext, renderTickCounter) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    float delta = client.getRenderTickCounter().getDynamicDeltaTicks();

                    if (FeatureToggle.RANGING_SYSTEM.getBooleanValue()){
                        rangingSystemLoopRenderingOnHUD(client, drawContext, delta);
                    }
                }
        );
    }

    public static void fillLavaLoop(MinecraftClient client) {
        var player = client.player;
        var world = client.world;
        var interactionManager = client.interactionManager;
        if (player == null || world == null || interactionManager == null) return;
        Hand hand = Hand.OFF_HAND;
        ItemStack handStack = player.getOffHandStack();
        if (handStack == null || handStack.isEmpty()) {
            hand = Hand.MAIN_HAND;
        }
        handStack = player.getMainHandStack();
        if (handStack == null || handStack.isEmpty()) {
            return;
        }
        Set<BlockPos> placedPos = new HashSet<>();
        if (handStack.getItem() instanceof BlockItem blockItem) {
            double reach = player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE) - 1.2;
            int radius = (int) Math.ceil(reach);
            List<BlockPos> blockPosList = new ArrayList<>(radius*radius*radius*8);
            for (int y = -radius; y <= radius; y++) {
                for (int x = -radius+1; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        blockPosList.add(pos);
                    }
                }
            }
            blockPosList.sort((pos1,pos2) -> {
                int dist1 = pos1.getX()*pos1.getX() +pos1.getY()*pos1.getY() +pos1.getZ()*pos1.getZ();
                int dist2 = pos2.getX()*pos2.getX() + pos2.getY()*pos2.getY() +pos2.getZ()*pos2.getZ();
                return Integer.compare(dist2, dist1);
            });
            for (BlockPos pos : blockPosList) {
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                {
                    BlockPos center = new BlockPos(player.getBlockX() + x,player.getBlockY() + y,player.getBlockZ() + z);
                    BlockState centerState = world.getBlockState(center);
                    if (centerState.getFluidState().isIn(FluidTags.LAVA)){continue;}
                    BlockHitResult hitResult;
                    Pair<BlockPos,Direction>[] pairs = new Pair[]{
                            new Pair(center.up(),Direction.UP),
                            new Pair(center.down(),Direction.DOWN),
                            new Pair(center.north(),Direction.NORTH),
                            new Pair(center.south(),Direction.SOUTH),
                            new Pair(center.west(),Direction.WEST),
                            new Pair(center.east(),Direction.EAST),
                    };
                    for (Pair<BlockPos,Direction> pair : pairs) {
                        if (placedPos.contains(pair.getLeft())) {
                            continue;
                        }
                        if (!center.isWithinDistance(player.getEntityPos(),radius)){
                            continue;
                        }
                        if (world.getBlockState(pair.getLeft()).getFluidState().isIn(FluidTags.LAVA)) {
                            hitResult = new BlockHitResult(
                                    pair.getLeft().toCenterPos(),
                                    pair.getRight(),
                                    center,
                                    false
                            );
                            interactionManager.interactBlock(player,hand,hitResult);
                            placedPos.add(pair.getLeft());
                        }
                    }
                }
            }
        }
    }

    public static void autoTorchLoop(MinecraftClient client) {
        var player = client.player;
        var world = client.world;
        var interactionManager = client.interactionManager;
        if (player == null || world == null || interactionManager == null) return;

        Hand hand = Hand.MAIN_HAND;
        ItemStack handStack = player.getMainHandStack();
        if (handStack == null || handStack.isEmpty() || !handStack.isOf(Items.TORCH)) {
            handStack = player.getOffHandStack();
            if (handStack == null || handStack.isEmpty() || !handStack.isOf(Items.TORCH)) {
                return; // 双手都没有火把
            }
            hand = Hand.OFF_HAND;
        }

        double reach = player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE) - 1.2;
        int radius = (int) Math.ceil(reach);

        // 扫描范围
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos center = player.getBlockPos().add(x, y, z);
                    if (!center.isWithinDistance(player.getEntityPos(), reach)) continue;

                    BlockState centerState = world.getBlockState(center);
                    if (centerState.getFluidState().isIn(FluidTags.LAVA) || centerState.getFluidState().isIn(FluidTags.WATER)) continue;

                    // 周围六个方向尝试放火把
                    for (Direction dir : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                        BlockPos targetPos = center.offset(dir);
                        BlockState targetState = world.getBlockState(targetPos);

                        if (!targetState.isAir()) continue; // 目标必须是空气
                        int light = world.getLightLevel(LightType.BLOCK, targetPos);

// 如果当前格亮度为 0，检查周围是否已经是亮的
                        if (light == 0) {
                            boolean neighborBright = false;

                            for (Direction checkDir : Direction.values()) {
                                BlockPos np = targetPos.offset(checkDir);
                                int nl = world.getLightLevel(LightType.BLOCK, np);

                                if (nl >= 8) { // 邻格已有亮光 → 说明光照还没更新
                                    neighborBright = true;
                                    break;
                                }
                            }

                            if (neighborBright) {
                                continue; // 光照未更新完成，不放火把
                            }
                        }

// 原本的判断：如果太亮直接跳过
                        if (light > 7) continue;

                        // 判断火把是否能放
                        if (((AbstractBlockAccessor)Blocks.TORCH).invokeCanPlaceAt(targetState, world, targetPos)) {
                            BlockHitResult hitResult = new BlockHitResult(
                                    targetPos.toCenterPos(),
                                    dir,
                                    center,
                                    false
                            );
                            boolean noMoreTorchOnHand = false;
                            if (handStack.getCount() == 1){
                                noMoreTorchOnHand = true;
                            }

                            interactionManager.interactBlock(player, hand, hitResult);
                            if (noMoreTorchOnHand) {
                                equipToHandIf((stack -> Objects.equals(stack.getItem(),Items.TORCH)),client,Hand.OFF_HAND);
                            }

                            return; // 放置成功就结束本刻循环
                        }
                    }
                }
            }
        }
    }


    public static void tryFeedNearbyAnimals(MinecraftClient client) {
        var player = client.player;
        var world = client.world;
        var interactionManager = client.interactionManager;
        if (player == null || world == null || interactionManager == null) return;


// player interaction reach (client-side approximation)
        double reach = player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);

// Box around player
        Box box = player.getBoundingBox().expand(reach);
        var animals = world.getEntitiesByClass(AnimalEntity.class, box, a -> true);


        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) return;


        for (AnimalEntity animal : animals) {
            if (!animal.isBreedingItem(held)) continue;
            if (animal.isBaby() || animal.isInLove()) continue;


// simulate interact
            interactionManager.interactEntity(player, animal, Hand.MAIN_HAND);
//            return; //feed a lot
        }
    }

    public static void tryFarm(MinecraftClient client) {
        var player = client.player;
        var world = client.world;
        var interactionManager = client.interactionManager;
        if (player == null || world == null || interactionManager == null) return;

        double reach = player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE) - 1;

        BlockPos playerPos = player.getBlockPos();

        int radius = (int) Math.ceil(reach);

        // 枚举附近方块
        BlockPos.Mutable mutable = new BlockPos.Mutable();

            for (int y = -radius; y <= radius; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {

                        mutable.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);

                        BlockState state = world.getBlockState(mutable);
                        Block block = state.getBlock();
                        if (!placesToPlantWithItems.isEmpty()){
                            Set<BlockPos> entriesToRemove = new HashSet<>();
                            for (Map.Entry<BlockPos, Item> entry : placesToPlantWithItems.entrySet()) {
                                if (equipSeedToOffhand(player, entry.getValue(),client)) {
                                    BlockHitResult hit = new BlockHitResult(
                                            entry.getKey().up().toCenterPos(),
                                            Direction.UP,
                                            entry.getKey(),
                                            false
                                    );
                                    interactionManager.interactBlock(
                                            player,
                                            Hand.OFF_HAND,
                                            hit
                                    );
                                    entriesToRemove.add(entry.getKey());
                                }
                            }
                            for (BlockPos pos : entriesToRemove) {
                                placesToPlantWithItems.remove(pos);
                            }
                        }

                        if ((block instanceof CropBlock crop)) {

                            // 未成熟直接跳过
                            if (!crop.isMature(state)) continue;
                            BlockPos cropPos = mutable.toImmutable();

                            // ───── 自动收割（破坏 mature 作物） ─────
                            interactionManager.attackBlock(mutable, player.getHorizontalFacing());

                            // ───── 自动补种（使用种子） ─────
                            // 储存副手物品
                            ItemStack originalOffhand = player.getOffHandStack().copy();

// equip seed → 放到副手
                            BlockHitResult hit = new BlockHitResult(
                                    Vec3d.ofCenter(cropPos),
                                    Direction.UP,
                                    cropPos.down(),
                                    false
                            );
                            if (equipSeedToOffhand(player, seedFromBlock(state.getBlock()),client)) {
                                interactionManager.interactBlock(
                                        player,
                                        Hand.OFF_HAND,
                                        hit
                                );
                            }else {
                                placesToPlantWithItems.put(cropPos.down(),seedFromBlock(state.getBlock()));
                            }

// restore → 补回去
//                        restoreOffhand(player, originalOffhand);
                        }
                        else if (block instanceof NetherWartBlock netherWart) {
                            if (state.get(NetherWartBlock.AGE) != NetherWartBlock.MAX_AGE){
                                continue;
                            }
                            BlockPos cropPos = mutable.toImmutable();

                            // ───── 自动收割（破坏 mature 作物） ─────
                            interactionManager.attackBlock(mutable, player.getHorizontalFacing());

                            // ───── 自动补种（使用种子） ─────
                            // 储存副手物品
                            ItemStack originalOffhand = player.getOffHandStack().copy();

// equip seed → 放到副手
                            BlockHitResult hit = new BlockHitResult(
                                    Vec3d.ofCenter(cropPos),
                                    Direction.UP,
                                    cropPos.down(),
                                    false
                            );
                            if (equipSeedToOffhand(player, seedFromBlock(state.getBlock()),client)) {
                                interactionManager.interactBlock(
                                        player,
                                        Hand.OFF_HAND,
                                        hit
                                );
                            }
                            else {
                                placesToPlantWithItems.put(cropPos.down(),seedFromBlock(state.getBlock()));
                            }

// restore → 补回去
//                        restoreOffhand(player, originalOffhand);
                        }
                        else if (block == Blocks.SUGAR_CANE) {
                            BlockPos below = mutable.down();
                            BlockPos twoBelow = below.down();

                            BlockState belowState = world.getBlockState(below);
                            BlockState twoBelowState = world.getBlockState(twoBelow);

                            // 当前甘蔗上方没有甘蔗，且下方是甘蔗，破坏当前甘蔗
                            // 或者：当前方块是甘蔗，下面是甘蔗，下面的下面不是甘蔗
                            if (belowState.getBlock() == Blocks.SUGAR_CANE && twoBelowState.getBlock() != Blocks.SUGAR_CANE) {
                                interactionManager.attackBlock(mutable, player.getHorizontalFacing());
                            }
                        }
                        else if (block instanceof SweetBerryBushBlock sweetBerryBush) {
                            // 成熟判断
                            int age = state.get(SweetBerryBushBlock.AGE);
                            if (age >= SweetBerryBushBlock.MAX_AGE) {
                                // 成熟了，可以右键收获
                                BlockHitResult hit = new BlockHitResult(
                                        Vec3d.ofCenter(mutable),
                                        Direction.UP,
                                        mutable.toImmutable(),
                                        false
                                );
                                interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
                            }
                        }

                        if (player.getMainHandStack().isIn(ItemTags.HOES)){
                            if (block instanceof LeavesBlock leavesBlock  && getBreakTimeTicks(player,state,world,mutable) == 1) {
                                interactionManager.attackBlock(mutable, player.getHorizontalFacing());
                            }
                        }
                        if (y >= 0){
                            if (player.getMainHandStack().isIn(ItemTags.SHOVELS)) {
                                if (Objects.equals(player.getOffHandStack().getItem(),block.asItem())
                                        && getBreakTimeTicks(player,state,world,mutable) == 1
                                ) {
                                    interactionManager.attackBlock(mutable, player.getHorizontalFacing());
                                }
                            }
                            if (player.getMainHandStack().isIn(ItemTags.PICKAXES)) {
                                if (
                                        Objects.equals(player.getOffHandStack().getItem(),block.asItem())
                                                && getBreakTimeTicks(player,state,world,mutable) == 1
                                ) {
                                    interactionManager.attackBlock(mutable, player.getHorizontalFacing());
                                }
                            }
                        }
//                    else if (block instanceof BambooBlock) {
//                        BlockPos below = mutable.down();
//                        BlockPos twoBelow = below.down();
//
//                        BlockState belowState = world.getBlockState(below);
//                        BlockState twoBelowState = world.getBlockState(twoBelow);
//
//                        // 顶部竹子判断：下面是竹子，下面的下面不是竹子
//                        if (belowState.getBlock() instanceof BambooBlock && !(twoBelowState.getBlock() instanceof BambooBlock)) {
//                            // 持续破坏竹子顶部
//
//                        }
//                    }
//                    else if (block instanceof CocoaBlock cocoa) {
//                        int age = state.get(CocoaBlock.AGE);
//                        if (age >= CocoaBlock.MAX_AGE) {
//                            // 1️⃣ 收割：持续破坏
//                            Direction attachDir = state.get(CocoaBlock.FACING); // 附着方向
//                            while (interactionManager.updateBlockBreakingProgress(mutable, attachDir)) {
//                                // 客户端 tick 会累加破坏进度
//                            }
//
//                            // 2️⃣ 补种：需要玩家持有可可豆
//                            ItemStack handStack = player.getOffHandStack(); // 可以用副手
//                            if (handStack.isOf(Items.COCOA_BEANS) && handStack.getCount() > 0) {
//                                BlockHitResult hit = new BlockHitResult(
//                                        Vec3d.ofCenter(mutable),
//                                        attachDir,
//                                        mutable.toImmutable(),
//                                        false
//                                );
//                                interactionManager.interactBlock(player, Hand.OFF_HAND, hit);
//                            }
//                        }
//                    }
                    }
                }
            }


        reach = player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
        // 副手是剪刀时，尝试剪附近的羊
        ItemStack offhand = player.getOffHandStack();
        if (offhand.isOf(Items.SHEARS)) {
            // 使用玩家交互范围作为判定半径
            Box box = player.getBoundingBox().expand(reach);
            var sheepList = client.world.getEntitiesByClass(SheepEntity.class, box, s -> !s.isSheared() && !s.isBaby());

            for (SheepEntity sheep : sheepList) {
                // 模拟右键互动
                client.interactionManager.interactEntity(player, sheep, Hand.OFF_HAND);
            }
        }
//        if (Set.of(
//                Items.WOODEN_SWORD,
//                Items.STONE_SWORD,
//                Items.IRON_SWORD,
//                Items.GOLDEN_SWORD,
//                Items.DIAMOND_SWORD,
//                Items.NETHERITE_SWORD
//            ).contains(player.getMainHandStack().getItem())
//        ) {
//            Box box = player.getBoundingBox().expand(reach);
//            var toAttack = client.world.getEntitiesByClass(Entity.class, box, s -> s instanceof Monster || s instanceof HostileEntity);
//
//            for (var beingAttack : toAttack) {
//                client.interactionManager.attackEntity(player, beingAttack);
//            }
//        }
    }

    public static final AtomicLong considerReleaseShieldTimestamp = new AtomicLong(0);

    public static void autoAttackLoop(MinecraftClient client){

        World world = client.world;
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (player == null) return;
        if (player.getAttackCooldownProgress(0) < 1.f){
            return;
        }
        if (world == null) return;
        if (interactionManager == null) return;

        double reach = player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);

        Box box = player.getBoundingBox().expand(reach);
        var toAttack = world.getEntitiesByClass(Entity.class, box, s ->
                {
                    if (!(s instanceof LivingEntity livingEntity)) return false;
                    if (livingEntity.hurtTime > 0){
                        return false;
                    }
                    if (!((livingEntity instanceof Monster || livingEntity instanceof HostileEntity) && s.isAlive())){
                        return false;
                    }
                    return true;
                }
        );

        for (var beingAttack : toAttack) {
            interactionManager.attackEntity(player, beingAttack);
        }
    }


    private static Item seedFromBlock(Block block) {
        if (block == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS) return Items.CARROT;
        if (block == Blocks.POTATOES) return Items.POTATO;
        if (block == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        if (block == Blocks.COCOA) return Items.COCOA_BEANS;
        if (block == Blocks.NETHER_WART) return Items.NETHER_WART;
        return null;
    }
    private static boolean equipSeedToOffhand(ClientPlayerEntity player, Item neededSeed, MinecraftClient client) {
        if (neededSeed == null) return false;

        if (player.getOffHandStack().isOf(neededSeed)) return true;

        return equipToHandIf((stack -> Objects.equals(stack.getItem(),neededSeed)),client,Hand.OFF_HAND);
    }

    private static boolean hasWaterBelow(PlayerEntity player, World world) {
        BlockPos pos = player.getBlockPos();
        for (int i = 0; i < 3; i++) {
            BlockState s = world.getBlockState(pos.down(i));
            if (s.getFluidState().isIn(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSolidForWaterPlace(BlockState state) {
        Block block = state.getBlock();

        // 不是空气
        if (state.isAir()) return false;

        // 不是流体（例如水方块 / 岩浆方块）
        if (!state.getFluidState().isEmpty()) return false;

        return true;
    }


    private static final AtomicReference<ItemStack> storedStackToTurnBack = new AtomicReference<>();
    public static void checkIfPlaceWater(MinecraftClient mc){
        ClientPlayerEntity player = mc.player;
        World world = mc.world;
        ClientPlayerInteractionManager interactionManager = mc.interactionManager;
        ClientCommonNetworkHandler networkHandler = mc.getNetworkHandler();
        if (player == null || world == null || interactionManager == null || networkHandler == null) return;
        if (world.getDimension().ultrawarm()){return;}

        double reach = player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE) - 1;

        BlockPos pos = player.getBlockPos();

        int radius = (int) Math.ceil(reach);

        if (hasWaterBelow(player, world)) {
            return;
        }

        if (player.fallDistance < 3.) {
            return;
        }

        ItemStack offHandStack = player.getOffHandStack();
        for (ItemStack probablyWaterBucket :player.currentScreenHandler.getStacks()){
            if (Objects.equals(probablyWaterBucket.getItem(),Items.WATER_BUCKET)) {
                AtomicInteger slot = new AtomicInteger(InventoryUtils.findSlotWithItem(player.currentScreenHandler, probablyWaterBucket, true, false));
                if (!player.getOffHandStack().isOf(Items.WATER_BUCKET)) {
                    storedStackToTurnBack.set(offHandStack);
                    InventoryUtils.swapSlotToHand(mc, slot.get(),Hand.OFF_HAND);
                    player.sendMessage(Text.of("已找到水桶"),true);//TODO:Translation key
                }

                for (int i = 0; i < 5; i++) {
                    BlockPos pos2 = pos.down(i);
                    BlockState state = world.getBlockState(pos2);
                    if (isSolidForWaterPlace(state)){
//                        mc.player.sendMessage(state.getBlock().getName(),false);

                        boolean wasSneaking = player.isSneaking();
//                        if (!wasSneaking){
//                            forceSneakingFlag.set(true);
//                        }
                        if (state.getBlock() instanceof Waterloggable) {
                            forceSneakingFlag.set(true);

                            runOnNextTickQueue.add(() -> {
                                player.setYaw(+90.F);
                                player.setPitch(+90.F);
                                interactionManager.interactItem(player,Hand.OFF_HAND);
                                runOnNextTickQueue.add(() -> forceSneakingFlag.set(false));

                                mc.player.sendMessage(Text.of("已尝试落地水"),true);//TODO:Translation key

                                //a kind of cheat?
                                networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(player.getEntityPos(),90.F,90.F,false,false));
                                player.setSneaking(wasSneaking);
                                slot.set(InventoryUtils.findSlotWithItem(player.currentScreenHandler, offHandStack, true, false));
                                InventoryUtils.swapSlotToHand(mc, slot.get(),Hand.OFF_HAND);

                                runOnNextTickQueue.add(() -> {

                                    for (ItemStack stack1:player.currentScreenHandler.getStacks()){
                                        if (stack1.isOf(Items.BUCKET)){
                                            int emptyBucketSlot = InventoryUtils.findSlotWithItem(player.currentScreenHandler, stack1, true, false);
                                            InventoryUtils.swapSlotToHand(mc,emptyBucketSlot,Hand.OFF_HAND);
                                            runOnNextTickQueue.add(() -> {
                                                player.setYaw(+90.F);
                                                player.setPitch(+90.F);
                                                interactionManager.interactItem(player, Hand.OFF_HAND);
                                            });


                                        }
                                    }
                                    runOnNextTickQueue.add(() -> recoverItemForWaterPlacing(mc, player));
                                });
                            });
                        }else {
                            player.setYaw(+90.F);
                            player.setPitch(+90.F);
                            interactionManager.interactItem(player,Hand.OFF_HAND);

                            player.sendMessage(Text.of("已尝试落地水"),true);//TODO:Translation

                            //a kind of cheat?
                            networkHandler.sendPacket(
                                    new PlayerMoveC2SPacket.Full(player.getEntityPos(),90.F,90.F,false,false)
                            );
                            player.setSneaking(wasSneaking);
                            slot.set(InventoryUtils.findSlotWithItem(player.currentScreenHandler, offHandStack, true, false));
                            InventoryUtils.swapSlotToHand(mc, slot.get(),Hand.OFF_HAND);
                            runOnNextTickQueue.add(() -> {

                                for (ItemStack stack1:player.currentScreenHandler.getStacks()){
                                    if (stack1.isOf(Items.BUCKET)){
                                        int emptyBucketSlot = InventoryUtils.findSlotWithItem(player.currentScreenHandler, stack1, true, false);
                                        InventoryUtils.swapSlotToHand(mc,emptyBucketSlot,Hand.OFF_HAND);
                                        runOnNextTickQueue.add(() -> {
                                            player.setYaw(+90.F);
                                            player.setPitch(+90.F);
                                            interactionManager.interactItem(player,Hand.OFF_HAND);
                                        });
                                    }
                                }
                                runOnNextTickQueue.add(() -> recoverItemForWaterPlacing(mc, player));
                            });
                        }


                        return;
                    }
                }
            }
        }
    }

    private static void recoverItemForWaterPlacing(MinecraftClient mc, ClientPlayerEntity player) {
        runOnNextTickQueue.add(() -> runOnNextTickQueue.add(() -> {
            ItemStack toTurnBack = storedStackToTurnBack.get();
            storedStackToTurnBack.set(ItemStack.EMPTY);
            if (toTurnBack != null && !toTurnBack.isEmpty()) {
                int slotHere = InventoryUtils.findSlotWithItem(player.currentScreenHandler, toTurnBack, true, false);
                InventoryUtils.swapSlotToHand(mc, slotHere, Hand.OFF_HAND);
            }
        }));
    }

    public static int getBreakTimeTicks(PlayerEntity player, BlockState state, World world, BlockPos pos) {
        float hardness = state.getHardness(world, pos);
        if (hardness < 0) return Integer.MAX_VALUE; // 无法破坏

        float speed = player.getBlockBreakingSpeed(state);
        float breakTimeTicks = hardness * 1.5f / speed;

        // 四舍五入为整数 tick
        return Math.max(1, Math.round(breakTimeTicks));
    }

    @Override
    public void registerModHandlers() {
        MinecraftClient client = MinecraftClient.getInstance();

        ConfigManager.getInstance().registerConfigHandler(MOD_ID, new Configs());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(MOD_ID, MOD_NAME, GuiConfigs::new)
        );
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerMouseInputHandler(InputHandler.getInstance());

        IRenderer renderer = new RenderHandler();
        RenderEventHandler.getInstance().registerGameOverlayRenderer(renderer);
        RenderEventHandler.getInstance().registerTooltipLastRenderer(renderer);
        RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);

        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());

        Callbacks.init(client);
    }
}
