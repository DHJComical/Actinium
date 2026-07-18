package net.irisshaders.iris.compat.sodium;

import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.coderbot.iris.gui.option.IrisVideoSettings;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IrisConfigEntryPointTest {
    @Test
    void registersShadowOptionAndExternalPageAndAppliesInjectedBinding() {
        AtomicInteger persisted = new AtomicInteger(32);
        AtomicBoolean openedCalled = new AtomicBoolean();
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "test"), () -> "en_us");
        manager.registerConfigEntryPoint("iris", new IrisConfigEntryPoint(persisted::set, persisted::get,
                ignored -> openedCalled.set(true), (key, arguments) -> "translated:" + key));

        Config config = manager.freeze();
        assertTrue(config.optionIds().contains(new ResourceLocation("iris", "shadow_distance")));
        ModOptions irisOptions = config.getModOptions().stream()
                .filter(options -> "iris".equals(options.configId())).findFirst().orElseThrow();
        ExternalPage external = irisOptions.pages().stream()
                .filter(ExternalPage.class::isInstance).map(ExternalPage.class::cast).findFirst().orElseThrow();
        OptionPage optionPage = irisOptions.pages().stream()
                .filter(OptionPage.class::isInstance).map(OptionPage.class::cast).findFirst().orElseThrow();
        assertEquals(1, irisOptions.pages().stream().filter(ExternalPage.class::isInstance).count());
        assertEquals("translated:options.iris.title", optionPage.name().getUnformattedText());
        assertEquals("translated:options.iris.shaderPackSelection", external.name().getUnformattedText());
        external.screenConsumer().accept(null);
        assertTrue(openedCalled.get());

        IntegerOption shadow = config.getOption(new ResourceLocation("iris", "shadow_distance"), IntegerOption.class);
        assertEquals("translated:options.iris.shadowDistance", shadow.getName().getUnformattedText());
        String expectedTooltipKey = IrisVideoSettings.isShadowDistanceSliderEnabled()
                ? "options.iris.shadowDistance.enabled"
                : "options.iris.shadowDistance.disabled";
        assertEquals("translated:" + expectedTooltipKey, shadow.getTooltip().getUnformattedText());
        shadow.modifyValue(64);
        config.applyChanges();
        assertEquals(64, persisted.get());
    }

    @Test
    void freezeIsIdempotentAndRejectsLateRegistration() {
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "test"), () -> "en_us");
        manager.registerConfigEntryPoint("iris", new IrisConfigEntryPoint(value -> { }, () -> 32, ignored -> { },
                (key, arguments) -> "translated:" + key));
        Config first = manager.freeze();
        assertSame(first, manager.freeze());
        assertThrows(IllegalStateException.class,
                () -> manager.registerConfigEntryPoint("late", new IrisConfigEntryPoint()));
    }
}
