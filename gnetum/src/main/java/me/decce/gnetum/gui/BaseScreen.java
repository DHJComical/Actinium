package me.decce.gnetum.gui;

import me.decce.gnetum.Gnetum;
import me.decce.gnetum.gui.widgets.GuiButtonEx;
import me.decce.gnetum.gui.widgets.GuiButtonImageEx;
import me.decce.gnetum.gui.widgets.ITooltipWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.io.IOException;


public class BaseScreen extends GuiScreen {
    protected static final int BTN_CLOSE = 11;
    protected final GuiScreen parent;

    public BaseScreen() {
        this(Minecraft.getMinecraft().currentScreen);
    }

    protected BaseScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        super.actionPerformed(button);
        if (button.enabled) {
            if (button instanceof GuiButtonEx ex) {
                ex.onPress();
            }
            else if (button instanceof GuiButtonImageEx ex) {
                ex.onPress();
            }
        }
    }

    public void close() { // called when "Done" is pressed
        Gnetum.config.save();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() { // TODO: this is called when ESC is pressed: ideally we should have a dialog asking the user whether to save or discard changes
        super.onGuiClosed();
        Gnetum.config.save();
        //or: GnetumConfig.reload();
    }

    @Override
    public void initGui() {
        this.rebuild();
    }

    protected void rebuild() {
        this.buttonList.clear();
        var btnClose = new GuiButtonEx(BTN_CLOSE, width / 2 - 60, height / 2 + 90, 120, 20, I18n.format("gui.done"), this::close);
        this.buttonList.add(btnClose);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        this.drawDefaultBackground();

        super.drawScreen(mouseX, mouseY, partialTick);

        fontRenderer.drawString("Gnetum", width / 2 - fontRenderer.getStringWidth("Gnetum") / 2, 5, 0xFFFFFF);

        for (var button : this.buttonList) {
            if (button instanceof ITooltipWidget tooltip) {
                tooltip.drawTooltip(mouseX, mouseY);
            }
        }
    }
}
