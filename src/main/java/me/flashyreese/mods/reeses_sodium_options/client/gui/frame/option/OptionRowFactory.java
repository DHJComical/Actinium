package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoOption;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.ActiniumTheme;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OptionRowFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger("Reese's Sodium Options");

    private final GuiScreen screen;
    private final ActiniumTheme sodiumTheme;
    private final GuiTheme theme;
    private final OptionStateStore optionStateStore;

    OptionRowFactory(GuiScreen screen, ActiniumTheme sodiumTheme, GuiTheme theme, OptionStateStore optionStateStore) {
        this.screen = screen;
        this.sodiumTheme = sodiumTheme;
        this.theme = theme;
        this.optionStateStore = optionStateStore;
    }

    OptionRow create(Option<?> option, LayoutBounds dim) {
        OptionRow element = this.createOptionRow(option, dim);

        this.registerOptionBounds(element, dim);

        return element;
    }

    void registerParentBounds(PageLayout layout, LayoutBounds parentDim) {
        for (PageLayout.Row row : layout.rows()) {
            if (row instanceof PageLayout.OptionRow optionRow) {
                ResourceLocation id = PageLayout.optionId(optionRow.option());
                if (id != null) {
                    this.optionStateStore.optionLayoutState(id)
                            .setParentBounds(parentDim);
                }
            }
        }
    }

    void registerOptionBounds(OptionRow element, LayoutBounds dim) {
        String id = element.getOption().rso$getId();
        if (id == null || id.isEmpty()) {
            return;
        }
        this.optionStateStore.optionLayoutState(new ResourceLocation(id))
                .setBounds(dim);
    }

    private OptionRow createOptionRow(Option<?> option, LayoutBounds dim) {
        // The embeddium option model carries no upstream Control objects; the
        // concrete control type selects the row implementation directly.
        return switch (option) {
            case Option<?> o when o.getControl() instanceof org.embeddedt.embeddium.api.options.control.TickBoxControl ->
                    new BooleanOptionRow(dim, this.theme, this.optionStateStore, new RsoOption(o));
            case Option<?> o when o.getControl() instanceof org.embeddedt.embeddium.api.options.control.SliderControl ->
                    new IntegerSliderOptionRow(dim, this.theme, this.optionStateStore, new RsoOption(o));
            case Option<?> o when o.getControl() instanceof org.embeddedt.embeddium.api.options.control.CyclingControl ->
                    new CyclingOptionRow(dim, this.theme, this.optionStateStore, new RsoOption(o));
            case Option<?> o when o.getControl() instanceof org.embeddedt.embeddium.api.options.control.ExternalButtonControl ->
                    new ExternalButtonOptionRow(this.screen, dim, this.theme, this.optionStateStore, new RsoOption(o));
            default -> this.createUnsupportedRow(option, dim);
        };
    }

    private OptionRow createUnsupportedRow(Option<?> option, LayoutBounds dim) {
        LOGGER.warn("No option row registered for option type {}; rendering it as unsupported", option.getClass().getName());
        return new UnsupportedOptionRow(dim, this.theme, this.optionStateStore, new RsoOption(option));
    }
}
