package net.caffeinemc.mods.sodium.client.config;

import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.EnumOption;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalButtonOption;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigBuilderContractTest {
    private enum Quality {
        LOW,
        HIGH
    }

    @Test
    void requiresIntegerFormatterAndEnumElementNamesAtBuildTime() {
        ConfigManager integerManager = manager();
        integerManager.registerCoreConfigEntryPoint("integer", builder -> {
            var option = builder.createIntegerOption(new ResourceLocation("integer", "value"))
                    .setName(new TextComponentString("Value"))
                    .setTooltip(new TextComponentString("Value tooltip"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(1)
                    .setRange(0, 2, 1)
                    .setBinding(value -> { }, () -> 1);
            builder.registerModOptions("integer", "Integer", "1.0").addPage(
                    builder.createOptionPage().setName(new TextComponentString("Page")).addOption(option));
        });
        assertTrue(assertThrows(IllegalStateException.class, integerManager::freeze)
                .getMessage().contains("formatter"));

        ConfigManager enumManager = manager();
        enumManager.registerCoreConfigEntryPoint("enum", builder -> {
            var option = builder.createEnumOption(new ResourceLocation("enum", "quality"), Quality.class)
                    .setName(new TextComponentString("Quality"))
                    .setTooltip(new TextComponentString("Quality tooltip"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(Quality.LOW)
                    .setBinding(value -> { }, () -> Quality.LOW);
            builder.registerModOptions("enum", "Enum", "1.0").addPage(
                    builder.createOptionPage().setName(new TextComponentString("Page")).addOption(option));
        });
        assertTrue(assertThrows(IllegalStateException.class, enumManager::freeze)
                .getMessage().contains("element name"));
    }

    @Test
    void invalidLoadedValuesUseDefaultsAndPersistOnApply() {
        AtomicInteger integerBacking = new AtomicInteger(99);
        AtomicReference<Quality> enumBacking = new AtomicReference<>(Quality.HIGH);
        AtomicInteger storageWrites = new AtomicInteger();
        ConfigManager manager = manager();
        manager.registerCoreConfigEntryPoint("test", builder -> {
            var integer = builder.createIntegerOption(new ResourceLocation("test", "integer"))
                    .setName(new TextComponentString("Integer"))
                    .setTooltip(new TextComponentString("Integer tooltip"))
                    .setStorageHandler(storageWrites::incrementAndGet)
                    .setDefaultValue(4)
                    .setRange(0, 8, 2)
                    .setValueFormatter(value -> new TextComponentString(Integer.toString(value)))
                    .setBinding(integerBacking::set, integerBacking::get);
            var enumOption = builder.createEnumOption(new ResourceLocation("test", "enum"), Quality.class)
                    .setName(new TextComponentString("Enum"))
                    .setTooltip(new TextComponentString("Enum tooltip"))
                    .setStorageHandler(storageWrites::incrementAndGet)
                    .setDefaultValue(Quality.LOW)
                    .setAllowedValues(Set.of(Quality.LOW))
                    .setElementNameProvider(value -> new TextComponentString(value.name()))
                    .setBinding(enumBacking::set, enumBacking::get);
            builder.registerModOptions("test", "Test", "1.0").addPage(
                    builder.createOptionPage().setName(new TextComponentString("Page"))
                            .addOption(integer).addOption(enumOption));
        });
        Config config = manager.freeze();

        assertEquals(4, config.getOption(new ResourceLocation("test", "integer"), IntegerOption.class)
                .getPendingValue());
        assertEquals(Quality.LOW, config.getOption(new ResourceLocation("test", "enum"), EnumOption.class)
                .getPendingValue());
        assertTrue(config.hasPendingChanges());
        config.applyChanges();
        assertEquals(4, integerBacking.get());
        assertEquals(Quality.LOW, enumBacking.get());
        assertEquals(2, storageWrites.get());
    }

    @Test
    void appliesReplacementOverlayAndBuildsExternalButtonOption() {
        AtomicReference<Boolean> backing = new AtomicReference<>(false);
        ResourceLocation target = new ResourceLocation("base", "enabled");
        ConfigManager manager = manager();
        manager.registerCoreConfigEntryPoint("base", builder -> {
            var base = builder.createBooleanOption(target)
                    .setName(new TextComponentString("Base"))
                    .setTooltip(new TextComponentString("Base tooltip"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(false)
                    .setBinding(backing::set, backing::get);
            var external = builder.createExternalButtonOption(new ResourceLocation("base", "external"))
                    .setName(new TextComponentString("External"))
                    .setTooltip(new TextComponentString("Open external screen"))
                    .setScreenConsumer(screen -> { });
            builder.registerModOptions("base", "Base", "1.0").addPage(
                    builder.createOptionPage().setName(new TextComponentString("Page"))
                            .addOption(base).addOption(external));
        });
        manager.registerConfigEntryPoint("extension", builder -> {
            var replacement = builder.createBooleanOption(target)
                    .setName(new TextComponentString("Replacement"))
                    .setTooltip(new TextComponentString("Replacement tooltip"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(false)
                    .setBinding(backing::set, backing::get);
            var overlay = builder.createBooleanOption(target)
                    .setTooltip(new TextComponentString("Overlay tooltip"));
            builder.registerModOptions("extension", "Extension", "1.0")
                    .registerOptionReplacement(target, replacement)
                    .registerOptionOverlay(target, overlay);
        });

        Config config = manager.freeze();

        BooleanOption option = config.getOption(target, BooleanOption.class);
        assertEquals("Replacement", option.getName().getUnformattedText());
        assertEquals("Overlay tooltip", option.getTooltip().getUnformattedText());
        assertEquals(ExternalButtonOption.class,
                config.getOption(new ResourceLocation("base", "external"), ExternalButtonOption.class).getClass());
    }

    private static ConfigManager manager() {
        return new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
    }
}
