package com.dhj.actinium.shader.gui.screen;

import com.dhj.actinium.celeritas.ActiniumShaderProvider;
import com.dhj.actinium.shader.ActiniumShaderEntrypoint;
import com.dhj.actinium.shader.gui.element.ActiniumShaderOptionList;
import com.dhj.actinium.shader.options.ActiniumShaderOption;
import com.dhj.actinium.shader.options.ActiniumShaderOptionMenu;
import com.dhj.actinium.shader.options.ActiniumShaderProfile;
import com.dhj.actinium.shader.options.ActiniumShaderProfileSet;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ActiniumShaderOptionScreen extends GuiScreen {
    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_APPLY = 1;
    private static final int BUTTON_CANCEL = 2;
    private static final int BUTTON_RESET = 3;
    private static final int BUTTON_BACK = 4;
    private static final int OPTION_PANEL_TOP = 44;
    private static final int BOTTOM_BAR_HEIGHT = 32;
    private static final int OPTION_PANEL_BOTTOM_MARGIN = BOTTOM_BAR_HEIGHT + 4;

    private final GuiScreen parent;
    private final String packName;
    private final Deque<String> navigationHistory = new ArrayDeque<>();
    private final Map<String, String> appliedOverrides = new LinkedHashMap<>();
    private final Map<String, String> pendingOverrides = new LinkedHashMap<>();
    private ActiniumShaderOptionMenu menu;
    private ActiniumShaderOptionList optionList;
    private @Nullable String currentScreenId;

    public ActiniumShaderOptionScreen(GuiScreen parent, String packName) {
        this.parent = parent;
        this.packName = packName;
        this.menu = createEmptyMenu(packName);
        this.reloadMenu(ActiniumShaderPackManager.getPackOptionOverrides(packName));
    }

    @Override
    public void initGui() {
        int buttonY = this.getBottomBarTop() + (BOTTOM_BAR_HEIGHT - 20) / 2;
        this.optionList = new ActiniumShaderOptionList(this, this.mc, this.width, this.height, this.getOptionPanelTop(), this.getOptionPanelBottom());
        this.optionList.rebuild();
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(BUTTON_DONE, this.width / 2 + 106, buttonY, 100, 20, I18n.format("gui.done")));
        this.buttonList.add(new GuiButton(BUTTON_APPLY, this.width / 2 + 2, buttonY, 100, 20, I18n.format("options.actinium.shaderPack.apply")));
        this.buttonList.add(new GuiButton(BUTTON_CANCEL, this.width / 2 - 102, buttonY, 100, 20, I18n.format("gui.cancel")));
        this.buttonList.add(new GuiButton(BUTTON_RESET, this.width / 2 - 206, buttonY, 100, 20, I18n.format("options.actinium.shaderPack.options.reset")));
        this.buttonList.add(new GuiButton(BUTTON_BACK, 6, 6, 70, 20, I18n.format("options.actinium.shaderPack.options.back")));
        this.updateButtonState();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BUTTON_DONE -> this.applyChanges(true);
            case BUTTON_APPLY -> this.applyChanges(false);
            case BUTTON_CANCEL -> this.mc.displayGuiScreen(this.parent);
            case BUTTON_RESET -> {
                this.pendingOverrides.clear();
                this.reloadMenu(this.pendingOverrides);
            }
            case BUTTON_BACK -> this.navigateBack();
            default -> {
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.isClickOnButton(mouseX, mouseY) || !this.isWithinOptionPanel(mouseY)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        boolean handled = this.optionList.mouseClicked(mouseX, mouseY, mouseButton);

        if (!handled) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.isClickOnButton(mouseX, mouseY) || !this.isWithinOptionPanel(mouseY)) {
            super.mouseReleased(mouseX, mouseY, state);
            return;
        }

        boolean handled = this.optionList.mouseReleased(mouseX, mouseY, state);

        if (!handled) {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.optionList.handleMouseInput();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.mc.world == null) {
            this.drawDefaultBackground();
        } else {
            this.drawGradientRect(0, 0, this.width, this.height, 0x4F232323, 0x4F232323);
        }

        this.optionList.drawScreen(mouseX, mouseY, partialTicks);
        this.drawChromeMask();
        this.drawCenteredString(this.fontRenderer, I18n.format("options.actinium.shaderPack.options.title"), this.width / 2, 10, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, this.menu.getScreenLabel(this.currentScreenId), this.width / 2, 21, 0xA0A0A0);
        this.drawString(this.fontRenderer, this.packName, 2, this.height - 10, 0xFFFFFF);

        if (this.menu.getOptions().isEmpty()) {
            this.drawCenteredString(this.fontRenderer, I18n.format("options.actinium.shaderPack.options.empty"), this.width / 2, this.height / 2 - 4, 0xC0C0C0);
        } else {
            this.drawCommentPanel();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public ActiniumShaderOptionMenu getMenu() {
        return this.menu;
    }

    public @Nullable String getCurrentScreenId() {
        return this.currentScreenId;
    }

    public String getCurrentValue(ActiniumShaderOption option) {
        return this.pendingOverrides.getOrDefault(option.name(), option.getDefaultSerializedValue());
    }

    public String getAppliedValue(ActiniumShaderOption option) {
        return this.appliedOverrides.getOrDefault(option.name(), option.getDefaultSerializedValue());
    }

    public boolean isOptionModified(ActiniumShaderOption option) {
        return !Objects.equals(this.getCurrentValue(option), this.getAppliedValue(option));
    }

    public void cycleOption(ActiniumShaderOption option, int direction) {
        if (option.isBooleanOption()) {
            this.setOptionValue(option, Boolean.toString(!Boolean.parseBoolean(this.getCurrentValue(option))));
            return;
        }

        List<String> allowedValues = option.getAllowedValues();

        if (allowedValues.isEmpty()) {
            return;
        }

        String currentValue = this.getCurrentValue(option);
        int index = allowedValues.indexOf(currentValue);

        if (index < 0) {
            index = allowedValues.indexOf(option.getDefaultValue());
        }

        if (index < 0) {
            index = 0;
        }

        int nextIndex = Math.floorMod(index + direction, allowedValues.size());
        this.setOptionValue(option, allowedValues.get(nextIndex));
    }

    public void setSliderValue(ActiniumShaderOption option, int mouseX, int sliderLeft, int sliderWidth) {
        List<String> allowedValues = option.getAllowedValues();

        if (allowedValues.isEmpty()) {
            return;
        }

        float normalized = sliderWidth <= 0 ? 0.0f : (float) (mouseX - sliderLeft) / (float) sliderWidth;
        normalized = Math.max(0.0f, Math.min(1.0f, normalized));
        int valueIndex = Math.min(allowedValues.size() - 1, Math.round(normalized * (allowedValues.size() - 1)));
        this.setOptionValue(option, allowedValues.get(valueIndex));
    }

    public void resetOption(ActiniumShaderOption option) {
        this.pendingOverrides.remove(option.name());
        this.refreshOptionState();
    }

    public void applyProfile(ActiniumShaderProfile profile) {
        if (profile == null) {
            return;
        }

        for (Map.Entry<String, String> entry : profile.optionValues().entrySet()) {
            ActiniumShaderOption option = this.menu.getOption(entry.getKey());

            if (option != null) {
                this.setOptionValue(option, entry.getValue(), false);
            }
        }

        this.refreshOptionState();
    }

    public ActiniumShaderProfileSet.ProfileResult getProfileResult(boolean secondSet) {
        return secondSet
                ? this.menu.getProfiles2().scan(this.menu, this.getCurrentValuesSnapshot())
                : this.menu.getProfiles().scan(this.menu, this.getCurrentValuesSnapshot());
    }

    public void openSubScreen(String screenId) {
        this.currentScreenId = screenId;
        this.navigationHistory.addLast(screenId);
        this.refreshOptionState();
    }

    private void navigateBack() {
        if (!this.navigationHistory.isEmpty()) {
            this.navigationHistory.removeLast();
        }

        this.currentScreenId = this.navigationHistory.peekLast();
        this.refreshOptionState();
    }

    private void applyChanges(boolean closeAfterApply) {
        if (!this.hasPendingChanges()) {
            if (closeAfterApply) {
                this.mc.displayGuiScreen(this.parent);
            }
            return;
        }

        ActiniumShaderPackManager.savePackOptionOverrides(this.packName, this.pendingOverrides);
        this.appliedOverrides.clear();
        this.appliedOverrides.putAll(this.pendingOverrides);
        this.reloadMenu(this.pendingOverrides);

        if (ActiniumShaderPackManager.isShaderToggleEnabled() && Objects.equals(this.packName, ActiniumShaderPackManager.getSelectedPackName())) {
            this.reloadShaders();
        }

        if (closeAfterApply) {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    private void reloadShaders() {
        ActiniumShaderPackManager.reload();

        ActiniumShaderProvider provider = ActiniumShaderEntrypoint.getProvider();
        if (provider != null) {
            provider.deleteShaders();
        }

        ActiniumRenderPipeline.INSTANCE.resetVanillaRenderState();

        if (this.mc.world != null) {
            this.mc.renderGlobal.loadRenderers();
        }
    }

    private void reloadMenu(Map<String, String> overrides) {
        ActiniumShaderOptionMenu loadedMenu = ActiniumShaderPackManager.loadShaderOptionMenu(this.packName, overrides);

        if (loadedMenu != null) {
            this.menu = loadedMenu;
        }

        Map<String, String> normalized = this.menu.normalizeOverrides(overrides);
        this.pendingOverrides.clear();
        this.pendingOverrides.putAll(normalized);

        if (this.appliedOverrides.isEmpty()) {
            this.appliedOverrides.putAll(normalized);
        }

        if (this.currentScreenId != null && this.menu.getScreenTokens(this.currentScreenId).isEmpty()) {
            this.currentScreenId = null;
            this.navigationHistory.clear();
        }

        this.refreshOptionState();
    }

    private void setOptionValue(ActiniumShaderOption option, String value) {
        this.setOptionValue(option, value, true);
    }

    private void setOptionValue(ActiniumShaderOption option, String value, boolean refresh) {
        if (!option.acceptsValue(value)) {
            return;
        }

        if (option.isDefaultValue(value)) {
            this.pendingOverrides.remove(option.name());
        } else {
            this.pendingOverrides.put(option.name(), value);
        }

        if (refresh) {
            this.refreshOptionState();
        }
    }

    private void refreshOptionState() {
        if (this.optionList != null) {
            this.optionList.rebuild();
        }

        this.updateButtonState();
    }

    private Map<String, String> getCurrentValuesSnapshot() {
        Map<String, String> values = new LinkedHashMap<>();

        for (ActiniumShaderOption option : this.menu.getOptions()) {
            values.put(option.name(), this.getCurrentValue(option));
        }

        return values;
    }

    private boolean hasPendingChanges() {
        return !this.pendingOverrides.equals(this.appliedOverrides);
    }

    private void updateButtonState() {
        for (GuiButton button : this.buttonList) {
            switch (button.id) {
                case BUTTON_DONE -> button.enabled = true;
                case BUTTON_APPLY -> button.enabled = this.hasPendingChanges();
                case BUTTON_RESET -> button.enabled = !this.pendingOverrides.isEmpty();
                case BUTTON_BACK -> button.visible = this.currentScreenId != null;
                default -> {
                }
            }
        }
    }

    private void drawCommentPanel() {
        String title = this.optionList.getHoveredCommentTitle();
        String body = this.optionList.getHoveredCommentBody();

        if ((title == null || title.isBlank()) && (body == null || body.isBlank())) {
            body = this.menu.getScreenComment(this.currentScreenId);
            title = body != null ? this.menu.getScreenLabel(this.currentScreenId) : null;
        }

        if ((title == null || title.isBlank()) && (body == null || body.isBlank())) {
            return;
        }

        List<String> wrappedBody = body != null ? this.fontRenderer.listFormattedStringToWidth(body, this.width - 40) : List.of();
        int panelHeight = 18 + (wrappedBody.size() * 10) + (title != null ? 10 : 0);
        int left = 20;
        int top = Math.max(this.getOptionPanelTop() + 4, this.getBottomBarTop() - panelHeight - 6);
        int right = this.width - 20;
        this.drawGradientRect(left, top, right, top + panelHeight, 0xC0101010, 0xC0101010);

        int y = top + 4;

        if (title != null && !title.isBlank()) {
            this.drawString(this.fontRenderer, title, left + 6, y, 0xFFFFFF);
            y += 12;
        }

        for (String line : wrappedBody) {
            this.drawString(this.fontRenderer, line, left + 6, y, 0xD0D0D0);
            y += 10;
        }
    }

    private int getOptionPanelTop() {
        return OPTION_PANEL_TOP;
    }

    private int getOptionPanelBottom() {
        return this.height - OPTION_PANEL_BOTTOM_MARGIN;
    }

    private int getBottomBarTop() {
        return this.height - BOTTOM_BAR_HEIGHT;
    }

    private void drawChromeMask() {
        int bottomBarTop = this.getBottomBarTop();
        this.drawGradientRect(0, 0, this.width, this.getOptionPanelTop(), 0xF0101010, 0xF0101010);
        this.drawGradientRect(0, bottomBarTop, this.width, this.height, 0xF0101010, 0xF0101010);
    }

    private boolean isWithinOptionPanel(int mouseY) {
        return mouseY >= this.getOptionPanelTop() && mouseY <= this.getOptionPanelBottom();
    }

    private boolean isClickOnButton(int mouseX, int mouseY) {
        for (GuiButton button : this.buttonList) {
            if (button.visible
                    && mouseX >= button.x
                    && mouseX < button.x + button.width
                    && mouseY >= button.y
                    && mouseY < button.y + button.height) {
                return true;
            }
        }

        return false;
    }

    private static ActiniumShaderOptionMenu createEmptyMenu(String packName) {
        return new ActiniumShaderOptionMenu(
                packName,
                List.of(),
                Map.of(),
                List.of("*"),
                Map.of(),
                2,
                Map.of(),
                new ActiniumShaderProfileSet(new LinkedHashMap<>()),
                new ActiniumShaderProfileSet(new LinkedHashMap<>())
        );
    }
}
