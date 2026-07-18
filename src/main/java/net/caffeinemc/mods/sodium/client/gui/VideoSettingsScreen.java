package net.caffeinemc.mods.sodium.client.gui;

import net.caffeinemc.mods.sodium.client.config.search.OptionTextSource;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.caffeinemc.mods.sodium.client.gui.options.control.OptionControl;
import net.caffeinemc.mods.sodium.client.gui.input.FocusNavigator;
import net.caffeinemc.mods.sodium.client.gui.input.FocusTarget;
import net.caffeinemc.mods.sodium.client.gui.resource.IconResourceValidator;
import net.caffeinemc.mods.sodium.client.gui.resource.ResourceValidator;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.OptionListWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.PageListWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.ScrollableTooltip;
import net.caffeinemc.mods.sodium.client.gui.widgets.SearchWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Sodium configuration screen adapted to the Minecraft 1.12.2 GuiScreen lifecycle.
 */
public final class VideoSettingsScreen extends GuiScreen {
    private static final Logger LOGGER = LogManager.getLogger("SodiumGui");
    private final GuiScreen previousScreen;
    private final Config config;
    private final ResourceValidator resourceValidator;
    private final FocusNavigator focusNavigator = new FocusNavigator();
    private Layout.ScreenLayout layout;
    private SearchWidget search;
    private PageListWidget pages;
    private OptionListWidget options;
    private ScrollableTooltip tooltip;
    private FlatButtonWidget applyButton;
    private FlatButtonWidget undoButton;
    private FlatButtonWidget doneButton;
    private List<FlatButtonWidget> footerButtons = List.of();
    private OptionPage selectedPage;
    private ModOptions selectedOwner;
    private boolean searchFiltering;
    private String statusMessage;

    public VideoSettingsScreen(GuiScreen previousScreen, Config config) {
        this(previousScreen, config, VideoSettingsScreen::requireMinecraftResource);
    }

    public VideoSettingsScreen(GuiScreen previousScreen, Config config, ResourceValidator resourceValidator) {
        if (config == null) {
            throw new IllegalArgumentException("Sodium settings screen requires a frozen Config model");
        }
        if (resourceValidator == null) {
            throw new IllegalArgumentException("Sodium settings screen requires an icon resource validator");
        }
        this.previousScreen = previousScreen;
        this.config = config;
        this.resourceValidator = resourceValidator;
    }

    /** Explicit construction entry used by the platform entry mixin or another owning screen. */
    public static GuiScreen createScreen(GuiScreen previousScreen, Config config) {
        return new VideoSettingsScreen(previousScreen, config);
    }

    public static GuiScreen createScreen(GuiScreen previousScreen, Config config, ResourceValidator validator) {
        return new VideoSettingsScreen(previousScreen, config, validator);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        IconResourceValidator.validate(this.config.getModOptions(), this.resourceValidator);
        this.layout = Layout.calculate(this.width, this.height);
        this.search = new SearchWidget(this.layout.search(), this.config.startSearchQuery(), this::onSearchResults);
        this.pages = new PageListWidget(this.layout.pages(), this.config.getModOptions(), this, this::selectPage);
        this.options = new OptionListWidget(this.layout.options(), this.config, this);
        this.tooltip = new ScrollableTooltip(this.layout.tooltip(), this.layout.tooltipOverlay());
        this.buildFooter();

        OptionPage initialPage = this.selectedPage;
        if (initialPage == null || this.findOwner(initialPage) == null) {
            initialPage = this.firstOptionPage();
        }
        if (initialPage != null) {
            this.selectPage(initialPage);
        } else {
            this.refreshFocusTargets();
        }
    }

    private void buildFooter() {
        Layout.ActionButtons buttons = this.layout.actionButtons();
        this.undoButton = new FlatButtonWidget(buttons.undo(), "Undo", this.config::undoChanges);
        this.applyButton = new FlatButtonWidget(buttons.apply(), "Apply", this::applyChanges);
        this.doneButton = new FlatButtonWidget(buttons.done(), I18n.format("gui.done"), this::closeClean);
        this.footerButtons = List.of(this.undoButton, this.applyButton, this.doneButton);
    }

