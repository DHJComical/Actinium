package net.caffeinemc.mods.sodium.client.config;

import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {
    @Test
    void failsFastWhenCoreEntryPointFails() {
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
        manager.registerCoreConfigEntryPoint("actinium-core", builder -> {
            throw new IllegalStateException("core failed");
        });

        IllegalStateException exception = assertThrows(IllegalStateException.class, manager::freeze);

        assertEquals("core failed", exception.getMessage());
        assertTrue(manager.failures().isEmpty());
    }

    @Test
    void isolatesFailingEntriesAndFreezesExplicitRegistration() {
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
        manager.registerConfigEntryPoint("broken", builder -> {
            throw new IllegalStateException("broken entry");
        });
        manager.registerConfigEntryPoint("working", builder -> registerBooleanPage(builder, "working"));

        Config config = manager.freeze();

        assertEquals(1, manager.failures().size());
        assertEquals("broken", manager.failures().getFirst().source());
        assertTrue(config.readBooleanOption(new ResourceLocation("working", "enabled")));
        assertThrows(IllegalStateException.class,
                () -> manager.registerConfigEntryPoint("late", builder -> registerBooleanPage(builder, "late")));
    }

    @Test
    void rejectsDuplicateIdsAndIncompleteDependencies() {
        ConfigManager duplicateManager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
        duplicateManager.registerConfigEntryPoint("first", builder -> registerBooleanPage(builder, "same"));
        duplicateManager.registerConfigEntryPoint("second", builder -> registerBooleanPage(builder, "same"));
        Config duplicateConfig = duplicateManager.freeze();

        assertTrue(duplicateConfig.readBooleanOption(new ResourceLocation("same", "enabled")));
        assertEquals(1, duplicateManager.failures().size());
        assertTrue(duplicateManager.failures().getFirst().cause().getMessage().contains("same"));

        ConfigManager dependencyManager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
        dependencyManager.registerConfigEntryPoint("dependent", builder -> {
            AtomicBoolean value = new AtomicBoolean(true);
            var option = builder.createBooleanOption(new ResourceLocation("dependent", "child"))
                    .setName(new TextComponentString("Child"))
                    .setTooltip(new TextComponentString("Child option"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(true)
                    .setBinding(value::set, value::get)
                    .setEnabledProvider(state -> state.readBooleanOption(new ResourceLocation("missing", "parent")),
                            new ResourceLocation("missing", "parent"));
            var page = builder.createOptionPage().setName(new TextComponentString("Page")).addOption(option);
            builder.registerModOptions("dependent", "Dependent", "1.0").addPage(page);
        });

        Config config = dependencyManager.freeze();

        assertFalse(config.optionIds().contains(new ResourceLocation("dependent", "child")));
        assertEquals(1, dependencyManager.failures().size());
        assertTrue(dependencyManager.failures().getFirst().cause().getMessage().contains("missing:parent"));
    }

    private static void registerBooleanPage(ConfigBuilder builder, String namespace) {
        AtomicBoolean value = new AtomicBoolean(true);
        var option = builder.createBooleanOption(new ResourceLocation(namespace, "enabled"))
                .setName(new TextComponentString("Enabled"))
                .setTooltip(new TextComponentString("Enables the feature"))
                .setStorageHandler(() -> { })
                .setDefaultValue(false)
                .setBinding(value::set, value::get);
        var page = builder.createOptionPage().setName(new TextComponentString("General")).addOption(option);
        builder.registerModOptions(namespace, namespace, "1.0").addPage(page);
    }
}
