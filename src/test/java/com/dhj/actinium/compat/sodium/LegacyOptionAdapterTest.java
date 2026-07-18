package com.dhj.actinium.compat.sodium;

import net.caffeinemc.mods.sodium.client.config.builder.ConfigBuilderImpl;
import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.EnumOption;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionImpl;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
class LegacyOptionAdapterTest {
    @Test
    void convertsControlsAndPersistsSharedLegacyStorageOnce() {
        MutableStorage storage = new MutableStorage();
        OptionPage page = createPage(storage);
        ConfigBuilderImpl builder = new ConfigBuilderImpl(id -> null, "testmod");
        var owner = builder.registerModOptions("testmod", "Test Mod", "1");

        new LegacyOptionAdapter(builder, true, new LinkedHashSet<>()).addPages(owner, List.of(page));
        Config config = new Config(builder.build(), () -> "en_us");

        config.getOption(id("enabled"), BooleanOption.class).modifyValue(false);
        config.getOption(id("distance"), IntegerOption.class).modifyValue(6);
        config.getOption(id("mode"), EnumOption.class).modifyValue(Mode.SECOND);
        Config.ApplyResult result = config.applyChanges();

        assertFalse(storage.data.enabled);
        assertEquals(6, storage.data.distance);
        assertEquals(Mode.SECOND, storage.data.mode);
        assertEquals(1, storage.saves);
        assertEquals(Set.of(
                new ResourceLocation("sodium", "builtin_option_flag.requires_renderer_reload"),
                new ResourceLocation("sodium", "builtin_option_flag.requires_renderer_update")),
                result.flags());
    }

    @Test
    void disablesEveryConvertedControlWhenConfigurationIsReadOnly() {
        MutableStorage storage = new MutableStorage();
        ConfigBuilderImpl builder = new ConfigBuilderImpl(id -> null, "testmod");
        var owner = builder.registerModOptions("testmod", "Test Mod", "1");

        new LegacyOptionAdapter(builder, false, new LinkedHashSet<>()).addPages(owner, List.of(createPage(storage)));
        List<ModOptions> models = builder.build();
        Config config = new Config(models, () -> "en_us");

        assertTrue(config.optionIds().stream().noneMatch(optionId ->
                config.getOption(optionId, Option.class).isEnabled()));
        assertFalse(config.hasPendingChanges());
    }

    @Test
    void resolvesLegacyTextThroughClientTranslator() {
        MutableStorage storage = new MutableStorage();
        var option = OptionImpl.createBuilder(boolean.class, storage)
                .setId(OptionIdentifier.create("testmod", "translated", boolean.class))
                .setName(TextComponent.translatable("test.option.name"))
                .setTooltip(TextComponent.translatable("test.option.tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding((data, value) -> data.enabled = value, data -> data.enabled)
                .build();
        OptionPage page = new OptionPage(OptionIdentifier.create("testmod", "translated_page"),
                TextComponent.translatable("test.page.name"),
                List.of(OptionGroup.createBuilder().add(option).build()));
        ConfigBuilderImpl builder = new ConfigBuilderImpl(id -> null, "testmod");
        var owner = builder.registerModOptions("testmod", "Test Mod", "1");

        new LegacyOptionAdapter(builder, true, new LinkedHashSet<>(), key -> true,
                (key, arguments) -> "translated:" + key).addPages(owner, List.of(page));
        Config config = new Config(builder.build(), () -> "en_us");
        Option translated = config.getOption(id("translated"), Option.class);

        assertEquals("translated:test.page.name", config.getModOptions().getFirst().pages().getFirst()
                .name().getUnformattedText());
        assertEquals("translated:test.option.name", translated.getName().getUnformattedText());
        assertEquals("translated:test.option.tooltip", translated.getTooltip().getUnformattedText());
    }

    private static OptionPage createPage(MutableStorage storage) {
        var enabled = OptionImpl.createBuilder(boolean.class, storage)
                .setId(OptionIdentifier.create("testmod", "enabled", boolean.class))
                .setName(TextComponent.literal("Enabled"))
                .setTooltip(TextComponent.literal("Enabled tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding((data, value) -> data.enabled = value, data -> data.enabled)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build();
        var distance = OptionImpl.createBuilder(int.class, storage)
                .setId(OptionIdentifier.create("testmod", "distance", int.class))
                .setName(TextComponent.literal("Distance"))
                .setTooltip(TextComponent.literal("Distance tooltip"))
                .setControl(option -> new SliderControl(option, 2, 8, 2, ControlValueFormatter.number()))
                .setBinding((data, value) -> data.distance = value, data -> data.distance)
                .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                .build();
        var mode = OptionImpl.createBuilder(Mode.class, storage)
                .setId(OptionIdentifier.create("testmod", "mode", Mode.class))
                .setName(TextComponent.literal("Mode"))
                .setTooltip(TextComponent.literal("Mode tooltip"))
                .setControl(option -> new CyclingControl<>(option, Mode.class,
                        new TextComponent[] { TextComponent.literal("First"), TextComponent.literal("Second") }))
                .setBinding((data, value) -> data.mode = value, data -> data.mode)
                .build();
        OptionGroup group = OptionGroup.createBuilder().add(enabled).add(distance).add(mode).build();
        return new OptionPage(OptionIdentifier.create("testmod", "page"), TextComponent.literal("Page"),
                List.of(group));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("testmod", path);
    }

    private enum Mode {
        FIRST,
        SECOND
    }

    private static final class Data {
        private boolean enabled = true;
        private int distance = 2;
        private Mode mode = Mode.FIRST;
    }

    private static final class MutableStorage implements OptionStorage<Data> {
        private final Data data = new Data();
        private int saves;

        @Override
        public Data getData() {
            return this.data;
        }

        @Override
        public void save(Set<OptionFlag> flags) {
            this.saves++;
        }
    }
}
