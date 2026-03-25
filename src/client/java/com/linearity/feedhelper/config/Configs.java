package com.linearity.feedhelper.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.ActiveMode;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.MessageOutputType;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import static com.linearity.feedhelper.client.FeedhelperClient.LOGGER;
import static com.linearity.feedhelper.client.FeedhelperClient.MOD_ID;

public class Configs implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = MOD_ID+".json";
    private static final String GENERIC_KEY = MOD_ID+".config.generic";
    private static final String FIXES_KEY = MOD_ID+".config.fixes";
    private static final String LISTS_KEY = MOD_ID+".config.lists";
    private static final String DISABLE_KEY = MOD_ID+".config.disable";
    private static final String INTERNAL_KEY = MOD_ID+".config.internal";
    public Configs() {
    }
//    public static ConfigDouble getActiveFlySpeedConfig() {
//        ConfigDouble var10000;
//        switch (Configs.Internal.FLY_SPEED_PRESET.getIntegerValue()) {
//            case 1 -> var10000 = Configs.Generic.FLY_SPEED_PRESET_2;
//            case 2 -> var10000 = Configs.Generic.FLY_SPEED_PRESET_3;
//            case 3 -> var10000 = Configs.Generic.FLY_SPEED_PRESET_4;
//            default -> var10000 = Configs.Generic.FLY_SPEED_PRESET_1;
//        }
//
//        return var10000;
//    }

    public static void loadFromFile() {
        Path configFile = FileUtils.getConfigDirectoryAsPath().resolve(MOD_ID+".json");
        if (Files.exists(configFile, new LinkOption[0]) && Files.isReadable(configFile)) {
            JsonElement element = JsonUtils.parseJsonFileAsPath(configFile);
            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, "Fixes", Fixes.OPTIONS);
                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "GenericHotkeys", Hotkeys.HOTKEY_LIST);
                ConfigUtils.readConfigBase(root, "Internal", Internal.OPTIONS);
                ConfigUtils.readConfigBase(root, "Lists", Lists.OPTIONS);
                ConfigUtils.readHotkeyToggleOptions(root, "DisableHotkeys", "DisableToggles", Disable.OPTIONS);
                ConfigUtils.readHotkeyToggleOptions(root, "TweakHotkeys", "TweakToggles", FeatureToggle.VALUES);
            }
        } else {
            LOGGER.error("loadFromFile(): Failed to load config file '{}'.", configFile.toAbsolutePath());
        }

