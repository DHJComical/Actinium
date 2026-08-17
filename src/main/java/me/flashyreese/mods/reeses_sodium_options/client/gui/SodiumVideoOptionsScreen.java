package me.flashyreese.mods.reeses_sodium_options.client.gui;

import com.dhj.actinium.gui.rso.compat.Component;
import com.dhj.actinium.gui.rso.compat.ComponentPath;
import com.dhj.actinium.gui.rso.compat.ContainerEventHandler;
import com.dhj.actinium.gui.rso.compat.FocusNavigationEvent;
import com.dhj.actinium.gui.rso.compat.GuiEventListener;
import com.dhj.actinium.gui.rso.compat.GuiGraphicsExtractor;
import com.dhj.actinium.gui.rso.compat.KeyEvent;
import com.dhj.actinium.gui.rso.compat.MouseButtonEvent;
import com.dhj.actinium.gui.rso.compat.ScreenDirection;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.BasicFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.OptionRow;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.action.OptionUndoAction;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoModOptions;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoOption;
import me.flashyreese.mods.reeses_sodium_options.client.gui.search.SearchTextFieldWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionsScreenUiState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.FlatButtonWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SodiumVideoOptionsScreen extends GuiScreen implements PreviousScreenHolder {

    private static final OptionsScreenUiState SHARED_UI_STATE = new OptionsScreenUiState();

    private static final double ASPECT_RATIO_16_9 = 16.0 / 9.0;
    private static final int TOOLBAR_BUTTON_WIDTH = 65;
    private static final int TOOLBAR_BUTTON_GAP = 4;
    private static final int TOOLBAR_BUTTON_Y_GAP = 5;
    private static final int TOP_ROW_HEIGHT = 20;
    private static final int TOP_ROW_Y_GAP = 26;

    private final GuiScreen prevScreen;
    private final OptionsScreenUiState uiState;
    private final List<RsoModOptions> modOptions;
    private FlatButtonWidget applyButton, closeButton, undoButton;
    private boolean hasPendingChanges;
    private SearchTextFieldWidget searchTextField;
    private AbstractFrame rootFrame;
    private TabFrame tabFrame;
    private GuiEventListener focused;
    private ComponentPath previousArrowFocusPath;
    private GuiEventListener currentArrowFocusLeaf;
    private ScreenDirection lastArrowDirection;

    public SodiumVideoOptionsScreen(GuiScreen prev, List<RsoModOptions> modOptions) {
        this.prevScreen = prev;
        this.modOptions = getOrderedModOptions(modOptions);
        this.uiState = SHARED_UI_STATE;
    }

    @Override
    public GuiScreen rso$previousScreen() {
        return this.prevScreen;
    }

    public FlatButtonWidget rso$getApplyButton() {
        return this.applyButton;
    }

    public FlatButtonWidget rso$getCloseButton() {
        return this.closeButton;
    }

    public FlatButtonWidget rso$getUndoButton() {
        return this.undoButton;
    }

    public SearchTextFieldWidget rso$getSearchTextField() {
        return this.searchTextField;
    }

    public TabFrame rso$getTabFrame() {
        return this.tabFrame;
    }

    // Hackalicious! Rebuild UI
    public void rebuildUI() {
        boolean wasSearchBarFocused = this.searchTextField.isFocused();
        this.clearArrowNavigationMemory();
        this.rebuildWidgets();
        if (wasSearchBarFocused) this.focusSearchTextField();
    }

    private void refreshSearchResults() {
        this.clearArrowNavigationMemory();
        if (this.tabFrame != null) {
            this.tabFrame.refreshFromState();
        }
    }

    private void rebuildWidgets() {
        this.rootFrame = this.parentFrameBuilder().build();
    }

    @Override
    public void initGui() {
        BaseWidget.setKeyboardFocusVisible(true);

        this.rebuildWidgets();

        if (this.searchTextField.isFocused()) {
            this.focusSearchTextField();
        } else if (this.restoreFocusedOptionForSelectedTab()) {
            this.rememberCurrentOptionFocus();
        } else {
            this.setFocused(this.rootFrame);
        }
    }

    private void focusSearchTextField() {
        if (this.searchTextField == null) {
            return;
        }
        this.searchTextField.setFocused(true);

        if (this.rootFrame != null) {
            this.rootFrame.setFocused(this.searchTextField);
            this.setFocused(this.rootFrame);
        } else {
            this.setFocused(this.searchTextField);
        }
    }

    private BasicFrame.Builder parentFrameBuilder() {
        // Calculates if resolution exceeds 16:9 ratio, force 16:9
        int newWidth = this.width;
        if ((float) this.width / (float) this.height > ASPECT_RATIO_16_9) {
            newWidth = (int) (this.height * ASPECT_RATIO_16_9);
        }

        LayoutBounds basicFrameDim = new LayoutBounds((this.width - newWidth) / 2, 0, newWidth, this.height);
        LayoutBounds tabFrameDim = new LayoutBounds(basicFrameDim.x() + basicFrameDim.width() / 20 / 2, basicFrameDim.y() + basicFrameDim.height() / 4 / 2, basicFrameDim.width() - (basicFrameDim.width() / 20), basicFrameDim.height() / 4 * 3);

        int toolbarY = tabFrameDim.getLimitY() + TOOLBAR_BUTTON_Y_GAP;
        LayoutBounds closeButtonDim = new LayoutBounds(toolbarButtonX(tabFrameDim, 0), toolbarY, TOOLBAR_BUTTON_WIDTH, TOP_ROW_HEIGHT);
        LayoutBounds applyButtonDim = new LayoutBounds(toolbarButtonX(tabFrameDim, 1), toolbarY, TOOLBAR_BUTTON_WIDTH, TOP_ROW_HEIGHT);
        LayoutBounds undoButtonDim = new LayoutBounds(toolbarButtonX(tabFrameDim, 2), toolbarY, TOOLBAR_BUTTON_WIDTH, TOP_ROW_HEIGHT);

        int topRowY = tabFrameDim.y() - TOP_ROW_Y_GAP;

        this.undoButton = new FlatButtonWidget(undoButtonDim, Component.translatable("sodium.options.buttons.undo"), this::resetAllOptionsFromBindings, true, false);
        this.applyButton = new FlatButtonWidget(applyButtonDim, Component.translatable("sodium.options.buttons.apply"), this::applyAllOptions, true, false);
        this.closeButton = new FlatButtonWidget(closeButtonDim, Component.translatable("gui.done"), this::onClose, true, false);

        BasicFrame.Builder basicFrameBuilder = this.parentBasicFrameBuilder(basicFrameDim, tabFrameDim);

        LayoutBounds searchTextFieldDim = new LayoutBounds(tabFrameDim.x(), topRowY, tabFrameDim.width(), TOP_ROW_HEIGHT);

        this.searchTextField = new SearchTextFieldWidget(searchTextFieldDim, this.modOptions, this.uiState,
                tabFrameDim.height(), this::refreshSearchResults);

        basicFrameBuilder.addChild(() -> this.searchTextField);

        return basicFrameBuilder;
    }

    // Toolbar buttons are laid out right-to-left: slot 0 is the rightmost (close).
    private static int toolbarButtonX(LayoutBounds tabFrameDim, int slotFromRight) {
        return tabFrameDim.getLimitX() - (slotFromRight + 1) * TOOLBAR_BUTTON_WIDTH - slotFromRight * TOOLBAR_BUTTON_GAP;
    }

    private BasicFrame.Builder parentBasicFrameBuilder(LayoutBounds parentBasicFrameDim, LayoutBounds tabFrameDim) {
        return BasicFrame.builder()
                .withDimension(parentBasicFrameDim)
                .withRenderOutline(false)
                .withScreen(this)
                .addChild(() -> {
                    this.tabFrame = TabFrame.createBuilder()
                            .setDimension(tabFrameDim)
                            .withScreen(this)
                            .shouldRenderOutline(false)
                            .setTabRailScrollBarOffset(this.uiState.tabFrameScrollBarOffset())
                            .setScrollSelectedTabIntoView(this.uiState.scrollSelectedTabIntoView())
                            .setTabRailSelectedTab(this.uiState.tabFrameSelectedTab())
                            .setTabRailSelectedGroup(this.uiState.tabFrameSelectedGroup())
                            .setManuallyCollapsedTabGroups(this.uiState.manuallyCollapsedTabGroups())
                            .setOptionPageScrollBarOffset(this.uiState.optionPageScrollBarOffset())
                            .setOptionStateStore(this.uiState)
                            .addTabs(tabs -> this.modOptions
                                    .forEach(config -> config.pages()
                                            .forEach(page -> tabs.add(Tab.builder().from(this, config, page.unwrap(), this.uiState.optionPageScrollBarOffset(), this.uiState))))
                            )
                            .onSetTab(() -> {
                                this.uiState.optionPageScrollBarOffset().set(0);
                            })
                            .build();
                    return this.tabFrame;
                })
                .addChild(() -> this.undoButton)
                .addChild(() -> this.applyButton)
                .addChild(() -> this.closeButton);
    }

    private void updateControls() {
        boolean hasChanges = this.anyOptionChanged();

        this.applyButton.setEnabled(hasChanges);
        this.undoButton.setVisible(hasChanges);
        this.closeButton.setEnabled(!hasChanges);

        this.hasPendingChanges = hasChanges;
    }

    private boolean anyOptionChanged() {
        return com.dhj.actinium.compat.sodium.ActiniumOptionHost.shared().hasPendingChanges();
    }

    private void applyAllOptions() {
        com.dhj.actinium.compat.sodium.ActiniumOptionHost.shared().applyChanges();
        this.updateControls();
    }

    private void resetAllOptionsFromBindings() {
        com.dhj.actinium.compat.sodium.ActiniumOptionHost.shared().undoChanges();
        this.updateControls();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.updateControls();
        GuiGraphicsExtractor guiGraphics = new GuiGraphicsExtractor();
        this.rootFrame.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // RSO's own configuration page is placed first in the tab rail.
    private static final java.util.Set<String> RSO_CONFIG_IDS =
            java.util.Set.of("reeses-sodium-options", "reeses_sodium_options");

    private static List<RsoModOptions> getOrderedModOptions(List<RsoModOptions> modOptions) {
        return modOptions.stream()
                .sorted((left, right) -> Boolean.compare(isOwnConfig(left), isOwnConfig(right)))
                .toList();
    }

    private static boolean isOwnConfig(RsoModOptions modOptions) {
        return RSO_CONFIG_IDS.contains(modOptions.configId());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Mouse interaction hides keyboard focus borders, mirroring the
        // upstream lastInputType behavior.
        BaseWidget.setKeyboardFocusVisible(false);
        this.clearArrowNavigationMemory();
        String previousTabKey = this.getSelectedTabKey();
        MouseButtonEvent event = new MouseButtonEvent(mouseButton, mouseX, mouseY);
        this.rootFrame.mouseClicked(event, false);
        this.afterInput(previousTabKey);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        MouseButtonEvent event = new MouseButtonEvent(state, mouseX, mouseY);
        if (state == 0 && this.rootFrame != null) {
            this.rootFrame.releaseActionButtonLayoutHolds();
        }
        this.rootFrame.mouseReleased(event);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        MouseButtonEvent event = new MouseButtonEvent(clickedMouseButton, mouseX, mouseY);
        this.rootFrame.mouseDragged(event, 0, 0);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (dWheel != 0) {
            int mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            double scrollDelta = dWheel > 0 ? 1 : -1;
            this.rootFrame.mouseScrolled(mouseX, mouseY, 0, scrollDelta);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Keyboard interaction reveals keyboard focus borders, mirroring the
        // upstream lastInputType behavior.
        BaseWidget.setKeyboardFocusVisible(true);
        String previousTabKey = this.getSelectedTabKey();

        if (this.isSearchShortcut(keyCode)) {
            this.focusSearchTextField();
            this.searchTextField.selectAllText();
            this.clearArrowNavigationMemory();
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE && this.handleFocusedOptionBackNavigation()) {
            this.clearArrowNavigationMemory();
            this.afterInput(previousTabKey);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE && this.clearSearchText()) {
            this.clearArrowNavigationMemory();
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (!this.hasPendingChanges) {
                this.onClose();
            }
            return;
        }

        if (keyCode == Keyboard.KEY_P && GuiScreen.isShiftKeyDown() && !this.isSearchTextFieldFocused()) {
            // Shift+P bypasses the RSO screen into the vanilla video settings screen.
            this.mc.displayGuiScreen(new net.minecraft.client.gui.GuiVideoSettings(this.prevScreen,
                    this.mc.gameSettings));
            return;
        }

        if (!this.isSearchTextFieldFocused()) {
            if (this.isUndoShortcut(keyCode) && this.undoFocusedOption()) {
                this.clearArrowNavigationMemory();
                this.afterInput(previousTabKey);
                return;
            }

            if (this.keyPressedOptionListNavigation(keyCode)) {
                this.clearArrowNavigationMemory();
                this.afterInput(previousTabKey);
                return;
            }

            if (this.isApplyShortcut(keyCode)) {
                if (this.anyOptionChanged()) {
                    this.applyAllOptions();
                    this.updateControls();
                    this.clearArrowNavigationMemory();
                    this.afterInput(previousTabKey);
                    return;
                }
            }
        }

        ScreenDirection arrowDirection = getArrowDirection(keyCode);
        if (arrowDirection != null) {
            this.keyPressedArrow(new KeyEvent(KeyEvent.toGlfw(keyCode), keyCode, modifiersFor(keyCode)), arrowDirection);
            return;
        }

        this.clearArrowNavigationMemory();

        KeyEvent event = new KeyEvent(KeyEvent.toGlfw(keyCode), keyCode, modifiersFor(keyCode));
        if (this.isSearchTextFieldFocused()) {
            // Forward to the search field: key handling first, then character input.
            if (!this.searchTextField.keyPressed(event)) {
                this.searchTextField.charTyped(new com.dhj.actinium.gui.rso.compat.CharacterEvent(typedChar));
            }
        } else {
            this.rootFrame.keyPressed(event);
        }
        this.afterInput(previousTabKey);
    }

    private int modifiersFor(int keyCode) {
        int modifiers = 0;
        if (GuiScreen.isShiftKeyDown()) modifiers |= 1;
        if (GuiScreen.isCtrlKeyDown()) modifiers |= 2;
        if (GuiScreen.isAltKeyDown()) modifiers |= 4;
        return modifiers;
    }

    private boolean isSearchShortcut(int keyCode) {
        return keyCode == Keyboard.KEY_F && GuiScreen.isCtrlKeyDown();
    }

    private boolean isUndoShortcut(int keyCode) {
        return keyCode == Keyboard.KEY_Z && GuiScreen.isCtrlKeyDown();
    }

    private boolean isApplyShortcut(int keyCode) {
        return keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER;
    }

    private boolean isSearchTextFieldFocused() {
        return this.searchTextField != null && this.searchTextField.isFocused();
    }

    private boolean clearSearchText() {
        if (this.searchTextField == null || !this.searchTextField.hasText()) {
            return false;
        }

        this.searchTextField.clearText();
        this.focusSearchTextField();

        return true;
    }

    private boolean keyPressedOptionListNavigation(int keyCode) {
        if (this.tabFrame == null || this.rootFrame == null) {
            return false;
        }

        OptionRow target;
        boolean handled = true;

        switch (keyCode) {
            case Keyboard.KEY_HOME -> {
                this.tabFrame.scrollSelectedPageToStart();
                target = this.tabFrame.findFirstSelectedOptionRow();
            }
            case Keyboard.KEY_END -> {
                this.tabFrame.scrollSelectedPageToEnd();
                target = this.tabFrame.findLastSelectedOptionRow();
            }
            case Keyboard.KEY_PRIOR -> {
                handled = this.tabFrame.scrollSelectedPage(-1);
                target = this.tabFrame.findFirstVisibleSelectedOptionRow();
            }
            case Keyboard.KEY_NEXT -> {
                handled = this.tabFrame.scrollSelectedPage(1);
                target = this.tabFrame.findLastVisibleSelectedOptionRow();
            }
            default -> {
                return false;
            }
        }

        return this.focusOptionRow(target) || handled;
    }

    private boolean undoFocusedOption() {
        OptionRow focusedOptionRow = this.getFocusedOptionRow();
        if (focusedOptionRow == null || !(focusedOptionRow.getOption() instanceof RsoOption option)) {
            return false;
        }

        if (focusedOptionRow.undoFocusedActionButton()) {
            this.updateControls();
            return true;
        }

        if (!OptionUndoAction.canUndo(option)) {
            return false;
        }

        OptionUndoAction.undoChanges(option);
        focusedOptionRow.clearActionButtonFocus();
        this.updateControls();

        return true;
    }

    private boolean restoreFocusedOptionForSelectedTab() {
        if (this.tabFrame == null) {
            return false;
        }

        String tabKey = this.getSelectedTabKey();
        if (tabKey == null) {
            return false;
        }

        ResourceLocation optionId = this.uiState.focusedOptionIdsByTab().get(tabKey);

        return optionId != null && this.focusOptionRow(this.tabFrame.findSelectedOptionRow(optionId));
    }

    private boolean focusOptionRow(OptionRow optionRow) {
        if (optionRow == null || this.rootFrame == null || !this.rootFrame.focusOptionRow(optionRow)) {
            return false;
        }

        this.setFocused(this.rootFrame);

        return true;
    }

    private void afterInput(String previousTabKey) {
        if (this.isSearchTextFieldFocused()) {
            return;
        }

        if (!Objects.equals(previousTabKey, this.getSelectedTabKey()) && this.restoreFocusedOptionForSelectedTab()) {
            return;
        }

        this.rememberCurrentOptionFocus();
    }

    private void rememberCurrentOptionFocus() {
        OptionRow focusedOptionRow = this.getFocusedOptionRow();
        String tabKey = this.getSelectedTabKey();

        if (tabKey != null && focusedOptionRow != null && focusedOptionRow.getOption() instanceof RsoOption rsoOption) {
            String optionId = rsoOption.rso$getId();
            if (!optionId.isEmpty()) {
                this.uiState.focusedOptionIdsByTab().put(tabKey, new ResourceLocation(optionId));
            }
        }
    }

    private OptionRow getFocusedOptionRow() {
        return this.rootFrame == null ? null : findFocusedOptionRow(this.rootFrame);
    }

    private boolean handleFocusedOptionBackNavigation() {
        OptionRow focusedOptionRow = this.getFocusedOptionRow();

        return focusedOptionRow != null && focusedOptionRow.handleBackNavigation();
    }

    private static OptionRow findFocusedOptionRow(GuiEventListener listener) {
        if (listener instanceof OptionRow optionRow && optionRow.isFocused()) {
            return optionRow;
        }

        if (listener instanceof ContainerEventHandler container) {
            GuiEventListener focused = container.getFocused();
            if (focused != null) {
                return findFocusedOptionRow(focused);
            }
        }

        return null;
    }

    private String getSelectedTabKey() {
        return this.tabFrame == null ? null : this.tabFrame.getSelectedTabKey().orElse(null);
    }

    private void keyPressedArrow(KeyEvent event, ScreenDirection direction) {
        GuiEventListener focused = this.getFocused();
        if (focused != null && focused.keyPressed(event)) {
            this.clearArrowNavigationMemory();
            this.rememberCurrentOptionFocus();
            return;
        }

        ComponentPath currentFocusPath = this.getCurrentFocusPath();
        if (this.restorePreviousArrowFocus(direction, currentFocusPath)) {
            this.rememberCurrentOptionFocus();
            return;
        }

        ComponentPath nextFocusPath = this.nextFocusPath(new FocusNavigationEvent.ArrowNavigation(direction));
        if (nextFocusPath == null) {
            this.clearArrowNavigationMemory();
            return;
        }

        this.changeFocus(nextFocusPath);
        this.rememberArrowNavigation(direction, currentFocusPath, nextFocusPath);
        this.rememberCurrentOptionFocus();
    }

    private boolean restorePreviousArrowFocus(ScreenDirection direction, ComponentPath currentFocusPath) {
        if (this.previousArrowFocusPath == null
                || this.lastArrowDirection == null
                || currentFocusPath == null
                || direction != this.lastArrowDirection.getOpposite()
                || currentFocusPath.leafComponent() != this.currentArrowFocusLeaf
                || !this.containsFocusLeaf(this.previousArrowFocusPath.leafComponent())) {
            return false;
        }

        ComponentPath previousPath = this.previousArrowFocusPath;
        this.changeFocus(previousPath);
        this.previousArrowFocusPath = currentFocusPath;
        this.currentArrowFocusLeaf = previousPath.leafComponent();
        this.lastArrowDirection = direction;

        return true;
    }

    private void rememberArrowNavigation(ScreenDirection direction, ComponentPath previousPath, ComponentPath currentPath) {
        if (previousPath == null || previousPath.leafComponent() == currentPath.leafComponent()) {
            this.clearArrowNavigationMemory();
            return;
        }

        this.previousArrowFocusPath = previousPath;
        this.currentArrowFocusLeaf = currentPath.leafComponent();
        this.lastArrowDirection = direction;
    }

    private void clearArrowNavigationMemory() {
        this.previousArrowFocusPath = null;
        this.currentArrowFocusLeaf = null;
        this.lastArrowDirection = null;
    }

    private boolean containsFocusLeaf(GuiEventListener leaf) {
        return containsFocusLeaf(this.rootFrame, leaf);
    }

    private static boolean containsFocusLeaf(GuiEventListener component, GuiEventListener leaf) {
        if (component == leaf) {
            return true;
        }

        if (component instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                if (containsFocusLeaf(child, leaf)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static ScreenDirection getArrowDirection(int keyCode) {
        return switch (keyCode) {
            case Keyboard.KEY_LEFT -> ScreenDirection.LEFT;
            case Keyboard.KEY_RIGHT -> ScreenDirection.RIGHT;
            case Keyboard.KEY_UP -> ScreenDirection.UP;
            case Keyboard.KEY_DOWN -> ScreenDirection.DOWN;
            default -> null;
        };
    }

    private ComponentPath getCurrentFocusPath() {
        GuiEventListener focused = this.getFocused();
        if (focused instanceof ContainerEventHandler container) {
            return container.getCurrentFocusPath();
        }
        return focused == null ? null : ComponentPath.leaf(focused);
    }

    /** Returns the currently focused child element, or null. */
    public GuiEventListener getFocused() {
        return this.focused;
    }

    /** Sets the focused child element, updating focus state on the old and new elements. */
    public void setFocused(GuiEventListener focused) {
        if (this.focused == focused) {
            return;
        }
        if (this.focused != null) {
            this.focused.setFocused(false);
        }
        this.focused = focused;
        if (focused != null) {
            focused.setFocused(true);
        }
    }

    private ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        if (this.rootFrame == null) {
            return null;
        }
        return this.rootFrame.nextFocusPath(navigation);
    }

    private void changeFocus(ComponentPath path) {
        if (path != null && path.leafComponent() != null) {
            GuiEventListener leaf = path.leafComponent();
            leaf.setFocused(true);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        this.uiState.lastSearch().set("");
        this.uiState.lastSearchIndex().set(0);
        this.uiState.updateSearchResults(false, List.of());
        this.uiState.clearOptionUiStates();
        this.uiState.focusedOptionIdsByTab().clear();
    }

    public void onClose() {
        this.mc.displayGuiScreen(this.prevScreen);
    }
}
