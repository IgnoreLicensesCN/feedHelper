package com.linearity.feedhelper.config;

import com.linearity.feedhelper.gui.GuiConfigs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.Minecraft;

import static com.linearity.feedhelper.client.FeedhelperClient.*;
import static com.linearity.feedhelper.config.Hotkeys.*;

public class Callbacks {

    public static void init(Minecraft client) {
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
        ATTACK_HEALTH_LOWEST_GROWN_ANIMAL.getKeybind().setCallback(((keyAction, iKeybind) -> {
            tryAttackHealthLowestGrownAnimal(client);
            return true;
        }));
    }
}
