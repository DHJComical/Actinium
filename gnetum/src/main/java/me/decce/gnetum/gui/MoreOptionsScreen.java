package me.decce.gnetum.gui;

import me.decce.gnetum.Gnetum;
import me.decce.gnetum.GnetumConfig;
import me.decce.gnetum.PerformanceAnalyzer;
import me.decce.gnetum.gui.widgets.GuiButtonEx;
import me.decce.gnetum.gui.widgets.IntSlider;
import me.decce.gnetum.gui.widgets.ToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class MoreOptionsScreen extends BaseScreen {
    public static final int BTN_SHOW_FPS = 1;
    public static final int SLIDER_NUMBER_OF_PASSES = 2;
    public static final int SLIDER_MAX_FPS = 3;
    public static final int BTN_RESET = 4;
    private ToggleButton btnShowFps;
    private IntSlider sliderNumberOfPasses;
    private IntSlider sliderMaxFps;
    private GuiButtonEx btnReset;

    @Override
    public void rebuild() {
        super.rebuild();

        int w = 181;
        int h = 20;
        int margin = 5;
        int xl = width / 2 - 2 - w;
        int xr = width / 2 + 2;
        int y = height / 2 - h / 2 - margin - h - margin - h - 15;

        btnShowFps = new ToggleButton(BTN_SHOW_FPS, xl, y, w, h, Gnetum.config.showHudFps, () -> I18n.format("gnetum.config.showFps") + ": %s");
        sliderNumberOfPasses = new IntSlider(SLIDER_NUMBER_OF_PASSES, xr, y, w, h, () -> I18n.format("gnetum.config.numberOfPasses") + ": %s", 2, 10, Gnetum.config.numberOfPasses, true, i -> Gnetum.config.numberOfPasses = i);
        sliderNumberOfPasses.setTooltip(I18n.format("gnetum.config.numberOfPasses.tooltip"));
        y += h + margin;
        sliderMaxFps = new IntSlider(SLIDER_MAX_FPS, xl, y, w, h, () -> I18n.format("gnetum.config.maxFps") + ": %s fps", 5, GnetumConfig.UNLIMITED_FPS, Gnetum.config.maxFps, 5, true, i -> Gnetum.config.maxFps = i, i -> i == 125, () -> I18n.format("gnetum.config.maxFps") + ": Unlimited");
        sliderMaxFps.setTooltip(I18n.format("gnetum.config.maxFps.tooltip"));
        btnReset = new GuiButtonEx(BTN_RESET, xr, y, w, h, I18n.format("gnetum.config.reset"), () -> Minecraft.getMinecraft().displayGuiScreen(new ConfirmationScreen(() -> new ConfigScreen(null, PerformanceAnalyzer.latestAnalysisResult), () -> this, GnetumConfig::reset)));
        this.addButton(btnShowFps);
        this.addButton(sliderNumberOfPasses);
        this.addButton(sliderMaxFps);
        this.addButton(btnReset);
    }
}
