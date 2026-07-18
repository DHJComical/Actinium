package net.caffeinemc.mods.sodium.client.config.search;

import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.OptionGroup;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;

import java.util.function.Supplier;

/**
 * Search result retaining the exact page and option a GUI should navigate to.
 */
public final class OptionTextSource extends TextSource {
    /** Identifies which localized field produced this source. */
    public enum Kind {
        PAGE,
        NAME,
        DESCRIPTION
    }

    private final Option option;
    private final ModOptions modOptions;
    private final OptionPage page;
    private final OptionGroup optionGroup;
    private final Kind kind;
    private final Supplier<String> textProvider;

    public OptionTextSource(Option option, ModOptions modOptions, OptionPage page, OptionGroup optionGroup,
                            Kind kind, Supplier<String> textProvider) {
        this.option = option;
        this.modOptions = modOptions;
        this.page = page;
        this.optionGroup = optionGroup;
        this.kind = kind;
        this.textProvider = textProvider;
    }

    /** Returns the option selected by this result. */
    public Option getOption() {
        return this.option;
    }

    /** Returns the owner metadata displayed in navigation. */
    public ModOptions getModOptions() {
        return this.modOptions;
    }

    /** Returns the page selected by this result. */
    public OptionPage getPage() {
        return this.page;
    }

    /** Returns the group containing the selected option. */
    public OptionGroup getOptionGroup() {
        return this.optionGroup;
    }

    /** Returns the field which matched. */
    public Kind getKind() {
        return this.kind;
    }

    @Override
    protected String getTextFromSource() {
        return this.textProvider.get();
    }
}
