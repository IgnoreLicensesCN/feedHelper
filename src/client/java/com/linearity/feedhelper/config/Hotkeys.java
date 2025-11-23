package com.linearity.feedhelper.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

import java.util.List;

import static com.linearity.feedhelper.client.FeedhelperClient.MOD_ID;

public class Hotkeys
{
    private static final String HOTKEY_KEY = MOD_ID+".config.hotkey";
    public static final ConfigHotkey FARM                  = new ConfigHotkey("farm",                  "G", KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEY_KEY);
    public static final ConfigHotkey FEED                  = new ConfigHotkey("feed",                  "H", KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEY_KEY);
    public static final ConfigHotkey OPEN_CONFIG_GUI                    = new ConfigHotkey("openConfigGui",                     "X,V").apply(HOTKEY_KEY);
    public static final ConfigHotkey ELYTRA_SPEED_UP                  = new ConfigHotkey("elytraSpeedUp",                  "", KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEY_KEY);
    public static final ConfigHotkey ELYTRA_SPEED_DOWN                  = new ConfigHotkey("elytraSpeedDown",                  "", KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEY_KEY);

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            FARM,
            FEED,
            OPEN_CONFIG_GUI,
            ELYTRA_SPEED_UP,
            ELYTRA_SPEED_DOWN
    );
}