    private void onSearchResults(List<OptionTextSource> results) {
        this.searchFiltering = this.search.hasQuery();
        if (this.searchFiltering) {
            this.options.showSearchResults(results);
        } else if (this.selectedPage != null) {
            this.options.showPage(this.selectedOwner, this.selectedPage);
        }
        this.tooltip.setTarget(null);
        this.refreshFocusTargets();
    }

    private void selectPage(Page page) {
        if (!(page instanceof OptionPage optionPage)) {
            throw new IllegalArgumentException("Only option pages can be selected in the Sodium option viewport");
        }
        ModOptions owner = this.findOwner(optionPage);
        if (owner == null) {
            throw new IllegalArgumentException("Page is not owned by this Config model: " + page.name().getUnformattedText());
        }
        this.selectedPage = optionPage;
        this.selectedOwner = owner;
        this.pages.select(optionPage);
        if (!this.searchFiltering) {
            this.options.showPage(owner, optionPage);
        }
        this.tooltip.setTarget(null);
        this.refreshFocusTargets();
    }

    private ModOptions findOwner(Page page) {
        for (ModOptions owner : this.config.getModOptions()) {
            if (owner.pages().contains(page)) {
                return owner;
            }
        }
        return null;
    }

    private OptionPage firstOptionPage() {
        for (ModOptions owner : this.config.getModOptions()) {
            for (Page page : owner.pages()) {
                if (page instanceof OptionPage optionPage) {
                    return optionPage;
                }
            }
        }
        return null;
    }

    private void applyChanges() {
        try {
            this.config.applyChanges();
            this.statusMessage = null;
        } catch (RuntimeException exception) {
            this.statusMessage = "Apply failed: " + exception.getMessage();
            LOGGER.error("Sodium GUI failed to apply pending configuration", exception);
        }
    }

    private void closeClean() {
        if (this.config.hasPendingChanges()) {
            throw new IllegalStateException("Done cannot close while unapplied Sodium options are pending");
        }
        this.mc.displayGuiScreen(this.previousScreen);
    }

    private void discardAndClose() {
        this.config.discardChanges();
        this.mc.displayGuiScreen(this.previousScreen);
    }

