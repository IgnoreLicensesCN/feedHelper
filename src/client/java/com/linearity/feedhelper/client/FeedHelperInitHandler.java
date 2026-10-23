package com.linearity.feedhelper.client;

import com.linearity.feedhelper.client.event.ClientTickHandler;
import com.linearity.feedhelper.client.event.InputHandler;
import com.linearity.feedhelper.config.Callbacks;
import com.linearity.feedhelper.config.Configs;
import com.linearity.feedhelper.gui.GuiConfigs;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

import static com.linearity.feedhelper.client.FeedhelperClient.MOD_ID;

public class FeedHelperInitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(MOD_ID, new Configs());

        Minecraft client = Minecraft.getInstance();

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerMouseInputHandler(InputHandler.getInstance());
        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());

        Callbacks.init(client);
    }

    @Override
    public void preGameInit(Path runDir) {
        IInitializationHandler.super.preGameInit(runDir);
    }
}
