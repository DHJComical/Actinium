package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.api.options.structure.OptionStorage;

import java.util.List;

final class LegacyOptionFixtures {
    private LegacyOptionFixtures() {
    }

    static Option<Boolean> option(String path) {
        Box box = new Box();
        OptionStorage<Box> storage = () -> box;
        return OptionImpl.createBuilder(Boolean.class, storage)
                .setId(OptionIdentifier.create("compat_test", path, Boolean.class))
                .setName(TextComponent.literal(path))
                .setTooltip(TextComponent.literal(path + " tooltip"))
                .setBinding((data, value) -> data.value = value, data -> data.value)
                .setControl(TickBoxControl::new)
                .build();
    }

    static OptionGroup group(String path) {
        return OptionGroup.createBuilder()
                .setId(OptionIdentifier.create("compat_test", path))
                .add(option(path + "_option"))
                .build();
    }

    static OptionPage page(String path) {
        return new OptionPage(OptionIdentifier.create("compat_test", path), TextComponent.literal(path),
                List.of(group(path + "_group")));
    }

    private static final class Box {
        private boolean value;
    }
}
