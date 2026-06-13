package me.decce.gnetum.gui;

import me.decce.gnetum.PerformanceAnalyzer;
import me.decce.gnetum.gui.widgets.MultilineStringWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class AnalysisScreen extends BaseScreen {
    public final PerformanceAnalyzer.Result analysis;
    private MultilineStringWidget txt;

    public AnalysisScreen(PerformanceAnalyzer.Result analysis) {
        this.analysis = analysis;
    }

    @Override
    public void close() {
        // do not call super here because there is no need to save config
        Minecraft.getMinecraft().displayGuiScreen(super.parent);
    }

    @Override
    protected void rebuild() {
        super.rebuild();

        String body;
        if (analysis.getMessages().isEmpty()) {
            body = I18n.format("gnetum.config.analysis.good");
        }
        else {
            body = String.join("\n\n", analysis.getMessages());
        }

        StringBuilder data = new StringBuilder();
        long[] durations = analysis.getDurations();
        if (durations == null) {
            data.append(I18n.format("gnetum.config.analysis.failedDuration"));
        }
        else {
            data.append(I18n.format("gnetum.config.analysis.detailDuration"));
            for (int i = 1; i < durations.length; i++) {
                data.append("\n");
                data.append("Pass [").append(i).append("] - ").append(String.format("%.2f", durations[i] / 1000d)).append("μs");
            }
        }

        String str = body + "\n\n" + data.toString();

        int w = 300;
        int h = 150;
        txt = new MultilineStringWidget(11, width / 2 - w / 2, height / 2 - h / 2, w, str);
        this.addButton(txt);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        String title = I18n.format(analysis.getIcon() == PerformanceAnalyzer.ResultIcon.TICK ? "gnetum.config.analysis.title.good" : "gnetum.config.analysis.title.suboptimal");
        int titleWidth = fontRenderer.getStringWidth(title);
        int xfont = width / 2 - titleWidth / 2 + 8 + 2;
        int xicon = xfont - 4 - 16;
        int y = height / 2 - 90;
        var icon = analysis.getIcon().icon();

        Minecraft.getMinecraft().getTextureManager().bindTexture(icon);
        super.drawTexturedModalRect(xicon, y - 8, 0, 0, 16, 16);
        super.drawString(fontRenderer, title, xfont, y, 0xFFFFFF);


    }
}
