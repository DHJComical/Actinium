package me.decce.gnetum.gui;

import me.decce.gnetum.CacheSetting;
import me.decce.gnetum.Gnetum;
import me.decce.gnetum.PerformanceAnalyzer;
import me.decce.gnetum.gui.widgets.GuiButtonEx;
import me.decce.gnetum.gui.widgets.GuiButtonImageEx;
import me.decce.gnetum.gui.widgets.ToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigScreen extends BaseScreen {
    public static final int BTN_ENABLED = 12;
    public static final int BTN_MORE_OPTIONS = 13;
    public static final int BTN_ANALYSIS = 17;
    public static final int BTN_MODDED_PRE = 14;
    public static final int BTN_VANILLA = 15;
    public static final int BTN_MODDED_POST = 16;
    private GuiButton btnMoreOptions;
    private GuiButtonImageEx btnAnalysis;
    private GuiButton btnModdedPre;
    private GuiButton btnVanilla;
    private GuiButton btnModdedPost;
    private PerformanceAnalyzer.Result analysis;
    private String btnAnalysisTooltip;

    public ConfigScreen() {
        this((PerformanceAnalyzer.Result)null);
    }

    public ConfigScreen(GuiScreen parent) {
        super(parent);
    }

    public ConfigScreen(PerformanceAnalyzer.Result analysis) {
        super();
        this.analysis = analysis;
    }

    public ConfigScreen(GuiScreen parent, PerformanceAnalyzer.Result analysis) {
        super(parent);
        this.analysis = analysis;
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    protected void rebuild() {
        super.rebuild();
        var btnEnabled = new ToggleButton(BTN_ENABLED, width / 2 - 130, height / 2 - 90, 120, 20, Gnetum.config.enabled, () -> I18n.format("gnetum.config.enabled") + ": %s");
        int w1 = analysis == null ? 120 : 120 - 20;
        btnMoreOptions = new GuiButtonEx(BTN_MORE_OPTIONS, width / 2 + 10, height / 2 - 90, w1, 20, I18n.format("gnetum.config.moreOptions"),
                () -> Minecraft.getMinecraft().displayGuiScreen(new MoreOptionsScreen()));
        if (analysis != null) {
            btnAnalysis = new GuiButtonImageEx(BTN_ANALYSIS, width / 2 + 10 + 120 - 20, height / 2 - 90, 20, 20, 16, 16, 2, 2, analysis.getIcon().icon(), () -> { Minecraft.getMinecraft().displayGuiScreen(new AnalysisScreen(analysis)); });
            btnAnalysis.enabled = !analysis.isOutdated();

            btnAnalysisTooltip = I18n.format(
                    analysis.isOutdated() ? "gnetum.config.analysis.outdated" :
                            (analysis.getMessages().isEmpty() ? "gnetum.config.analysis.good" : "gnetum.config.analysis.suboptimal")
            );
            btnAnalysis.setTooltip(btnAnalysisTooltip);

            this.buttonList.add(btnAnalysis);
        }

        int w = 260;
        int h = 20;
        int margin = 8;
        int x = width / 2 - 130;
        int y = height / 2 - h / 2 - margin - h;
        btnModdedPre = new GuiButtonEx(BTN_MODDED_PRE, x, y, w, h, I18n.format("gnetum.config.moddedPre"),
                () -> Minecraft.getMinecraft().displayGuiScreen(new ElementsScreen(sorted(Gnetum.config.mapModdedElementsPre), false)));
        y += margin + h;
        btnVanilla = new GuiButtonEx(BTN_VANILLA, x, y, w, h, I18n.format("gnetum.config.vanilla"),
                () -> Minecraft.getMinecraft().displayGuiScreen(new ElementsScreen(sorted(Gnetum.config.mapVanillaElements), true)));
        y += margin + h;
        btnModdedPost = new GuiButtonEx(BTN_MODDED_POST, x, y, w, h, I18n.format("gnetum.config.moddedPost"),
                () -> Minecraft.getMinecraft().displayGuiScreen(new ElementsScreen(sorted(Gnetum.config.mapModdedElementsPost), false)));
        this.buttonList.add(btnEnabled);
        this.buttonList.add(btnMoreOptions);
        this.buttonList.add(btnModdedPre);
        this.buttonList.add(btnVanilla);
        this.buttonList.add(btnModdedPost);
    }

    private Map<String, CacheSetting> sorted(Map<String, CacheSetting> original) {
        return original.entrySet().stream().
                //TODO: should compare display string instead of key
                sorted(Map.Entry.comparingByKey()).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        super.drawScreen(mouseX, mouseY, partialTick);
        btnMoreOptions.enabled = Gnetum.config.enabled.get();
        if (btnAnalysis != null) {
            btnAnalysis.enabled = Gnetum.config.enabled.get() && !analysis.isOutdated();
            btnAnalysis.setTooltip(btnAnalysis.enabled ? btnAnalysisTooltip : null);
            if (Gnetum.config.enabled.get() && analysis.isOutdated()) {
                btnAnalysis.setTooltip(btnAnalysisTooltip); // tells the user that the analysis is outdated
            }
        }
        btnVanilla.enabled = Gnetum.config.enabled.get();
        btnModdedPre.enabled = Gnetum.config.enabled.get();
        btnModdedPost.enabled = Gnetum.config.enabled.get();

    }

    @Override
    public void close() {
        super.close();
    }
}
