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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
            var client = Minecraft.getInstance();
            var matrices = context.matrices();
            float delta = client.getDeltaTracker().getGameTimeDeltaTicks();

            if (FeatureToggle.RANGING_SYSTEM.getBooleanValue()){
                rangingSystemLoopRendering(client, matrices, delta);
            }
        });
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.INFO_BAR,
                Identifier.fromNamespaceAndPath("feedhelper", "debug_rect"),
                (drawContext, renderTickCounter) -> {
                    var client = Minecraft.getInstance();
                    float delta = client.getDeltaTracker().getGameTimeDeltaTicks();

                    if (FeatureToggle.RANGING_SYSTEM.getBooleanValue()){
                        rangingSystemLoopRenderingOnHUD(client, drawContext, delta);
                    }
                }
        );
    }

    public static void fillLavaLoop(Minecraft client) {
        var player = client.player;
        var world = client.level;
        var interactionManager = client.gameMode;
        if (player == null || world == null || interactionManager == null) return;
        InteractionHand hand = InteractionHand.OFF_HAND;
        ItemStack handStack = player.getOffhandItem();
        if (handStack == null || handStack.isEmpty()) {
            hand = InteractionHand.MAIN_HAND;
        }
        handStack = player.getMainHandItem();
        if (handStack == null || handStack.isEmpty()) {
            return;
        }
        Set<BlockPos> placedPos = new HashSet<>();
        if (handStack.getItem() instanceof BlockItem blockItem) {
            double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) - 1.2;
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
                    if (centerState.getFluidState().is(FluidTags.LAVA)){continue;}
                    BlockHitResult hitResult;
                    Tuple<BlockPos,Direction>[] pairs = new Tuple[]{
                            new Tuple(center.above(),Direction.UP),
                            new Tuple(center.below(),Direction.DOWN),
                            new Tuple(center.north(),Direction.NORTH),
                            new Tuple(center.south(),Direction.SOUTH),
                            new Tuple(center.west(),Direction.WEST),
                            new Tuple(center.east(),Direction.EAST),
                    };
                    for (Tuple<BlockPos,Direction> pair : pairs) {
                        if (placedPos.contains(pair.getA())) {
                            continue;
                        }
                        if (!center.closerToCenterThan(player.position(),radius)){
                            continue;
                        }
                        if (world.getBlockState(pair.getA()).getFluidState().is(FluidTags.LAVA)) {
                            hitResult = new BlockHitResult(
                                    pair.getA().getCenter(),
                                    pair.getB(),
                                    center,
                                    false
                            );
                            interactionManager.useItemOn(player,hand,hitResult);
                            placedPos.add(pair.getA());
                        }
                    }
                }
            }
        }
    }

    public static void autoTorchLoop(Minecraft client) {
        var player = client.player;
        var world = client.level;
        var interactionManager = client.gameMode;
        if (player == null || world == null || interactionManager == null) return;

        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack handStack = player.getMainHandItem();
        if (handStack == null || handStack.isEmpty() || !handStack.is(Items.TORCH)) {
            handStack = player.getOffhandItem();
            if (handStack == null || handStack.isEmpty() || !handStack.is(Items.TORCH)) {
                return; // 双手都没有火把
            }
            hand = InteractionHand.OFF_HAND;
        }

        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) - 1.2;
        int radius = (int) Math.ceil(reach);

        // 扫描范围
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos center = player.blockPosition().offset(x, y, z);
                    if (!center.closerToCenterThan(player.position(), reach)) continue;

                    BlockState centerState = world.getBlockState(center);
                    if (centerState.getFluidState().is(FluidTags.LAVA) || centerState.getFluidState().is(FluidTags.WATER)) continue;

                    // 周围六个方向尝试放火把
                    for (Direction dir : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                        BlockPos targetPos = center.relative(dir);
                        BlockState targetState = world.getBlockState(targetPos);

                        if (!targetState.isAir()) continue; // 目标必须是空气
                        int light = world.getBrightness(LightLayer.BLOCK, targetPos);

// 如果当前格亮度为 0，检查周围是否已经是亮的
                        if (light == 0) {
                            boolean neighborBright = false;

                            for (Direction checkDir : Direction.values()) {
                                BlockPos np = targetPos.relative(checkDir);
                                int nl = world.getBrightness(LightLayer.BLOCK, np);

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
                                    targetPos.getCenter(),
                                    dir,
                                    center,
                                    false
                            );
                            boolean noMoreTorchOnHand = false;
                            if (handStack.getCount() == 1){
                                noMoreTorchOnHand = true;
                            }

                            interactionManager.useItemOn(player, hand, hitResult);
                            if (noMoreTorchOnHand) {
                                equipToHandIf((stack -> Objects.equals(stack.getItem(),Items.TORCH)),client,InteractionHand.OFF_HAND);
                            }

                            return; // 放置成功就结束本刻循环
                        }
                    }
                }
            }
        }
    }

    public static void tryFeedNearbyAnimals(Minecraft client) {
        var player = client.player;
        var world = client.level;
        var interactionManager = client.gameMode;
        if (player == null || world == null || interactionManager == null) return;


// player interaction reach (client-side approximation)
        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

// Box around player
        AABB box = player.getBoundingBox().inflate(reach);
        var animals = world.getEntitiesOfClass(Animal.class, box, a -> true);


        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;


        for (Animal animal : animals) {
            if (!animal.isFood(held)) continue;
            if (animal.isBaby() || animal.isInLove()) continue;


// simulate interact
            interactionManager.interact(player, animal, InteractionHand.MAIN_HAND);
//            return; //feed a lot
        }
    }

    public static void tryAttackHealthLowestGrownAnimal(Minecraft client) {
        var player = client.player;
        var world = client.level;
        var interactionManager = client.gameMode;
        if (player == null || world == null || interactionManager == null) return;


// player interaction reach (client-side approximation)
        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

// Box around player
        AABB box = player.getBoundingBox().inflate(reach);
        var animals = world.getEntitiesOfClass(Animal.class, box, animal -> !((animal.isBaby())));


        if (animals.isEmpty()) return;
        animals.sort((animal1,animal2)-> Float.compare(animal1.getHealth(), animal2.getHealth()));

        var animal = animals.getFirst();
        interactionManager.attack(player, animal);
    }

    public static void tryFarm(Minecraft client) {
        var player = client.player;
        var world = client.level;
        var interactionManager = client.gameMode;
        if (player == null || world == null || interactionManager == null) return;

        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) - 1;

        BlockPos playerPos = player.blockPosition();

        int radius = (int) Math.ceil(reach);

        // 枚举附近方块
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

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
                                            entry.getKey().above().getCenter(),
                                            Direction.UP,
                                            entry.getKey(),
                                            false
                                    );
                                    interactionManager.useItemOn(
                                            player,
                                            InteractionHand.OFF_HAND,
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
                            if (!crop.isMaxAge(state)) continue;
                            BlockPos cropPos = mutable.immutable();

                            // ───── 自动收割（破坏 mature 作物） ─────
                            interactionManager.startDestroyBlock(mutable, player.getDirection());

                            // ───── 自动补种（使用种子） ─────
                            // 储存副手物品
                            ItemStack originalOffhand = player.getOffhandItem().copy();

// equip seed → 放到副手
                            BlockHitResult hit = new BlockHitResult(
                                    Vec3.atCenterOf(cropPos),
                                    Direction.UP,
                                    cropPos.below(),
                                    false
                            );
                            if (equipSeedToOffhand(player, seedFromBlock(state.getBlock()),client)) {
                                interactionManager.useItemOn(
                                        player,
                                        InteractionHand.OFF_HAND,
                                        hit
                                );
                            }else {
                                placesToPlantWithItems.put(cropPos.below(),seedFromBlock(state.getBlock()));
                            }

// restore → 补回去
//                        restoreOffhand(player, originalOffhand);
                        }
                        else if (block instanceof NetherWartBlock netherWart) {
                            if (state.getValue(NetherWartBlock.AGE) != NetherWartBlock.MAX_AGE){
                                continue;
                            }
                            BlockPos cropPos = mutable.immutable();

                            // ───── 自动收割（破坏 mature 作物） ─────
                            interactionManager.startDestroyBlock(mutable, player.getDirection());

                            // ───── 自动补种（使用种子） ─────
                            // 储存副手物品
                            ItemStack originalOffhand = player.getOffhandItem().copy();

// equip seed → 放到副手
                            BlockHitResult hit = new BlockHitResult(
                                    Vec3.atCenterOf(cropPos),
                                    Direction.UP,
                                    cropPos.below(),
                                    false
                            );
                            if (equipSeedToOffhand(player, seedFromBlock(state.getBlock()),client)) {
                                interactionManager.useItemOn(
                                        player,
                                        InteractionHand.OFF_HAND,
                                        hit
                                );
                            }
                            else {
                                placesToPlantWithItems.put(cropPos.below(),seedFromBlock(state.getBlock()));
                            }

// restore → 补回去
//                        restoreOffhand(player, originalOffhand);
                        }
                        else if (block == Blocks.SUGAR_CANE) {
                            BlockPos below = mutable.below();
                            BlockPos twoBelow = below.below();

                            BlockState belowState = world.getBlockState(below);
                            BlockState twoBelowState = world.getBlockState(twoBelow);

                            // 当前甘蔗上方没有甘蔗，且下方是甘蔗，破坏当前甘蔗
                            // 或者：当前方块是甘蔗，下面是甘蔗，下面的下面不是甘蔗
                            if (belowState.getBlock() == Blocks.SUGAR_CANE && twoBelowState.getBlock() != Blocks.SUGAR_CANE) {
                                interactionManager.startDestroyBlock(mutable, player.getDirection());
                            }
                        }
                        else if (block instanceof SweetBerryBushBlock sweetBerryBush) {
                            // 成熟判断
                            int age = state.getValue(SweetBerryBushBlock.AGE);
                            if (age >= SweetBerryBushBlock.MAX_AGE) {
                                // 成熟了，可以右键收获
                                BlockHitResult hit = new BlockHitResult(
                                        Vec3.atCenterOf(mutable),
                                        Direction.UP,
                                        mutable.immutable(),
                                        false
                                );
                                interactionManager.useItemOn(player, InteractionHand.MAIN_HAND, hit);
                            }
                        }

                        if (player.getMainHandItem().is(ItemTags.HOES)){
                            if (block instanceof LeavesBlock leavesBlock  && getBreakTimeTicks(player,state,world,mutable) == 1) {
                                interactionManager.startDestroyBlock(mutable, player.getDirection());
                            }
                        }
                        if (y >= 0){
                            if (player.getMainHandItem().is(ItemTags.SHOVELS)) {
                                if (Objects.equals(player.getOffhandItem().getItem(),block.asItem())
                                        && getBreakTimeTicks(player,state,world,mutable) == 1
                                ) {
                                    interactionManager.startDestroyBlock(mutable, player.getDirection());
                                }
                            }
                            if (player.getMainHandItem().is(ItemTags.PICKAXES)) {
                                if (
                                        Objects.equals(player.getOffhandItem().getItem(),block.asItem())
                                                && getBreakTimeTicks(player,state,world,mutable) == 1
                                ) {
                                    interactionManager.startDestroyBlock(mutable, player.getDirection());
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


        reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        // 副手是剪刀时，尝试剪附近的羊
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(Items.SHEARS)) {
            // 使用玩家交互范围作为判定半径
            AABB box = player.getBoundingBox().inflate(reach);
            var sheepList = client.level.getEntitiesOfClass(Sheep.class, box, s -> !s.isSheared() && !s.isBaby());

            for (Sheep sheep : sheepList) {
                // 模拟右键互动
                client.gameMode.interact(player, sheep, InteractionHand.OFF_HAND);
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

    public static void autoAttackLoop(Minecraft client){

        Level world = client.level;
        LocalPlayer player = client.player;
        MultiPlayerGameMode interactionManager = client.gameMode;
        if (player == null) return;
        if (player.getAttackStrengthScale(0) < 1.f){
            return;
        }
        if (world == null) return;
        if (interactionManager == null) return;

        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

        AABB box = player.getBoundingBox().inflate(reach);
        var toAttack = world.getEntitiesOfClass(Entity.class, box, s ->
                {
                    if (!(s instanceof LivingEntity livingEntity)) return false;
                    if (livingEntity.hurtTime > 0){
                        return false;
                    }
                    if (!((livingEntity instanceof Enemy || livingEntity instanceof Monster) && s.isAlive())){
                        return false;
                    }
                    return true;
                }
        );

        for (var beingAttack : toAttack) {
            interactionManager.attack(player, beingAttack);
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
    private static boolean equipSeedToOffhand(LocalPlayer player, Item neededSeed, Minecraft client) {
        if (neededSeed == null) return false;

        if (player.getOffhandItem().is(neededSeed)) return true;

        return equipToHandIf((stack -> Objects.equals(stack.getItem(),neededSeed)),client,InteractionHand.OFF_HAND);
    }

    private static boolean hasWaterBelow(Player player, Level world) {
        BlockPos pos = player.blockPosition();
        for (int i = 0; i < 3; i++) {
            BlockState s = world.getBlockState(pos.below(i));
            if (s.getFluidState().is(FluidTags.WATER)) {
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
    public static void checkIfPlaceWater(Minecraft mc){
        LocalPlayer player = mc.player;
        Level world = mc.level;
        MultiPlayerGameMode interactionManager = mc.gameMode;
        ClientCommonPacketListenerImpl networkHandler = mc.getConnection();
        if (player == null || world == null || interactionManager == null || networkHandler == null) return;

        if (world.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)){
            return;
        }

        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) - 1;

        BlockPos pos = player.blockPosition();

        int radius = (int) Math.ceil(reach);

        if (hasWaterBelow(player, world)) {
            return;
        }

        if (player.fallDistance < 3.) {
            return;
        }

        ItemStack offHandStack = player.getOffhandItem();
        for (ItemStack probablyWaterBucket :player.containerMenu.getItems()){
            if (Objects.equals(probablyWaterBucket.getItem(),Items.WATER_BUCKET)) {
                AtomicInteger slot = new AtomicInteger(InventoryUtils.findSlotWithItem(player.containerMenu, probablyWaterBucket, true, false));
                if (!player.getOffhandItem().is(Items.WATER_BUCKET)) {
                    storedStackToTurnBack.set(offHandStack);
                    InventoryUtils.swapSlotToHand(mc, slot.get(),InteractionHand.OFF_HAND);
                    player.displayClientMessage(Component.nullToEmpty("已找到水桶"),true);//TODO:Translation key
                }

                for (int i = 0; i < 5; i++) {
                    BlockPos pos2 = pos.below(i);
                    BlockState state = world.getBlockState(pos2);
                    if (isSolidForWaterPlace(state)){
//                        mc.player.sendMessage(state.getBlock().getName(),false);

                        boolean wasSneaking = player.isShiftKeyDown();
//                        if (!wasSneaking){
//                            forceSneakingFlag.set(true);
//                        }
                        if (state.getBlock() instanceof SimpleWaterloggedBlock) {
                            forceSneakingFlag.set(true);

                            runOnNextTickQueue.add(() -> {
                                player.setYRot(+90.F);
                                player.setXRot(+90.F);
                                interactionManager.useItem(player,InteractionHand.OFF_HAND);
                                runOnNextTickQueue.add(() -> forceSneakingFlag.set(false));

                                mc.player.displayClientMessage(Component.nullToEmpty("已尝试落地水"),true);//TODO:Translation key

                                //a kind of cheat?
                                networkHandler.send(new ServerboundMovePlayerPacket.PosRot(player.position(),90.F,90.F,false,false));
                                player.setShiftKeyDown(wasSneaking);
                                slot.set(InventoryUtils.findSlotWithItem(player.containerMenu, offHandStack, true, false));
                                InventoryUtils.swapSlotToHand(mc, slot.get(),InteractionHand.OFF_HAND);

                                runOnNextTickQueue.add(() -> {

                                    for (ItemStack stack1:player.containerMenu.getItems()){
                                        if (stack1.is(Items.BUCKET)){
                                            int emptyBucketSlot = InventoryUtils.findSlotWithItem(player.containerMenu, stack1, true, false);
                                            InventoryUtils.swapSlotToHand(mc,emptyBucketSlot,InteractionHand.OFF_HAND);
                                            runOnNextTickQueue.add(() -> {
                                                player.setYRot(+90.F);
                                                player.setXRot(+90.F);
                                                interactionManager.useItem(player, InteractionHand.OFF_HAND);
                                            });


                                        }
                                    }
                                    runOnNextTickQueue.add(() -> recoverItemForWaterPlacing(mc, player));
                                });
                            });
                        }else {
                            player.setYRot(+90.F);
                            player.setXRot(+90.F);
                            interactionManager.useItem(player,InteractionHand.OFF_HAND);

                            player.displayClientMessage(Component.nullToEmpty("已尝试落地水"),true);//TODO:Translation

                            //a kind of cheat?
                            networkHandler.send(
                                    new ServerboundMovePlayerPacket.PosRot(player.position(),90.F,90.F,false,false)
                            );
                            player.setShiftKeyDown(wasSneaking);
                            slot.set(InventoryUtils.findSlotWithItem(player.containerMenu, offHandStack, true, false));
                            InventoryUtils.swapSlotToHand(mc, slot.get(),InteractionHand.OFF_HAND);
                            runOnNextTickQueue.add(() -> {

                                for (ItemStack stack1:player.containerMenu.getItems()){
                                    if (stack1.is(Items.BUCKET)){
                                        int emptyBucketSlot = InventoryUtils.findSlotWithItem(player.containerMenu, stack1, true, false);
                                        InventoryUtils.swapSlotToHand(mc,emptyBucketSlot,InteractionHand.OFF_HAND);
                                        runOnNextTickQueue.add(() -> {
                                            player.setYRot(+90.F);
                                            player.setXRot(+90.F);
                                            interactionManager.useItem(player,InteractionHand.OFF_HAND);
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

    private static void recoverItemForWaterPlacing(Minecraft mc, LocalPlayer player) {
        runOnNextTickQueue.add(() -> runOnNextTickQueue.add(() -> {
            ItemStack toTurnBack = storedStackToTurnBack.get();
            storedStackToTurnBack.set(ItemStack.EMPTY);
            if (toTurnBack != null && !toTurnBack.isEmpty()) {
                int slotHere = InventoryUtils.findSlotWithItem(player.containerMenu, toTurnBack, true, false);
                InventoryUtils.swapSlotToHand(mc, slotHere, InteractionHand.OFF_HAND);
            }
        }));
    }

    public static int getBreakTimeTicks(Player player, BlockState state, Level world, BlockPos pos) {
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness < 0) return Integer.MAX_VALUE; // 无法破坏

        float speed = player.getDestroySpeed(state);
        float breakTimeTicks = hardness * 1.5f / speed;

        // 四舍五入为整数 tick
        return Math.max(1, Math.round(breakTimeTicks));
    }

    @Override
    public void registerModHandlers() {
        Minecraft client = Minecraft.getInstance();

        ConfigManager.getInstance().registerConfigHandler(MOD_ID, new Configs());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(MOD_ID, MOD_NAME, GuiConfigs::new)
        );
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerMouseInputHandler(InputHandler.getInstance());

//        IRenderer renderer = new RenderHandler();
//        RenderEventHandler.getInstance().registerGameOverlayRenderer(renderer);
//        RenderEventHandler.getInstance().registerTooltipLastRenderer(renderer);
//        RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);

        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());

        Callbacks.init(client);
    }
}
