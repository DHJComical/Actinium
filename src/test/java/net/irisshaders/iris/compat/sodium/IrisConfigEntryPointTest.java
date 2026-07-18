package net.irisshaders.iris.compat.sodium;

import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IrisConfigEntryPointTest {
    @Test
    void registersShadowOptionAndExternalPageAndAppliesInjectedBinding() {
        AtomicInteger persisted = new AtomicInteger(32);
        AtomicBoolean openedCalled = new AtomicBoolean();
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "test"), () -> "en_us");
        manager.registerConfigEntryPoint("iris", new IrisConfigEntryPoint(persisted::set, persisted::get,
                ignored -> openedCalled.set(true)));

        Config config = manager.freeze();
        assertTrue(config.optionIds().contains(new ResourceLocation("iris", "shadow_distance")));
        ExternalPage external = config.getModOptions().stream().flatMap(options -> options.pages().stream())
                .filter(ExternalPage.class::isInstance).map(ExternalPage.class::cast).findFirst().orElseThrow();
        assertEquals(1, config.getModOptions().stream().filter(options -> options.pages().stream()
                .anyMatch(page -> page instanceof ExternalPage)).count());
        external.screenConsumer().accept(null);
        assertTrue(openedCalled.get());

        IntegerOption shadow = config.getOption(new ResourceLocation("iris", "shadow_distance"), IntegerOption.class);
        shadow.modifyValue(64);
        config.applyChanges();
        assertEquals(64, persisted.get());
    }

    @Test
    void freezeIsIdempotentAndRejectsLateRegistration() {
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "test"), () -> "en_us");
        manager.registerConfigEntryPoint("iris", new IrisConfigEntryPoint(value -> { }, () -> 32, ignored -> { }));
        Config first = manager.freeze();
        assertSame(first, manager.freeze());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> manager.registerConfigEntryPoint("late", new IrisConfigEntryPoint()));
    }
}
