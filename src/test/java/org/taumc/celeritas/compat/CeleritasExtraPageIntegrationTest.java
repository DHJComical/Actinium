package org.taumc.celeritas.compat;

import net.caffeinemc.mods.sodium.client.config.builder.ConfigBuilderImpl;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.minecraft.util.ResourceLocation;
import com.dhj.actinium.compat.sodium.LegacyOptionAdapter;
import com.dhj.actinium.compat.sodium.OptionGUIConstructionBridge;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.junit.jupiter.api.Test;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.api.options.structure.OptionStorage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CeleritasExtraPageIntegrationTest {
    @Test
    void convertsFivePagesWhoseOptionsUseLegacyEmptyIdentifiers() {
        EventHandlerRegistrar.Handler<OptionGUIConstructionEvent> listener = event -> createPages().forEach(event::addPage);
        OptionGUIConstructionEvent.BUS.addListener(listener);
        try {
            CeleritasLegacyEventBridge.install();
            var currentPages = OptionGUIConstructionBridge.collectExtensions(List.of()).get("celeritasextra");

            assertEquals(5, currentPages.size());
            Set<ResourceLocation> firstIds = new LinkedHashSet<>();
            Set<ResourceLocation> secondIds = new LinkedHashSet<>();
            for (Set<ResourceLocation> ids : List.of(firstIds, secondIds)) {
                ConfigBuilderImpl builder = new ConfigBuilderImpl(id -> null, "celeritasextra");
                var owner = builder.registerModOptions("celeritasextra", "Celeritas Extra", "legacy-event");
                new LegacyOptionAdapter(builder, true, new LinkedHashSet<>()).addPages(owner, currentPages);
                Config config = new Config(builder.build(), () -> "en_us");
                assertEquals(5, config.getModOptions().getFirst().pages().size());
                ids.addAll(config.optionIds());
            }

            assertEquals(firstIds, secondIds);
            assertEquals(10, firstIds.size());
            assertEquals(new LinkedHashSet<>(List.of(
                    id("animation", 0), id("animation", 1),
                    id("particle", 0), id("particle", 1),
                    id("details", 0), id("details", 1),
                    id("render", 0), id("render", 1),
                    id("extra", 0), id("extra", 1))), firstIds);
        } finally {
            OptionGUIConstructionEvent.BUS.removeListener(listener);
        }
    }

    private static List<OptionPage> createPages() {
        return List.of(page("animation"), page("particle"), page("details"), page("render"), page("extra"));
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
                .setControl(option -> new SliderControl(option, 0, 10, 1, ControlValueFormatter.number()))
                .setBinding((data, value) -> data.value = value, data -> data.value)
                .build();
        assertSame(OptionIdentifier.EMPTY, toggle.getId());
        assertSame(OptionIdentifier.EMPTY, slider.getId());
        OptionGroup group = OptionGroup.createBuilder().add(toggle).add(slider).build();
        return new OptionPage(
                OptionIdentifier.create("celeritasextra", path), TextComponent.literal(path), List.of(group));
    }

    private static ResourceLocation id(String page, int option) {
        return new ResourceLocation("celeritasextra",
                "_legacy_unnamed/" + page + "/group_0/option_" + option);
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
