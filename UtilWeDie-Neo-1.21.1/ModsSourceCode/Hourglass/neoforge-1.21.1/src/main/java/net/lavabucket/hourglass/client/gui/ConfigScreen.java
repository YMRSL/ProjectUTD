/*
 * Copyright (C) 2021 Nick Iacullo
 *
 * This file is part of Hourglass.
 *
 * Hourglass is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hourglass is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Hourglass.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.lavabucket.hourglass.client.gui;

import static net.lavabucket.hourglass.config.HourglassConfig.CLIENT_CONFIG;
import static net.lavabucket.hourglass.wrappers.TextWrapper.translation;

import java.util.Arrays;

import com.mojang.serialization.Codec;

import net.lavabucket.hourglass.wrappers.TextWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Mod configuration screen, accessed from the mod list in the main menu.
 */
public final class ConfigScreen extends OptionsSubScreen {

    private static final String KEY_TITLE = "hourglass.configgui.title";
    private static final String KEY_CLOCK_ALIGNMENT = "hourglass.configgui.clockAlignment";
    private static final String KEY_CLOCK_SCALE = "hourglass.configgui.clockScale";
    private static final String KEY_CLOCK_MARGIN = "hourglass.configgui.clockMargin";
    private static final String KEY_PREVENT_CLOCK_WOBBLE = "hourglass.configgui.preventClockWobble";
    private static final String KEY_PIXELS = "hourglass.configgui.pixels";
    private static final String KEY_GENERIC_OPTION = "options.generic_value";

    private ScreenAlignment clockAlignment;
    private int clockScale;
    private int clockMargin;
    private boolean preventClockWobble;

    /**
     * Registers this screen as the mod's config screen via the NeoForge extension point.
     * @param modContainer  the mod container provided at construction time
     */
    public static void register(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, modListScreen) -> new ConfigScreen(modListScreen));
    }

    /**
     * Instantiates a new {@code ConfigScreen} object.
     * @param lastScreen  the screen that was active prior to this screen opening
     */
    public ConfigScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, translation(KEY_TITLE).get());
        fetchSettings();
    }

    @Override
    protected void addOptions() {
        this.list.addBig(new OptionInstance<>(
                KEY_CLOCK_ALIGNMENT,
                OptionInstance.noTooltip(),
                (caption, value) -> translation(value.getKey()).get(),
                new OptionInstance.Enum<>(Arrays.asList(ScreenAlignment.values()),
                        Codec.STRING.xmap(ScreenAlignment::valueOf, Enum::name)),
                clockAlignment,
                value -> clockAlignment = value));

        this.list.addBig(new OptionInstance<Double>(
                KEY_CLOCK_SCALE,
                OptionInstance.noTooltip(),
                (caption, value) -> pixelOptionText(KEY_CLOCK_SCALE, value).get(),
                (new OptionInstance.IntRange(0, 128)).xmap((value) -> (double) value, (value) -> value.intValue()),
                Codec.doubleRange(0.0D, 128.0D),
                Double.valueOf(clockScale),
                value -> clockScale = value.intValue()));

        this.list.addBig(new OptionInstance<Double>(
                KEY_CLOCK_MARGIN,
                OptionInstance.noTooltip(),
                (caption, value) -> pixelOptionText(KEY_CLOCK_MARGIN, value).get(),
                (new OptionInstance.IntRange(0, 128)).xmap((value) -> (double) value, (value) -> value.intValue()),
                Codec.doubleRange(0.0D, 128.0D),
                Double.valueOf(clockMargin),
                value -> clockMargin = value.intValue()));

        this.list.addBig(OptionInstance.createBoolean(
                KEY_PREVENT_CLOCK_WOBBLE,
                preventClockWobble,
                value -> preventClockWobble = value));
    }

    @Override
    public void onClose() {
        saveSettings();
        super.onClose();
    }

    @Override
    public void removed() {
        // Avoid the vanilla OptionsSubScreen behaviour of saving the game options file.
    }

    private void fetchSettings() {
        clockAlignment = CLIENT_CONFIG.clockAlignment.get();
        clockScale = CLIENT_CONFIG.clockScale.get();
        clockMargin = CLIENT_CONFIG.clockMargin.get();
        preventClockWobble = CLIENT_CONFIG.preventClockWobble.get();
    }

    private void saveSettings() {
        CLIENT_CONFIG.clockAlignment.set(clockAlignment);
        CLIENT_CONFIG.clockScale.set(clockScale);
        CLIENT_CONFIG.clockMargin.set(clockMargin);
        CLIENT_CONFIG.preventClockWobble.set(preventClockWobble);
    }

    /**
     * Returns a wrapped translatable text component for a generic option that includes a pixel
     * count.
     *
     * @param key  the translation key for the option
     * @param pixelCount  the pixel count to display
     * @return the new wrapped text component
     */
    public static TextWrapper pixelOptionText(String key, double pixelCount) {
        return translation(KEY_GENERIC_OPTION,
                translation(key).get(),
                translation(KEY_PIXELS, (int) pixelCount).get());
    }

}
