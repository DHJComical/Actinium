package com.dhj.actinium.shader.gui.screen;

import com.dhj.actinium.celeritas.ActiniumShaderProvider;
import com.dhj.actinium.shader.ActiniumShaderEntrypoint;
import com.dhj.actinium.shader.gui.element.ActiniumShaderPackSelectionList;
import com.dhj.actinium.shader.gui.screen.ActiniumShaderOptionScreen;
import com.dhj.actinium.shader.pack.ActiniumShaderPack;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.List;

public class ActiniumShaderPackScreen extends GuiScreen {
    private static final int BOTTOM_BAR_HEIGHT = 32;
    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_CANCEL = 1;
    private static final int BUTTON_APPLY = 2;
    private static final int BUTTON_OPEN_FOLDER = 3;
    private static final int BUTTON_REFRESH = 4;
    private static final int BUTTON_DEBUG = 5;
    private static final int BUTTON_OPTIONS = 6;
    private static final int BUTTON_TERRAIN_DEBUG = 7;

    private final GuiScreen parent;

    private List<ActiniumShaderPack> availablePacks;
    private ActiniumShaderPackSelectionList packList;
    private @Nullable String pendingPackName;
    private @Nullable String appliedPackName;
    private boolean pendingShadersEnabled;
    private boolean appliedShadersEnabled;
    private boolean debugEnabled;
    private int terrainDebugMode;

    public ActiniumShaderPackScreen(GuiScreen parent) {
        this.parent = parent;
        this.availablePacks = List.of();
    }

    @Override
    public void initGui() {
        this.availablePacks = ActiniumShaderPackManager.discoverPacks();
        this.appliedPackName = ActiniumShaderPackManager.getSelectedPackName();
        this.appliedShadersEnabled = ActiniumShaderPackManager.isShaderToggleEnabled();
        this.debugEnabled = ActiniumShaderPackManager.isDebugEnabled();
        this.terrainDebugMode = ActiniumShaderPackManager.getTerrainDebugMode();
        this.pendingPackName = this.appliedPackName;
        this.pendingShadersEnabled = this.appliedShadersEnabled;
        this.packList = new ActiniumShaderPackSelectionList(this, this.mc, this.width, this.height, 32, this.height - 58);
        this.packList.refresh(this.availablePacks, this.pendingPackName, this.appliedPackName, this.pendingShadersEnabled);
        this.packList.refreshToggleAvailability();

        int bottomRowY = this.getBottomBarTop() + (BOTTOM_BAR_HEIGHT - 20) / 2;
        int topRowY = bottomRowY - 24;
        int columnStart = this.width / 2 - 154;

        this.buttonList.clear();
        this.buttonList.add(new GuiButton(BUTTON_CANCEL, columnStart, bottomRowY, 100, 20, I18n.format("gui.cancel")));
        this.buttonList.add(new GuiButton(BUTTON_APPLY, columnStart + 104, bottomRowY, 100, 20, I18n.format("options.actinium.shaderPack.apply")));
        this.buttonList.add(new GuiButton(BUTTON_DONE, columnStart + 208, bottomRowY, 100, 20, I18n.format("gui.done")));
        this.buttonList.add(new GuiButton(BUTTON_OPEN_FOLDER, columnStart, topRowY, 100, 20, I18n.format("options.actinium.shaderPack.openFolder")));
        this.buttonList.add(new GuiButton(BUTTON_OPTIONS, columnStart + 104, topRowY, 100, 20, I18n.format("options.actinium.shaderPack.options")));
        this.buttonList.add(new GuiButton(BUTTON_REFRESH, columnStart + 208, topRowY, 100, 20, I18n.format("options.actinium.shaderPack.refresh")));
        this.buttonList.add(new GuiButton(BUTTON_DEBUG, this.width - 74, 6, 68, 20, this.getDebugButtonLabel()));
        this.buttonList.add(new GuiButton(BUTTON_TERRAIN_DEBUG, this.width - 144, 30, 138, 20, this.getTerrainDebugButtonLabel()));
        this.updateButtonState();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BUTTON_DONE -> this.applyChanges(true);
            case BUTTON_CANCEL -> this.mc.displayGuiScreen(this.parent);
            case BUTTON_APPLY -> this.applyChanges(false);
            case BUTTON_OPEN_FOLDER -> ActiniumShaderPackManager.openShaderPacksDirectory();
            case BUTTON_OPTIONS -> {
                if (this.canOpenShaderOptions()) {
                    this.mc.displayGuiScreen(new ActiniumShaderOptionScreen(this, this.pendingPackName));
                }
            }
            case BUTTON_REFRESH -> {
                this.availablePacks = ActiniumShaderPackManager.discoverPacks();
                this.packList.refresh(this.availablePacks, this.pendingPackName, this.appliedPackName, this.pendingShadersEnabled);
                this.packList.refreshToggleAvailability();
            }
            case BUTTON_DEBUG -> this.toggleDebug(button);
            case BUTTON_TERRAIN_DEBUG -> this.cycleTerrainDebug(button);
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

        ActiniumRenderPipeline.INSTANCE.resetVanillaRenderState();

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

    private void toggleDebug(GuiButton button) {
        this.debugEnabled = !this.debugEnabled;
        ActiniumShaderPackManager.setDebugEnabled(this.debugEnabled);
        button.displayString = this.getDebugButtonLabel();

        this.reloadShaderRuntime();
    }

    private String getDebugButtonLabel() {
        return I18n.format(this.debugEnabled ? "options.actinium.shaderPack.debugOn" : "options.actinium.shaderPack.debugOff");
    }

    private void cycleTerrainDebug(GuiButton button) {
        this.terrainDebugMode = (this.terrainDebugMode + 1) % (ActiniumShaderPackManager.MAX_TERRAIN_DEBUG_MODE + 1);
        ActiniumShaderPackManager.setTerrainDebugMode(this.terrainDebugMode);
        button.displayString = this.getTerrainDebugButtonLabel();
        this.reloadShaderRuntime();
    }

    private String getTerrainDebugButtonLabel() {
        return I18n.format(
                "options.actinium.shaderPack.terrainDebug",
                this.terrainDebugMode,
                I18n.format("options.actinium.shaderPack.terrainDebug." + this.terrainDebugMode)
        );
    }

    private void reloadShaderRuntime() {
        ActiniumShaderProvider provider = ActiniumShaderEntrypoint.getProvider();
        if (provider != null) {
            provider.deleteShaders();
        }

        ActiniumRenderPipeline.INSTANCE.resetVanillaRenderState();

        if (this.mc.world != null) {
            this.mc.renderGlobal.loadRenderers();
        }
    }

    private void updateButtonState() {
        GuiButton applyButton = null;
        GuiButton optionsButton = null;

        for (GuiButton button : this.buttonList) {
            if (button.id == BUTTON_APPLY) {
                applyButton = button;
            } else if (button.id == BUTTON_OPTIONS) {
                optionsButton = button;
            }
        }

        if (applyButton != null) {
            applyButton.enabled = this.hasPendingChanges();
        }

        if (optionsButton != null) {
            optionsButton.enabled = this.canOpenShaderOptions();
        }
    }

    private boolean canOpenShaderOptions() {
        return this.pendingPackName != null && !ActiniumShaderPackManager.isBuiltinPack(this.pendingPackName);
    }

    private int getBottomBarTop() {
        return this.height - BOTTOM_BAR_HEIGHT;
    }
}
