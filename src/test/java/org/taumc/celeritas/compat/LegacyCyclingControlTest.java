package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;
import org.junit.jupiter.api.Test;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionStorage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyCyclingControlTest {
    @Test
    void usesTextProviderNamesBeforeEnumNames() {
        Settings settings = new Settings();
        OptionStorage<Settings> storage = () -> settings;
        OptionImpl<Settings, LocalizedMode> option = OptionImpl.createBuilder(LocalizedMode.class, storage)
                .setId(OptionIdentifier.create("compat_test", "localized_mode", LocalizedMode.class))
                .setName(TextComponent.literal("Localized mode"))
                .setTooltip(TextComponent.literal("Localized mode tooltip"))
                .setBinding((data, value) -> data.mode = value, data -> data.mode)
                .setControl(value -> new CyclingControl<>(value, LocalizedMode.class))
                .build();

        CyclingControl<?> control = (CyclingControl<?>) option.getControl();
        assertArrayEquals(new TextComponent[] {
                TextComponent.translatable("compat.mode.first"),
                TextComponent.translatable("compat.mode.second")
        }, control.getNames());
        assertEquals(70, control.getMaxWidth());
    }

    @Test
    void preservesOriginalArrayIdentityForExplicitValuesAndNames() {
        LocalizedMode[] values = LocalizedMode.values();
        TextComponent[] names = {
                TextComponent.literal("First"),
                TextComponent.literal("Second")
        };
        CyclingControl<LocalizedMode> control = new CyclingControl<>(null, values, names);
        assertSame(values, control.getAllowedValues());
        assertSame(names, control.getNames());
    }

    @Test
    void rejectsValuesWithoutTextProviderOrEnumNames() {
        assertThrows(IllegalArgumentException.class,
                () -> new CyclingControl<Integer>(null, Integer.class, new Integer[] { 1 }));
    }

    private enum LocalizedMode implements TextProvider {
        FIRST("compat.mode.first"),
        SECOND("compat.mode.second");

        private final String translationKey;

        LocalizedMode(String translationKey) {
            this.translationKey = translationKey;
        }

        @Override
        public TextComponent getLocalizedName() {
            return TextComponent.translatable(translationKey);
        }
    }

    private static final class Settings {
        private LocalizedMode mode = LocalizedMode.FIRST;
    }
}
