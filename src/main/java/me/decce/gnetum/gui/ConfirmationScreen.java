package me.decce.gnetum.gui;

import me.decce.gnetum.gui.widgets.GuiButtonEx;
import me.decce.gnetum.gui.widgets.MultilineStringWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.util.function.Supplier;

public class ConfirmationScreen extends BaseScreen {
    private final Runnable action;
    private final Supplier<GuiScreen> parentYes;
    private final Supplier<GuiScreen> parentNo;

    public ConfirmationScreen(Supplier<GuiScreen> parentYes, Supplier<GuiScreen> parentNo, Runnable action) {
        super();
        this.action = action;
        this.parentYes = parentYes;
        this.parentNo = parentNo;
    }

    @Override
    protected void rebuild() {
        // No need for a Done button here, don't call super

        this.buttonList.clear();

        int w = 100;
        int h = 20;
        int m = 20;

        String txt = I18n.format("gnetum.config.confirmReset").replace("\\n", "\n");
        MultilineStringWidget stringWidget = new MultilineStringWidget(11, width / 2 - 150, height / 2 - fontRenderer.FONT_HEIGHT * 4, 300, txt);
        GuiButtonEx btnYes = new GuiButtonEx(12, width / 2 - w - m, height / 2 + 90, w, h, I18n.format("gui.yes"), this::onYes);
        GuiButtonEx btnNo = new GuiButtonEx(13, width / 2 + m, height / 2 + 90, w, h, I18n.format("gui.no"), this::onNo);

        this.addButton(stringWidget);
        this.addButton(btnYes);
        this.addButton(btnNo);
    }

    protected void onYes() {
        this.action.run();
        Minecraft.getMinecraft().displayGuiScreen(parentYes.get());
    }

    protected void onNo() {
        Minecraft.getMinecraft().displayGuiScreen(parentNo.get());
    }
}
