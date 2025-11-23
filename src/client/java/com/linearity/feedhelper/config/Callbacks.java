package com.linearity.feedhelper.config;

import com.linearity.feedhelper.gui.GuiConfigs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.MinecraftClient;

import static com.linearity.feedhelper.client.FeedhelperClient.tryFarm;
import static com.linearity.feedhelper.client.FeedhelperClient.tryFeedNearbyAnimals;
import static com.linearity.feedhelper.config.Hotkeys.*;

public class Callbacks {

    public static void init(MinecraftClient client) {
        OPEN_CONFIG_GUI.getKeybind().setCallback((keyAction, iKeybind) -> {
            GuiBase.openGui(new GuiConfigs());
            return true;
        });
        FARM.getKeybind().setCallback((keyAction, iKeybind) -> {
            tryFarm(client);
            return true;
        });
        FEED.getKeybind().setCallback(((keyAction, iKeybind) -> {
            tryFeedNearbyAnimals(client);
            return true;
        }));
    }
}
