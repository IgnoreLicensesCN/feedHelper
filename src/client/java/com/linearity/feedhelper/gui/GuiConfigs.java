package com.linearity.feedhelper.gui;

import com.google.common.collect.ImmutableList;
import com.linearity.feedhelper.client.FeedhelperClient;
import com.linearity.feedhelper.config.Configs;
import com.linearity.feedhelper.config.FeatureToggle;
import com.linearity.feedhelper.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.config.options.BooleanHotkeyGuiWrapper;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.tweakeroo.Reference;
import net.minecraft.client.gui.screen.Screen;

import java.util.Collections;
import java.util.List;

import static com.linearity.feedhelper.client.FeedhelperClient.MOD_ID;


//from tweakeroo stealing code
public class GuiConfigs extends GuiConfigsBase {
    
    public static ImmutableList<FeatureToggle> TWEAK_LIST;
    public static ImmutableList<IHotkeyTogglable> YEET_LIST;
    private static GuiConfigs.ConfigGuiTab tab;

    public GuiConfigs()
    {
        super(10, 50, Reference.MOD_ID, null, MOD_ID+".gui.title.configs", String.format("%s", Reference.MOD_VERSION));
    }
    public void initGui() {
        super.initGui();
        this.clearOptions();
        int x = 10;
        int y = 26;

        for(GuiConfigs.ConfigGuiTab tab : GuiConfigs.ConfigGuiTab.values()) {
            x += this.createButton(x, y, -1, tab);
        }

    }

    private int createButton(int x, int y, int width, GuiConfigs.ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName(), new String[0]);
        button.setEnabled(GuiConfigs.tab != tab);
        this.addButton(button, new GuiConfigs.ButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    protected int getConfigWidth() {
        return 260;
//        GuiConfigs.ConfigGuiTab tab = GuiConfigs.tab;
//        if (tab == GuiConfigs.ConfigGuiTab.GENERIC) {
//            return 170;
//        } else if (tab == GuiConfigs.ConfigGuiTab.FIXES) {
//            return 60;
//        } else {
//            return tab == GuiConfigs.ConfigGuiTab.LISTS ? 200 : 260;
//        }
    }

    protected boolean useKeybindSearch() {
        return tab == GuiConfigs.ConfigGuiTab.TWEAKS
                || tab == GuiConfigs.ConfigGuiTab.GENERIC_HOTKEYS
//                || tab == GuiConfigs.ConfigGuiTab.DISABLES
                ;
    }

    public List<GuiConfigsBase.ConfigOptionWrapper> getConfigs() {
        GuiConfigs.ConfigGuiTab tab = GuiConfigs.tab;
        List<? extends IConfigBase> configs;
        if (tab == GuiConfigs.ConfigGuiTab.TWEAKS) {
            return ConfigOptionWrapper.createFor(TWEAK_LIST.stream().map(this::wrapConfig).toList());
        }

        if (tab == GuiConfigs.ConfigGuiTab.GENERIC) {
            configs = Configs.Generic.OPTIONS;
            return ConfigOptionWrapper.createFor(configs);
        }

        if (tab != GuiConfigs.ConfigGuiTab.GENERIC_HOTKEYS) {
            return Collections.emptyList();
        }

        configs = Hotkeys.HOTKEY_LIST;
//        else if (tab == GuiConfigs.ConfigGuiTab.FIXES) {
//            configs = Configs.Fixes.OPTIONS;
//        } else if (tab == GuiConfigs.ConfigGuiTab.LISTS) {
//            configs = Configs.Lists.OPTIONS;
//        } else {
//            if (tab == GuiConfigs.ConfigGuiTab.DISABLES) {
//                return ConfigOptionWrapper.createFor(YEET_LIST);
//            }
//
//            if (tab == GuiConfigs.ConfigGuiTab.TWEAKS) {
//                return ConfigOptionWrapper.createFor(TWEAK_LIST.stream().map(this::wrapConfig).toList());
//            }
//
//            if (tab != GuiConfigs.ConfigGuiTab.GENERIC_HOTKEYS) {
//                return Collections.emptyList();
//            }
//
//            configs = Hotkeys.HOTKEY_LIST;
//        }

        return ConfigOptionWrapper.createFor(configs);
    }

    protected BooleanHotkeyGuiWrapper wrapConfig(FeatureToggle config) {
        return new BooleanHotkeyGuiWrapper(config.getName(), config, config.getKeybind());
    }

    static {
        TWEAK_LIST = FeatureToggle.VALUES;
        YEET_LIST = Configs.Disable.OPTIONS;
        tab = GuiConfigs.ConfigGuiTab.TWEAKS;
    }

    private static class ButtonListener implements IButtonActionListener {
        private final GuiConfigs parent;
        private final GuiConfigs.ConfigGuiTab tab;

        public ButtonListener(GuiConfigs.ConfigGuiTab tab, GuiConfigs parent) {
            this.tab = tab;
            this.parent = parent;
        }

        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            GuiConfigs.tab = this.tab;
            this.parent.reCreateListWidget(); // apply the new config width
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }

    public static enum ConfigGuiTab {
        GENERIC(MOD_ID+".gui.button.config_gui.generic"),
//        FIXES(MOD_ID+".gui.button.config_gui.fixes"),
//        LISTS(MOD_ID+".gui.button.config_gui.lists"),
        TWEAKS(MOD_ID+".gui.button.config_gui.tweaks"),
        GENERIC_HOTKEYS(MOD_ID+".gui.button.config_gui.generic_hotkeys"),
//        DISABLES(MOD_ID+".gui.button.config_gui.disables")
        ;

        private final String translationKey;

        private ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getDisplayName() {
            return StringUtils.translate(this.translationKey, new Object[0]);
        }
    }
}
