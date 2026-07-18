package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.util.ResourceLocation;

/**
 * Root factory for a single explicit configuration registration.
 */
public interface ConfigBuilder {
    /** Registers a mod-owned model with explicit metadata. */
    ModOptionsBuilder registerModOptions(String configId, String name, String version);

    /** Registers a mod-owned model using metadata supplied to the registry. */
    ModOptionsBuilder registerModOptions(String configId);

    /** Registers a model for the current entry point's source ID. */
    ModOptionsBuilder registerOwnModOptions();

    /** Creates renderer-independent theme data for a mod options model. */
    ColorThemeBuilder createColorTheme();

    /** Creates an option page builder. */
    OptionPageBuilder createOptionPage();

    /** Creates an external page builder. */
    ExternalPageBuilder createExternalPage();

    /** Creates an option group builder. */
    OptionGroupBuilder createOptionGroup();

    /** Creates a boolean option builder with a stable ID. */
    BooleanOptionBuilder createBooleanOption(ResourceLocation id);

    /** Creates an integer option builder with a stable ID. */
    IntegerOptionBuilder createIntegerOption(ResourceLocation id);

    /** Creates an enum option builder with a stable ID and explicit enum type. */
    <E extends Enum<E>> EnumOptionBuilder<E> createEnumOption(ResourceLocation id, Class<E> enumClass);

    /** Creates a non-stateful option which opens an external screen. */
    ExternalButtonOptionBuilder createExternalButtonOption(ResourceLocation id);
}