    @Override
    public void updateScreen() {
        this.search.update();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        boolean dirty = this.config.hasPendingChanges();
        this.applyButton.setEnabled(dirty);
        this.undoButton.setEnabled(dirty);
        this.undoButton.setVisible(dirty);
        this.doneButton.setEnabled(!dirty);

        this.search.render(mouseX, mouseY);
        this.pages.render(mouseX, mouseY, this.width, this.height);
        this.options.render(mouseX, mouseY, this.width, this.height);
        OptionControl<?> hovered = this.tooltip.capturesInput(mouseX, mouseY)
                ? null : this.options.getHoveredControl(mouseX, mouseY);
        FocusTarget focusedTarget = this.focusNavigator.focused();
        OptionControl<?> focused = focusedTarget instanceof OptionControl<?> control ? control : null;
        this.tooltip.updateTarget(hovered, focused, mouseX, mouseY);
        this.tooltip.render();
        for (FlatButtonWidget button : this.footerButtons) {
            button.render(mouseX, mouseY);
        }
        if (this.statusMessage != null) {
            String message = this.fontRenderer.trimStringToWidth(this.statusMessage,
                    Math.max(20, this.layout.content().width() - 10));
            this.fontRenderer.drawString(message, this.layout.content().x() + 5,
                    this.layout.content().y() - this.fontRenderer.FONT_HEIGHT - 2, 0xFFFF7777);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.tooltip.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (this.search.mouseClicked(mouseX, mouseY, mouseButton)) {
            this.focusTarget(this.search);
            return;
        }
        if (this.pages.mouseClicked(mouseX, mouseY, mouseButton)) {
            this.focusTarget(null);
            return;
        }
        if (this.options.mouseClicked(mouseX, mouseY, mouseButton)) {
            this.focusTarget(this.options.getFocusedControl());
            return;
        }
        for (FlatButtonWidget button : this.footerButtons) {
            if (mouseButton == 0 && button.getBounds().contains(mouseX, mouseY)) {
                this.focusTarget(button);
            }
            if (button.mouseClicked(mouseX, mouseY, mouseButton)) {
                return;
            }
        }
        this.focusTarget(null);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.tooltip.capturesInput(mouseX, mouseY)) {
            return;
        }
        this.pages.mouseDragged(mouseY);
        this.options.mouseDragged(mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.pages.mouseReleased();
        this.options.mouseReleased();
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int delta = wheel > 0 ? -Layout.ENTRY_HEIGHT : Layout.ENTRY_HEIGHT;
        if (this.tooltip.mouseScrolled(mouseX, mouseY, delta)) {
            return;
        }
        if (this.layout.pages().contains(mouseX, mouseY)) {
            this.pages.mouseScrolled(delta);
        } else if (this.layout.options().contains(mouseX, mouseY)) {
            this.options.scroll(delta);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.discardAndClose();
            return;
        }
        if (keyCode == Keyboard.KEY_TAB || keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) {
            int direction = keyCode == Keyboard.KEY_UP || (keyCode == Keyboard.KEY_TAB && isShiftDown()) ? -1 : 1;
            this.focusTarget(this.focusNavigator.move(direction));
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN && this.search.isFocused() && !this.search.getResults().isEmpty()) {
            this.navigateToResult(this.search.getResults().get(0));
            return;
        }
        if (this.search.keyTyped(typedChar, keyCode)) {
            return;
        }
        if (keyCode == Keyboard.KEY_T && !this.search.isFocused()) {
            this.focusTarget(this.search);
            return;
        }
        if (isAltDown()) {
            switch (Character.toLowerCase(typedChar)) {
                case 'a' -> this.applyChanges();
                case 'u' -> this.config.undoChanges();
                case 'r' -> this.config.resetToDefaults();
                case 'd' -> {
                    if (!this.config.hasPendingChanges()) {
                        this.closeClean();
                    }
                }
                default -> {
                }
            }
            return;
        }
        FocusTarget focused = this.focusNavigator.focused();
        if (focused instanceof OptionControl<?> control && control.keyPressed(keyCode)) {
            return;
        }
        if (focused instanceof FlatButtonWidget button && button.keyPressed(keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void navigateToResult(OptionTextSource result) {
        this.selectedPage = result.getPage();
        this.selectedOwner = result.getModOptions();
        this.pages.select(this.selectedPage);
        this.search.clear();
        this.options.focusOption(result.getOption());
        this.refreshFocusTargets();
        this.focusTarget(this.options.getFocusedControl());
    }

    private void refreshFocusTargets() {
        if (this.search == null || this.options == null) {
            return;
        }
        List<FocusTarget> targets = new ArrayList<>();
        targets.add(this.search);
        targets.addAll(this.options.getFocusTargets());
        targets.addAll(this.footerButtons);
        this.focusNavigator.replaceTargets(targets);
    }

    private void focusTarget(FocusTarget target) {
        if (!this.focusNavigator.focus(target) && target != null) {
            this.focusNavigator.focus(null);
            target = null;
        }
        this.options.adoptFocus(target instanceof OptionControl<?> control ? control : null);
    }

    private static boolean isAltDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    private static boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    private static void requireMinecraftResource(ResourceLocation resource) {
        try (IResource ignored = Minecraft.getMinecraft().getResourceManager().getResource(resource)) {
            // Opening the resource is the authoritative 1.12.2 existence check.
        } catch (IOException exception) {
            throw new IllegalStateException("Minecraft resource is unavailable: " + resource, exception);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
