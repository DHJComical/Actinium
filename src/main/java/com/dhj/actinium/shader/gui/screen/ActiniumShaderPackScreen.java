package com.dhj.actinium.shader.gui.screen;

import com.dhj.actinium.celeritas.ActiniumShaderProvider;
import com.dhj.actinium.shader.ActiniumShaderEntrypoint;
import com.dhj.actinium.shader.gui.element.ActiniumShaderPackSelectionList;
import com.dhj.actinium.shader.pack.ActiniumShaderPack;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.List;

public class ActiniumShaderPackScreen extends GuiScreen {
    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_CANCEL = 1;
    private static final int BUTTON_APPLY = 2;
    private static final int BUTTON_OPEN_FOLDER = 3;
    private static final int BUTTON_REFRESH = 4;

    private final GuiScreen parent;

    private List<ActiniumShaderPack> availablePacks;
    private ActiniumShaderPackSelectionList packList;
    private @Nullable String pendingPackName;
    private @Nullable String appliedPackName;
    private boolean pendingShadersEnabled;
    private boolean appliedShadersEnabled;

    public ActiniumShaderPackScreen(GuiScreen parent) {
        this.parent = parent;
        this.availablePacks = List.of();
    }

    @Override
    public void initGui() {
        this.availablePacks = ActiniumShaderPackManager.discoverPacks();
        this.appliedPackName = ActiniumShaderPackManager.getSelectedPackName();
        this.appliedShadersEnabled = ActiniumShaderPackManager.isShaderToggleEnabled();
        this.pendingPackName = this.appliedPackName;
        this.pendingShadersEnabled = this.appliedShadersEnabled;
        this.packList = new ActiniumShaderPackSelectionList(this, this.mc, this.width, this.height, 32, this.height - 58);
        this.packList.refresh(this.availablePacks, this.pendingPackName, this.appliedPackName, this.pendingShadersEnabled);
        this.packList.refreshToggleAvailability();

        int bottomCenter = this.width / 2 - 50;
        int topCenter = this.width / 2 - 76;

        this.buttonList.clear();
        this.buttonList.add(new GuiButton(BUTTON_DONE, bottomCenter + 104, this.height - 27, 100, 20, I18n.format("gui.done")));
        this.buttonList.add(new GuiButton(BUTTON_APPLY, bottomCenter, this.height - 27, 100, 20, I18n.format("options.actinium.shaderPack.apply")));
        this.buttonList.add(new GuiButton(BUTTON_CANCEL, bottomCenter - 104, this.height - 27, 100, 20, I18n.format("gui.cancel")));
        this.buttonList.add(new GuiButton(BUTTON_OPEN_FOLDER, topCenter - 78, this.height - 51, 152, 20, I18n.format("options.actinium.shaderPack.openFolder")));
        this.buttonList.add(new GuiButton(BUTTON_REFRESH, topCenter + 78, this.height - 51, 152, 20, I18n.format("options.actinium.shaderPack.refresh")));
        this.updateButtonState();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BUTTON_DONE -> this.applyChanges(true);
            case BUTTON_CANCEL -> this.mc.displayGuiScreen(this.parent);
            case BUTTON_APPLY -> this.applyChanges(false);
            case BUTTON_OPEN_FOLDER -> ActiniumShaderPackManager.openShaderPacksDirectory();
            case BUTTON_REFRESH -> {
                this.availablePacks = ActiniumShaderPackManager.discoverPacks();
                this.packList.refresh(this.availablePacks, this.pendingPackName, this.appliedPackName, this.pendingShadersEnabled);
                this.packList.refreshToggleAvailability();
            }
            default -> {
            }
        }
    }

    public void applyChanges(boolean closeAfterApply) {
        if (!this.hasPendingChanges()) {
            if (closeAfterApply) {
                this.mc.displayGuiScreen(this.parent);
            }
            return;
        }

        this.pendingPackName = this.packList.getSelectedPackName();
        this.pendingShadersEnabled = this.packList.areShadersEnabled();
        ActiniumShaderPackManager.applySelection(this.pendingPackName, this.pendingShadersEnabled);
        this.appliedPackName = this.pendingPackName;
        this.appliedShadersEnabled = this.pendingShadersEnabled;
        this.packList.setAppliedSelection(this.appliedPackName, this.appliedShadersEnabled);

        ActiniumShaderProvider provider = ActiniumShaderEntrypoint.getProvider();
        if (provider != null) {
            provider.deleteShaders();
        }

        if (this.mc.world != null) {
            this.mc.renderGlobal.loadRenderers();
        }

        this.updateButtonState();

        if (closeAfterApply) {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        boolean handled = this.packList.mouseClicked(mouseX, mouseY, mouseButton);

        if (!handled) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        boolean handled = this.packList.mouseReleased(mouseX, mouseY, state);

        if (!handled) {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.mc.world == null) {
            this.drawDefaultBackground();
        } else {
            this.drawGradientRect(0, 0, this.width, this.height, 0x4F232323, 0x4F232323);
        }

        this.packList.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRenderer, I18n.format("options.actinium.shaderPackSelection"), this.width / 2, 10, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, I18n.format("pack.actinium.select.title"), this.width / 2, 21, 0xA0A0A0);
        this.drawString(this.fontRenderer, "Actinium Shader", 2, this.height - 10, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, this.getStatusLine(), this.width / 2, this.height - 80, 0x808080);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public List<ActiniumShaderPack> getAvailablePacks() {
        return this.availablePacks;
    }

    public void onSelectionChanged() {
        this.pendingPackName = this.packList.getSelectedPackName();
        this.pendingShadersEnabled = this.packList.areShadersEnabled();

        if (!this.pendingShadersEnabled && this.appliedShadersEnabled) {
            this.pendingPackName = this.appliedPackName;
        }

        this.updateButtonState();
    }

    private String getStatusLine() {
        if (!this.pendingShadersEnabled || this.pendingPackName == null) {
            return I18n.format("options.actinium.shaderPack.currentDisabled");
        }

        return I18n.format("options.actinium.shaderPack.current", this.pendingPackName);
    }

    private boolean hasPendingChanges() {
        boolean packChanged;

        if (this.pendingPackName == null) {
            packChanged = this.appliedPackName != null;
        } else {
            packChanged = !this.pendingPackName.equals(this.appliedPackName);
        }

        return packChanged || this.pendingShadersEnabled != this.appliedShadersEnabled;
    }

    private void updateButtonState() {
        GuiButton applyButton = null;

        for (GuiButton button : this.buttonList) {
            if (button.id == BUTTON_APPLY) {
                applyButton = button;
                break;
            }
        }

        if (applyButton != null) {
            applyButton.enabled = this.hasPendingChanges();
        }
    }
}
