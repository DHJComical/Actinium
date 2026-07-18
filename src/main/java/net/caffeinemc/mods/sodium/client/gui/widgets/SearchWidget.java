package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.config.search.OptionTextSource;
import net.caffeinemc.mods.sodium.client.config.search.SearchQuerySession;
import net.caffeinemc.mods.sodium.client.config.search.TextSource;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Native 1.12.2 text field backed by Sodium's language-aware search session. */
public final class SearchWidget extends AbstractWidget {
    private static final int CLEAR_WIDTH = 20;
    private final SearchQuerySession session;
    private final Consumer<List<OptionTextSource>> listener;
    private GuiTextField field;
    private String lastQuery = "";
    private List<OptionTextSource> results = List.of();

    public SearchWidget(GuiRect bounds, SearchQuerySession session, Consumer<List<OptionTextSource>> listener) {
        super(bounds);
        this.session = session;
        this.listener = listener;
        this.rebuildField();
    }

    @Override
    public void setBounds(GuiRect bounds) {
        String text = this.field.getText();
        boolean wasFocused = this.field.isFocused();
        super.setBounds(bounds);
        this.rebuildField();
        this.field.setText(text);
        this.field.setFocused(wasFocused);
    }

    private void rebuildField() {
        this.field = new GuiTextField(0, this.font, this.bounds.x() + 6, this.bounds.y() + 3,
                Math.max(20, this.bounds.width() - CLEAR_WIDTH - 10), this.bounds.height() - 6);
        this.field.setEnableBackgroundDrawing(false);
        this.field.setMaxStringLength(200);
    }

    public void render(int mouseX, int mouseY) {
        Gui.drawRect(this.bounds.x(), this.bounds.y(), this.bounds.right(), this.bounds.bottom(),
                Colors.BACKGROUND_DEFAULT);
        this.field.drawTextBox();
        if (this.field.getText().isEmpty() && !this.field.isFocused()) {
            this.font.drawString(I18n.format("sodium.options.search.hint"), this.bounds.x() + 6,
                    this.bounds.y() + (this.bounds.height() - this.font.FONT_HEIGHT) / 2, Colors.FOREGROUND_DISABLED);
        } else if (!this.field.getText().isEmpty()) {
            int clearX = this.bounds.right() - CLEAR_WIDTH;
            boolean hovered = mouseX >= clearX && this.bounds.contains(mouseX, mouseY);
            Gui.drawRect(clearX, this.bounds.y(), this.bounds.right(), this.bounds.bottom(),
                    hovered ? Colors.BACKGROUND_MEDIUM : Colors.BACKGROUND_LIGHT);
            this.font.drawString("\u00d7", clearX + (CLEAR_WIDTH - this.font.getStringWidth("\u00d7")) / 2,
                    this.bounds.y() + (this.bounds.height() - this.font.FONT_HEIGHT) / 2, Colors.FOREGROUND);
        }
    }

    public void update() {
        this.field.updateCursorCounter();
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && !this.field.getText().isEmpty()
                && mouseX >= this.bounds.right() - CLEAR_WIDTH && this.bounds.contains(mouseX, mouseY)) {
            this.clear();
            return true;
        }
        this.field.mouseClicked(mouseX, mouseY, button);
        this.focused = this.field.isFocused();
        return this.focused;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!this.field.isFocused() || !this.field.textboxKeyTyped(typedChar, keyCode)) {
            return false;
        }
        this.searchIfChanged();
        return true;
    }

    private void searchIfChanged() {
        String query = this.field.getText().stripLeading();
        if (query.equals(this.lastQuery)) {
            return;
        }
        this.lastQuery = query;
        if (query.isEmpty()) {
            this.results = List.of();
        } else {
            List<? extends TextSource> raw = this.session.getSearchResults(query);
            List<OptionTextSource> typed = new ArrayList<>();
            Set<Option> seen = new LinkedHashSet<>();
            for (TextSource source : raw) {
                if (!(source instanceof OptionTextSource result)) {
                    throw new IllegalStateException("Unsupported Sodium search result type: " + source.getClass().getName());
                }
                if (seen.add(result.getOption())) {
                    result.setResultIndex(typed.size());
                    typed.add(result);
                }
            }
            this.results = List.copyOf(typed);
        }
        this.listener.accept(this.results);
    }

    public void clear() {
        this.field.setText("");
        this.searchIfChanged();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        this.field.setFocused(focused);
    }

    public List<OptionTextSource> getResults() {
        return this.results;
    }

    public boolean hasQuery() {
        return !this.lastQuery.isEmpty();
    }
}
