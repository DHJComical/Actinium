package net.caffeinemc.mods.sodium.client.config;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.client.config.search.OptionTextSource;
import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSearchAndDependencyTest {
    @Test
    void updateOnApplyReadsTheParentAppliedValue() {
        AtomicBoolean backing = new AtomicBoolean(false);
        ResourceLocation optionId = new ResourceLocation("test", "parent-aware");
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
        manager.registerCoreConfigEntryPoint("test", builder -> {
            var option = builder.createBooleanOption(optionId)
                    .setName(new TextComponentString("Parent aware"))
                    .setTooltip(new TextComponentString("Reads its applied state"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(false)
                    .setBinding(backing::set, backing::get)
                    .setEnabledProvider(state -> state.readBooleanOption(optionId), ConfigState.UPDATE_ON_APPLY);
            builder.registerModOptions("test", "Test", "1.0").addPage(
                    builder.createOptionPage().setName(new TextComponentString("General")).addOption(option));
        });
        Config config = manager.freeze();
        BooleanOption option = config.getOption(optionId, BooleanOption.class);

        assertFalse(option.isEnabled());
        option.modifyValue(true);
        assertFalse(option.isEnabled());
        config.applyChanges();
        assertTrue(option.isEnabled());
    }

    @Test
    void searchesPageOptionNameAndDescriptionAndEvaluatesPendingDependencies() {
        AtomicBoolean parentValue = new AtomicBoolean(false);
        AtomicBoolean childValue = new AtomicBoolean(true);
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
        manager.registerConfigEntryPoint("test", builder -> {
            ResourceLocation parentId = new ResourceLocation("test", "parent");
            var parent = builder.createBooleanOption(parentId)
                    .setName(new TextComponentString("Master Switch"))
                    .setTooltip(new TextComponentString("Controls all lighting features"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(false)
                    .setBinding(parentValue::set, parentValue::get);
            var child = builder.createBooleanOption(new ResourceLocation("test", "child"))
                    .setName(new TextComponentString("Quality"))
                    .setTooltip(new TextComponentString("Changes shadow fidelity"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(true)
                    .setBinding(childValue::set, childValue::get)
                    .setEnabledProvider(state -> state.readBooleanOption(parentId), parentId);
            builder.registerModOptions("test", "Test", "1.0").addPage(
                    builder.createOptionPage().setName(new TextComponentString("Advanced Lighting"))
                            .addOption(parent).addOption(child));
        });
        Config config = manager.freeze();
        BooleanOption parent = config.getOption(new ResourceLocation("test", "parent"), BooleanOption.class);
        BooleanOption child = config.getOption(new ResourceLocation("test", "child"), BooleanOption.class);

        assertFalse(child.isEnabled());
        parent.modifyValue(true);
        assertTrue(child.isEnabled());
        assertEquals(OptionTextSource.Kind.PAGE,
                ((OptionTextSource) config.startSearchQuery().getSearchResults("advanced").getFirst()).getKind());
        assertEquals(OptionTextSource.Kind.NAME,
                ((OptionTextSource) config.startSearchQuery().getSearchResults("quality").getFirst()).getKind());
        assertEquals(OptionTextSource.Kind.DESCRIPTION,
                ((OptionTextSource) config.startSearchQuery().getSearchResults("fidelity").getFirst()).getKind());
    }
}
