package com.linearity.feedhelper.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigNotifiable;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import fi.dy.masa.malilib.util.StringUtils;

import static com.linearity.feedhelper.client.FeedhelperClient.LOGGER;
import static com.linearity.feedhelper.client.FeedhelperClient.MOD_ID;
//from tweakeroo stealing code
public enum FeatureToggle implements IHotkeyTogglable, IConfigNotifiable<IConfigBoolean>
{
    PLACE_WATER_PREVENT_DAMAGE  ("placeWaterPreventDamage",         false, ""),
    ACTIVE_DEFENSE  ("activeDefense",         false, ""),
    ACTIVE_DEFENSE_SHIELDING  ("activeDefenseShielding",         false, ""),
    AUTO_ATTACK  ("autoAttack",         false, ""),
    FILL_LAVA  ("fillLava",         false, ""),
    RANGING_SYSTEM  ("rangingSystem",         true, ""),
    ENTITY_GLOWING  ("entityGlowing",         true, ""),
    AUTO_TORCH  ("autoTorch",         false, ""),
    AVOID_BREAK_TORCH_WHEN_HOLD_PICKAXE  ("avoidBreakTorchWhenHoldPickaxe",         false, ""),
    AVOID_ELYTRA_COLLISION("avoidElytraCollision",         false, ""),
    AVOID_ELYTRA_RESISTANCE("avoidElytraResistance",         false, ""),
    DISABLE_FOG("disableDarknessFog",         false, ""),
    AUTO_CHANGE_CROSSBOW("autoChangeCrossbow",         false, ""),
    ;

    public static final ImmutableList<FeatureToggle> VALUES = ImmutableList.copyOf(values());

    private final static String FEATURE_KEY = MOD_ID+ ".config.tweaks";

    private final String name;
    private String comment;
    private String prettyName;
    private String translatedName;
    private final IKeybind keybind;
    private final boolean defaultValueBoolean;
    private final boolean singlePlayer;
    private boolean valueBoolean;
    private IValueChangeCallback<IConfigBoolean> callback;
    private boolean dirty = false;

    FeatureToggle(String name, boolean defaultValue, String defaultHotkey)
    {
        this(name, defaultValue, false, defaultHotkey, KeybindSettings.DEFAULT,
                buildTranslateName(name, "comment"),
                buildTranslateName(name, "prettyName"),
                buildTranslateName(name, "name"));
    }

    FeatureToggle(String name, boolean defaultValue, String defaultHotkey, KeybindSettings settings)
    {
        this(name, defaultValue, false, defaultHotkey, settings,
                buildTranslateName(name, "comment"),
                buildTranslateName(name, "prettyName"),
                buildTranslateName(name, "name"));
    }

    FeatureToggle(String name, boolean defaultValue, boolean singlePlayer, String defaultHotkey)
    {
        this(name, defaultValue, singlePlayer, defaultHotkey, KeybindSettings.DEFAULT,
                buildTranslateName(name, "comment"),
                buildTranslateName(name, "prettyName"),
                buildTranslateName(name, "name"));
    }

    FeatureToggle(String name, boolean defaultValue, String defaultHotkey, String comment, String prettyName, String translatedName)
    {
        this(name, defaultValue, false, defaultHotkey,
                comment,
                prettyName,
                translatedName);
    }

    FeatureToggle(String name, boolean defaultValue, boolean singlePlayer, String defaultHotkey, String comment, String prettyName, String translatedName)
    {
        this(name, defaultValue, singlePlayer, defaultHotkey, KeybindSettings.DEFAULT,
                comment,
                prettyName,
                translatedName);
    }

    // Backwards Compatible constructors - START
    FeatureToggle(String name, boolean defaultValue, String defaultHotkey, String comment)
    {
        this(name, defaultValue, false, defaultHotkey, KeybindSettings.DEFAULT,
                comment,
                buildTranslateName(name, "prettyName"),
                buildTranslateName(name, "name"));
    }

    FeatureToggle(String name, boolean defaultValue, boolean singlePlayer, String defaultHotkey, String comment)
    {
        this(name, defaultValue, singlePlayer, defaultHotkey, KeybindSettings.DEFAULT,
                comment,
                buildTranslateName(name, "prettyName"),
                buildTranslateName(name, "name"));
    }

    FeatureToggle(String name, boolean defaultValue, String defaultHotkey, KeybindSettings settings, String comment)
    {
        this(name, defaultValue, false, defaultHotkey, settings,
                comment,
                buildTranslateName(name, "prettyName"),
                buildTranslateName(name, "name"));
    }

    FeatureToggle(String name, boolean defaultValue, boolean singlePlayer, String defaultHotkey, KeybindSettings settings, String comment)
    {
        this(name, defaultValue, singlePlayer, defaultHotkey, settings,
                comment,
                buildTranslateName(name, "prettyName"),
                buildTranslateName(name, "name"));
    }

    FeatureToggle(String name, boolean defaultValue, String defaultHotkey, String comment, String prettyName)
    {
        this(name, defaultValue, false, defaultHotkey,
                comment,
                prettyName,
                buildTranslateName(name, "name"));
    }

    FeatureToggle(String name, boolean defaultValue, boolean singlePlayer, String defaultHotkey, String comment, String prettyName)
    {
        this(name, defaultValue, singlePlayer, defaultHotkey, KeybindSettings.DEFAULT,
                comment,
                prettyName,
                buildTranslateName(name, "name"));
    }