//        InventoryUtils.setToolSwitchableSlots(Configs.Generic.TOOL_SWITCHABLE_SLOTS.getStringValue());
//        InventoryUtils.setToolSwitchIgnoreSlots(Configs.Generic.TOOL_SWITCH_IGNORED_SLOTS.getStringValue());
//        CachedTagManager.parseSilkTouchOverride(Configs.Lists.SILK_TOUCH_OVERRIDE.getStrings());
//        InventoryUtils.setRepairModeSlots(Configs.Lists.REPAIR_MODE_SLOTS.getStrings());
//        InventoryUtils.setUnstackingItems(Configs.Lists.UNSTACKING_ITEMS.getStrings());
//        InventoryUtils.setWeaponMapping(Configs.Lists.ENTITY_WEAPON_MAPPING.getStrings());
//        PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION.setListType((UsageRestriction.ListType) Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_LIST_TYPE.getOptionListValue());
//        PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION.setListContents(Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings(), Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings());
//        PlacementTweaks.FAST_RIGHT_CLICK_BLOCK_RESTRICTION.setListType((UsageRestriction.ListType) Configs.Lists.FAST_RIGHT_CLICK_BLOCK_LIST_TYPE.getOptionListValue());
//        PlacementTweaks.FAST_RIGHT_CLICK_BLOCK_RESTRICTION.setListContents(Configs.Lists.FAST_RIGHT_CLICK_BLOCK_BLACKLIST.getStrings(), Configs.Lists.FAST_RIGHT_CLICK_BLOCK_WHITELIST.getStrings());
//        PlacementTweaks.FAST_RIGHT_CLICK_ITEM_RESTRICTION.setListType((UsageRestriction.ListType) Configs.Lists.FAST_RIGHT_CLICK_ITEM_LIST_TYPE.getOptionListValue());
//        PlacementTweaks.FAST_RIGHT_CLICK_ITEM_RESTRICTION.setListContents(Configs.Lists.FAST_RIGHT_CLICK_ITEM_BLACKLIST.getStrings(), Configs.Lists.FAST_RIGHT_CLICK_ITEM_WHITELIST.getStrings());
//        PlacementTweaks.FAST_PLACEMENT_ITEM_RESTRICTION.setListType((UsageRestriction.ListType) Configs.Lists.FAST_PLACEMENT_ITEM_LIST_TYPE.getOptionListValue());
//        PlacementTweaks.FAST_PLACEMENT_ITEM_RESTRICTION.setListContents(Configs.Lists.FAST_PLACEMENT_ITEM_BLACKLIST.getStrings(), Configs.Lists.FAST_PLACEMENT_ITEM_WHITELIST.getStrings());
//        PlacementTweaks.HAND_RESTOCK_RESTRICTION.setListType((UsageRestriction.ListType) Configs.Lists.HAND_RESTOCK_LIST_TYPE.getOptionListValue());
//        PlacementTweaks.HAND_RESTOCK_RESTRICTION.setListContents(Configs.Lists.HAND_RESTOCK_BLACKLIST.getStrings(), Configs.Lists.HAND_RESTOCK_WHITELIST.getStrings());
//        MiscTweaks.POTION_RESTRICTION.setListType((UsageRestriction.ListType) Configs.Lists.POTION_WARNING_LIST_TYPE.getOptionListValue());
//        MiscTweaks.POTION_RESTRICTION.setListContents(Configs.Lists.POTION_WARNING_BLACKLIST.getStrings(), Configs.Lists.POTION_WARNING_WHITELIST.getStrings());
//        RenderTweaks.rebuildLists();
//        MiscTweaks.ENTITY_TYPE_ATTACK_RESTRICTION.setListType((UsageRestriction.ListType) Configs.Lists.ENTITY_TYPE_ATTACK_RESTRICTION_LIST_TYPE.getOptionListValue());
//        MiscTweaks.ENTITY_TYPE_ATTACK_RESTRICTION.setListContents(Configs.Lists.ENTITY_TYPE_ATTACK_RESTRICTION_BLACKLIST.getStrings(), Configs.Lists.ENTITY_TYPE_ATTACK_RESTRICTION_WHITELIST.getStrings());
//        if (class_310.method_1551().field_1687 == null) {
//            FeatureToggle.TWEAK_FREE_CAMERA.setBooleanValue(false);
//        }

    }

    public static void saveToFile() {
        Path dir = FileUtils.getConfigDirectoryAsPath();
        if (!Files.exists(dir, new LinkOption[0])) {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        if (Files.isDirectory(dir, new LinkOption[0])) {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Fixes", Fixes.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, "GenericHotkeys", Hotkeys.HOTKEY_LIST);
            ConfigUtils.writeConfigBase(root, "Internal", Internal.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Lists", Lists.OPTIONS);
            ConfigUtils.writeHotkeyToggleOptions(root, "DisableHotkeys", "DisableToggles", Disable.OPTIONS);
            ConfigUtils.writeHotkeyToggleOptions(root, "TweakHotkeys", "TweakToggles", FeatureToggle.VALUES);
            JsonUtils.writeJsonToFileAsPath(root, dir.resolve(MOD_ID+".json"));
        } else {
            LOGGER.error("saveToFile(): Config Folder '{}' does not exist!", dir.toAbsolutePath());
        }

    }

    public void load() {
        loadFromFile();
    }

    public void save() {
        saveToFile();
    }

    public static class Generic {
        public static final ConfigDouble ELYTRA_MAX_SPEED = new ConfigDouble("elytraMaxSpeed",8.).apply(GENERIC_KEY);
        public static final ConfigDouble ELYTRA_MIN_SPEED = new ConfigDouble("elytraMinSpeed",.1).apply(GENERIC_KEY);
        public static final ConfigDouble ELYTRA_SPEED_UP_MULTIPLIER = new ConfigDouble("elytraSpeedUpMultiplier", 1.0526315789473684).apply(GENERIC_KEY);
        public static final ConfigDouble ELYTRA_SPEED_DOWN_MULTIPLIER = new ConfigDouble("elytraSpeedDownMultiplier", 0.95).apply(GENERIC_KEY);
        public static final ConfigDouble AVOID_ELYTRA_COLLISION_MIN_SPEED = new ConfigDouble("avoidElytraCollisionMinSpeed",0.1).apply(GENERIC_KEY);
        public static final ConfigDouble ELYTRA_CREATIVE_FLYING_DIRECTION_SPEED = new ConfigDouble("elytraCreativeFlyingDirectionSpeed",1.2).apply(GENERIC_KEY);
        public static final ConfigDouble ELYTRA_CREATIVE_SPRINT_SPEED_MULTIPLIER = new ConfigDouble("elytraCreativeFlyingSprintSpeedMultiplier",2).apply(GENERIC_KEY);
        public static final ImmutableList<IConfigBase> OPTIONS;
        public static final ImmutableList<IHotkey> HOTKEYS;

        public Generic() {
        }

        static {
//            ACCURATE_PLACEMENT_PROTOCOL_MODE = (ConfigOptionList)(new ConfigOptionList("accuratePlacementProtocolMode", EasyPlacementProtocol.AUTO)).apply("tweakeroo.config.generic");
//            ACCURATE_PLACEMENT_PROTOCOL = (ConfigBoolean)(new ConfigBoolean("accuratePlacementProtocol", true)).apply("tweakeroo.config.generic");
            OPTIONS = ImmutableList.of(
                    ELYTRA_MAX_SPEED,
                    ELYTRA_MIN_SPEED,
                    ELYTRA_SPEED_UP_MULTIPLIER,
                    ELYTRA_SPEED_DOWN_MULTIPLIER,
                    AVOID_ELYTRA_COLLISION_MIN_SPEED,
                    ELYTRA_CREATIVE_FLYING_DIRECTION_SPEED,
                    ELYTRA_CREATIVE_SPRINT_SPEED_MULTIPLIER
            );
            HOTKEYS = ImmutableList.of();
        }
    }

    public static class Fixes {
//        public static final ConfigBoolean STACKABLE_SHULKERS_IN_HOPPER_FIX = (ConfigBoolean)(new ConfigBoolean("stackableShulkersInHopperFix", true)).apply("tweakeroo.config.fixes");
//        public static final ConfigBoolean MAC_HORIZONTAL_SCROLL = (ConfigBoolean)(new ConfigBoolean("macHorizontalScroll", false)).apply("tweakeroo.config.fixes");
//        public static final ConfigBoolean RAVAGER_CLIENT_BLOCK_BREAK_FIX = (ConfigBoolean)(new ConfigBoolean("ravagerClientBlockBreakFix", false)).apply("tweakeroo.config.fixes");
        public static final ImmutableList<IConfigBase> OPTIONS;

        public Fixes() {
        }

        static {
            OPTIONS = ImmutableList.of();
        }
    }

    public static class Lists {
//        public static final ConfigOptionList BLOCK_TYPE_BREAK_RESTRICTION_LIST_TYPE;
//        public static final ConfigStringList BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
//        public static final ConfigStringList BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
        public static final ImmutableList<IConfigBase> OPTIONS;

        public Lists() {
        }

        static {
//            BLOCK_TYPE_BREAK_RESTRICTION_LIST_TYPE = (ConfigOptionList)(new ConfigOptionList("blockTypeBreakRestrictionListType", UsageRestriction.ListType.BLACKLIST)).apply("tweakeroo.config.lists");
//            BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST = (ConfigStringList)(new ConfigStringList("blockTypeBreakRestrictionBlackList", ImmutableList.of("minecraft:budding_amethyst"))).apply("tweakeroo.config.lists");
//            BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST = (ConfigStringList)(new ConfigStringList("blockTypeBreakRestrictionWhiteList", ImmutableList.of())).apply("tweakeroo.config.lists");
            OPTIONS = ImmutableList.of();
        }
    }

    public static class Disable {
//        public static final ConfigBooleanHotkeyed DISABLE_ARMOR_STAND_RENDERING = (new ConfigBooleanHotkeyed("disableArmorStandRendering", false, "")).apply("tweakeroo.config.disable");
//        public static final ConfigBooleanHotkeyed DISABLE_ATMOSPHERIC_FOG = (new ConfigBooleanHotkeyed("disableAtmosphericFog", false, "")).apply("tweakeroo.config.disable");
        public static final ImmutableList<IHotkeyTogglable> OPTIONS;

        public Disable() {
        }

        static {
            OPTIONS = ImmutableList.<IHotkeyTogglable>of();
        }
    }

    public static class Internal {
//        public static final ConfigDouble DARKNESS_SCALE_VALUE_ORIGINAL = (ConfigDouble)(new ConfigDouble("darknessScaleValueOriginal", (double)0.0F, (double)0.0F, (double)1.0F)).apply("tweakeroo.config.internal");
//        public static final ConfigInteger FLY_SPEED_PRESET = (ConfigInteger)(new ConfigInteger("flySpeedPreset", 0, 0, 3)).apply("tweakeroo.config.internal");
//        public static final ConfigDouble GAMMA_VALUE_ORIGINAL = (ConfigDouble)(new ConfigDouble("gammaValueOriginal", (double)0.0F, (double)0.0F, (double)1.0F)).apply("tweakeroo.config.internal");
//        public static final ConfigInteger HOTBAR_SCROLL_CURRENT_ROW = (ConfigInteger)(new ConfigInteger("hotbarScrollCurrentRow", 3, 0, 3)).apply("tweakeroo.config.internal");
//        public static final ConfigDouble SLIME_BLOCK_SLIPPERINESS_ORIGINAL = (ConfigDouble)(new ConfigDouble("slimeBlockSlipperinessOriginal", 0.8, (double)0.0F, (double)1.0F)).apply("tweakeroo.config.internal");
//        public static final ConfigInteger SHULKER_MAX_STACK_SIZE = (ConfigInteger)(new ConfigInteger("shulkerMaxStackSize", 64, 1, 99)).apply("tweakeroo.config.internal");
        public static final ImmutableList<IConfigBase> OPTIONS;

        public Internal() {
        }

        static {
            OPTIONS = ImmutableList.of();
//            OPTIONS = ImmutableList.of(DARKNESS_SCALE_VALUE_ORIGINAL, FLY_SPEED_PRESET, GAMMA_VALUE_ORIGINAL, HOTBAR_SCROLL_CURRENT_ROW, SLIME_BLOCK_SLIPPERINESS_ORIGINAL, SHULKER_MAX_STACK_SIZE);
        }
    }
}
