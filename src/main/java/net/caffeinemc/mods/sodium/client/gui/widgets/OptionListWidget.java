package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.config.search.OptionTextSource;
import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.EnumOption;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalButtonOption;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.OptionGroup;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.gui.options.control.BooleanControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.EnumControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.ExternalButtonControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.IntegerControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.OptionControl;
import net.caffeinemc.mods.sodium.client.gui.render.ScissorScope;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Independently scrollable option list for the currently selected page or search result. */
public final class OptionListWidget extends AbstractWidget {
    private enum HeaderKind { MOD, GROUP }
    private final Config config;
    private final GuiScreen parent;
    private final List<Row> rows = new ArrayList<>();
    private final VisibleHitMap<OptionControl<?>> visibleControls = new VisibleHitMap<>();
    private final ScrollState scroll = new ScrollState(ignored -> {
        this.visibleControls.beginFrame();
    });
    private final ScrollbarWidget scrollbar;
    private OptionControl<?> focused;
    private OptionControl<?> dragged;

    public OptionListWidget(GuiRect bounds, Config config, GuiScreen parent) {
        super(bounds);
        this.config = config;
        this.parent = parent;
        this.scrollbar = new ScrollbarWidget(new GuiRect(bounds.right() - Layout.SCROLLBAR_WIDTH, bounds.y(),
                Layout.SCROLLBAR_WIDTH, bounds.height()), this.scroll);
    }


    public void showPage(ModOptions owner, OptionPage page) {
        this.rows.clear();
        int y = 0;
        this.rows.add(Row.header("◆ " + page.name().getUnformattedText(), owner.theme(), y));
        y += Layout.ENTRY_HEIGHT + Layout.OPTION_PAGE_MARGIN;
        for (OptionGroup group : page.groups()) {
            y += Layout.OPTION_GROUP_MARGIN;
            if (group.name() != null && !group.name().getUnformattedText().isBlank()) {
                this.rows.add(Row.header(group.name().getUnformattedText(), owner.theme(), y));
                y += Layout.ENTRY_HEIGHT;
            }
            for (Option option : group.options()) {
                this.rows.add(Row.control(this.createControl(option, owner.theme()), y));
                y += Layout.ENTRY_HEIGHT;
            }
        }
        this.finishRebuild(y + Layout.INNER_MARGIN);
    }

    public void showSearchResults(List<OptionTextSource> results) {
        this.rows.clear();
        int y = 0;
        Set<Option> seen = new LinkedHashSet<>();
        ModOptions lastMod = null;
        Page lastPage = null;
        for (OptionTextSource result : results) {
            if (!seen.add(result.getOption())) {
                continue;
            }
            if (result.getModOptions() != lastMod) {
                lastMod = result.getModOptions();
                y += Layout.OPTION_MOD_MARGIN;
                this.rows.add(Row.header(lastMod.name(), lastMod.theme(), y, HeaderKind.MOD, null));
                y += Layout.ENTRY_HEIGHT;
                lastPage = null;
            }
            if (result.getPage() != lastPage) {
                lastPage = result.getPage();
                y += Layout.OPTION_PAGE_MARGIN;
                this.rows.add(Row.header("◆ " + lastPage.name().getUnformattedText(), lastMod.theme(), y));
                y += Layout.ENTRY_HEIGHT;
            }
            this.rows.add(Row.control(this.createControl(result.getOption(), lastMod.theme()), y));
            y += Layout.ENTRY_HEIGHT;
        }
        this.finishRebuild(y);
    }

    private void finishRebuild(int totalHeight) {
        this.setFocused(null);
        this.visibleControls.beginFrame();
        this.scroll.scrollTo(0);
        this.scroll.setContext(this.bounds.height(), totalHeight);
    }