    FeatureToggle(String name, boolean defaultValue, boolean singlePlayer, String defaultHotkey, KeybindSettings settings, String comment, String prettyName)
    {
        this(name, defaultValue, singlePlayer, defaultHotkey, settings,
                comment,
                prettyName,
                buildTranslateName(name, "name"));
    }
    // Backwards Compatible constructors - END

    FeatureToggle(String name, boolean defaultValue, boolean singlePlayer, String defaultHotkey, KeybindSettings settings, String comment, String prettyName, String translatedName)
    {
        this.name = name;
        this.valueBoolean = defaultValue;
        this.defaultValueBoolean = defaultValue;
        this.singlePlayer = singlePlayer;
        this.comment = comment;
        this.prettyName = prettyName;
        this.translatedName = translatedName;
        this.keybind = KeybindMulti.fromStorageString(defaultHotkey, settings);
        this.keybind.setCallback(new KeyCallbackToggleBooleanConfigWithMessage(this));
    }

    @Override
    public ConfigType getType()
    {
        return ConfigType.HOTKEY;
    }

    @Override
    public String getName()
    {
        if (this.singlePlayer)
        {
            return GuiBase.TXT_GOLD + this.name + GuiBase.TXT_RST;
        }

        return this.name;
    }

    @Override
    public String getConfigGuiDisplayName()
    {
        String name = StringUtils.getTranslatedOrFallback(this.translatedName, this.name);

        if (this.singlePlayer)
        {
            name = GuiBase.TXT_GOLD + name + GuiBase.TXT_RST;
        }

        return name;
    }

    @Override
    public String getPrettyName()
    {
        return StringUtils.getTranslatedOrFallback(this.prettyName,
                !this.prettyName.isEmpty() ? this.prettyName : StringUtils.splitCamelCase(this.name.substring(5)));
    }

    @Override
    public String getTranslatedName()
    {
        String name = StringUtils.getTranslatedOrFallback(this.translatedName, this.name);

        if (this.singlePlayer)
        {
            name = GuiBase.TXT_GOLD + name + GuiBase.TXT_RST;
        }

        return name;
    }

    @Override
    public String getComment()
    {
        String comment = StringUtils.getTranslatedOrFallback(this.comment, this.comment);

        if (comment != null && this.singlePlayer)
        {
            return comment + "\n" + StringUtils.translate(MOD_ID+".label.config_comment.single_player_only");
        }

        //System.out.printf("FeatureToggle#getComment(): comment [%s] // test [%s]\n", this.comment, comment);
        return comment;
    }

    @Override
    public void setPrettyName(String s)
    {
        this.prettyName = s;
    }

    @Override
    public void setTranslatedName(String s)
    {
        this.translatedName = s;
    }

    @Override
    public void setComment(String s)
    {
        this.comment = s;
    }

    @Override
    public boolean isDirty()
    {
        return this.dirty;
    }

    @Override
    public void markDirty()
    {
        this.dirty = true;
    }

    @Override
    public void markClean()
    {
        this.dirty = false;
    }

    @Override
    public void checkIfClean()
    {
        if (this.isDirty())
        {
            this.markClean();
            this.onValueChanged();
        }
    }

    private static String buildTranslateName(String name, String type)
    {
        return FEATURE_KEY + "." + type + "." + name;
    }

    @Override
    public String getStringValue()
    {
        return String.valueOf(this.valueBoolean);
    }

    @Override
    public String getDefaultStringValue()
    {
        return String.valueOf(this.defaultValueBoolean);
    }

    @Override
    public void setValueFromString(String value)
    {
    }

    @Override
    public void onValueChanged()
    {
        if (this.callback != null)
        {
            this.callback.onValueChanged(this);
        }
    }

    @Override
    public void setValueChangeCallback(IValueChangeCallback<IConfigBoolean> callback)
    {
        this.callback = callback;
    }

    @Override
    public IKeybind getKeybind()
    {
        return this.keybind;
    }

    @Override
    public boolean getBooleanValue()
    {
        return this.valueBoolean;
    }

    @Override
    public boolean getDefaultBooleanValue()
    {
        return this.defaultValueBoolean;
    }

    @Override
    public void setBooleanValue(boolean value)
    {
        boolean oldValue = this.valueBoolean;
        this.valueBoolean = value;

        if (oldValue != this.valueBoolean)
        {
            this.onValueChanged();
        }
    }

    /**
     * Toggle ON without triggering a callback (Free Cam Preset Recall)
     */
    public void setEnabledNoCallback()
    {
        if (!this.valueBoolean)
        {
            this.valueBoolean = true;
        }
    }

    @Override
    public boolean isModified()
    {
        return this.valueBoolean != this.defaultValueBoolean;
    }

    @Override
    public boolean isModified(String newValue)
    {
        return Boolean.parseBoolean(newValue) != this.defaultValueBoolean;
    }

    @Override
    public void resetToDefault()
    {
        boolean oldValue = this.valueBoolean;
        this.valueBoolean = this.defaultValueBoolean;

        if (oldValue != this.valueBoolean)
        {
            this.onValueChanged();
        }
    }

    @Override
    public JsonElement getAsJsonElement()
    {
        return new JsonPrimitive(this.valueBoolean);
    }

    @Override
    public void setValueFromJsonElement(JsonElement element)
    {
        try
        {
            if (element.isJsonPrimitive())
            {
                this.setBooleanValue(element.getAsBoolean());
            }
            else
            {
                LOGGER.warn("Failed to set config value for '{}' from the JSON element '{}'", this.getName(), element);
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("Failed to set config value for '{}' from the JSON element '{}'", this.getName(), element, e);
        }
    }
}

