package me.flashyreese.mods.reeses_sodium_options.client.gui.option;

import net.minecraft.util.ResourceLocation;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionImpl;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RsoModOptionsTest {
    @Test
    void createBindsMetadataAndWrapsPages() {
        OptionPage page = page("general");
        RsoModMetadata metadata = new RsoModMetadata("Celeritas", "2.4.0-dev",
                new ResourceLocation("celeritas", "textures/gui/config-icon.png"), false);

        RsoModOptions options = RsoModOptions.create("celeritas", metadata, List.of(page));

        assertEquals("celeritas", options.configId());
        assertEquals("Celeritas", options.name());
        assertEquals("2.4.0-dev", options.version());
        assertEquals(new ResourceLocation("celeritas", "textures/gui/config-icon.png"), options.icon());
        assertFalse(options.iconMonochrome());
        assertNotNull(options.theme());
        assertEquals(1, options.pages().size());
        assertEquals("general", options.pages().getFirst().name().getUnformattedText());
        assertEquals(1, options.pages().getFirst().groups().size());
        assertEquals(2, options.pages().getFirst().groups().getFirst().options().size());
    }

    @Test
    void unwrapPagesCollectsAllDelegates() {
        RsoModOptions options = RsoModOptions.create("celeritas",
                new RsoModMetadata("Celeritas", "2.4.0-dev", null, false),
                List.of(page("general"), page("advanced")));

        List<OptionPage> unwrapped = new java.util.ArrayList<>();
        options.unwrapPages(unwrapped);

        assertEquals(2, unwrapped.size());
        assertTrue(unwrapped.stream().anyMatch(p -> p.getId().getPath().equals("general")));
        assertTrue(unwrapped.stream().anyMatch(p -> p.getId().getPath().equals("advanced")));
    }

    @Test
    void createRejectsBlankMetadataOrEmptyPages() {
        assertThrows(IllegalArgumentException.class, () -> new RsoModMetadata("", "1.0", null, false));
        assertThrows(IllegalArgumentException.class,
                () -> RsoModOptions.create("celeritas", new RsoModMetadata("Celeritas", "1.0", null, false), List.of()));
    }

    private static OptionPage page(String path) {
        Storage storage = new Storage();
        var toggle = OptionImpl.createBuilder(boolean.class, storage)
                .setName(TextComponent.literal(path + " toggle"))
                .setTooltip(TextComponent.literal(path + " toggle tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding((data, value) -> data.enabled = value, data -> data.enabled)
                .build();
        var slider = OptionImpl.createBuilder(int.class, storage)
                .setName(TextComponent.literal(path + " slider"))
                .setTooltip(TextComponent.literal(path + " slider tooltip"))
                .setControl(option -> new org.embeddedt.embeddium.api.options.control.SliderControl(option,
                        0, 10, 1, org.embeddedt.embeddium.api.options.control.ControlValueFormatter.number()))
                .setBinding((data, value) -> data.value = value, data -> data.value)
                .build();
        OptionGroup group = OptionGroup.createBuilder()
                .setId(OptionIdentifier.create("celeritas", path + "_group"))
                .add(toggle)
                .add(slider)
                .build();
        return new OptionPage(OptionIdentifier.create("celeritas", path), TextComponent.literal(path), List.of(group));
    }

    private static final class Storage implements OptionStorage<Storage> {
        private boolean enabled = true;
        private int value = 5;

        @Override
        public Storage getData() {
            return this;
        }
    }
}