    public void render(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        this.visibleControls.beginFrame();
        try (ScissorScope ignored = ScissorScope.open(this.bounds, screenWidth, screenHeight)) {
            for (Row row : this.rows) {
                int y = this.bounds.y() + row.contentY() - this.scroll.amount();
                if (y + row.height() <= this.bounds.y() || y >= this.bounds.bottom()) {
                    continue;
                }
                if (row.control() == null) {
                    Gui.drawRect(this.bounds.x(), y, this.bounds.right() - Layout.SCROLLBAR_WIDTH,
                            y + row.height(), row.theme().theme() == Colors.FOREGROUND ? Colors.BACKGROUND_MEDIUM : Colors.BACKGROUND_DEFAULT);
                    this.font.drawString(this.font.trimStringToWidth(row.title(),
                                    Math.max(1, this.bounds.width() - Layout.SCROLLBAR_WIDTH - 12)),
                            this.bounds.x() + Layout.OPTION_PAGE_MARGIN,
                            y + row.height() / 2 + Layout.REGULAR_TEXT_BASELINE_OFFSET,
                            row.theme().themeHighlight());
                } else if (row.external() == null) {
                    GuiRect rowBounds = new GuiRect(this.bounds.x() + Layout.OPTION_LEFT_INSET, y,
                            this.bounds.width() - Layout.SCROLLBAR_WIDTH - Layout.OPTION_LIST_SCROLLBAR_OFFSET
                                    - Layout.OPTION_LEFT_INSET, row.height());
                    row.control().setBounds(rowBounds);
                    GuiRect visible = rowBounds.intersection(this.bounds);
                    if (visible.width() > 0 && visible.height() > 0) {
                        this.visibleControls.add(row.control(), visible);
                    }
                    row.control().render(mouseX, mouseY);
                } else {
                    this.font.drawString(row.title(), this.bounds.x() + Layout.OPTION_PAGE_MARGIN,
                            y + row.height() / 2 + Layout.REGULAR_TEXT_BASELINE_OFFSET, row.theme().theme());
                }
            }
        }
        this.scrollbar.render();
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (this.scrollbar.mouseClicked(mouseX, mouseY, button)) return true;
        if (!this.bounds.contains(mouseX, mouseY)) return false;
        int scrollY = this.scroll.amount();
        for (Row row : this.rows) {
            if (row.external() != null) {
                GuiRect externalBounds = new GuiRect(this.bounds.x(), this.bounds.y() + row.contentY() - scrollY,
                        this.bounds.width() - Layout.SCROLLBAR_WIDTH, row.height());
                if (externalBounds.contains(mouseX, mouseY) && button == 0) {
                    row.external().screenConsumer().accept(this.parent);
                    return true;
                }
            }
        }
        OptionControl<?> control = this.visibleControls.find(mouseX, mouseY);
        if (control == null) { this.setFocused(null); return false; }
        this.setFocused(control);
        boolean handled = control.mouseClicked(mouseX, mouseY, button);
        this.dragged = handled ? control : null;
        return handled;
    }

    public void scroll(int delta) { this.scroll.scroll(delta); }
    public void mouseDragged(int mouseX, int mouseY) { this.scrollbar.mouseDragged(mouseY); if (this.dragged != null) this.dragged.mouseDragged(mouseX, mouseY); }
    public void mouseReleased() { this.scrollbar.mouseReleased(); this.dragged = null; }
    public OptionControl<?> getHoveredControl(int mouseX, int mouseY) { return this.bounds.contains(mouseX, mouseY) ? this.visibleControls.find(mouseX, mouseY) : null; }
    public List<OptionControl<?>> getFocusTargets() { return this.rows.stream().map(Row::control).filter(control -> control != null).toList(); }
    public OptionControl<?> getFocusedControl() { return this.focused != null && this.focused.isFocused() ? this.focused : null; }
    public void adoptFocus(OptionControl<?> control) { this.setFocused(control); }
    public void focusOption(Option option) { for (Row row : this.rows) if (row.control() != null && row.control().getOption() == option) { this.setFocused(row.control()); this.scroll.scrollTo(row.contentY()); return; } throw new IllegalArgumentException("Option is not present in the current list: " + option.getId()); }
    public boolean keyPressed(int keyCode) { return this.focused != null && this.focused.keyPressed(keyCode); }
    private void setFocused(OptionControl<?> control) { if (this.focused != null) this.focused.setFocused(false); this.focused = control; if (control != null) control.setFocused(true); }

    private OptionControl<?> createControl(Option option, ColorTheme theme) {
        if (option instanceof BooleanOption value) return new BooleanControl(value, theme, this.font);
        if (option instanceof IntegerOption value) return new IntegerControl(value, theme, this.font);
        if (option instanceof EnumOption<?> value) return createEnumControl(value, theme);
        if (option instanceof ExternalButtonOption value) return new ExternalButtonControl(value, theme, this.font, this.parent);
        throw new IllegalStateException("Unsupported Sodium option type: " + option.getClass().getName());
    }
    private <E extends Enum<E>> EnumControl<E> createEnumControl(EnumOption<E> option, ColorTheme theme) { return new EnumControl<>(option, theme, this.font, this.config); }
    private static void resetMod(ModOptions owner) {
        owner.pages().forEach(page -> {
            if (page instanceof OptionPage optionPage) {
                optionPage.groups().forEach(group -> group.options().forEach(Option::resetToDefault));
            }
        });
    }
    private record Row(String title, ColorTheme theme, OptionControl<?> control, ExternalPage external, int contentY, int height) {
        static Row header(String title, ColorTheme theme, int y) { return new Row(title, theme, null, null, y, Layout.ENTRY_HEIGHT); }
        static Row header(String title, ColorTheme theme, int y, HeaderKind kind, Runnable reset) { return header(title, theme, y); }
        static Row control(OptionControl<?> control, int y) { return new Row(null, null, control, null, y, Layout.ENTRY_HEIGHT); }
        static Row external(ExternalPage page, ColorTheme theme, int y) { return new Row(page.name().getUnformattedText(), theme, null, page, y, Layout.ENTRY_HEIGHT); }
    }
}
