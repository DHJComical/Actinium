package net.caffeinemc.mods.sodium.client.config;

import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTransactionTest {
    @Test
    void appliesPendingValuesAndDeduplicatesStorageAndFlags() {
        AtomicBoolean booleanValue = new AtomicBoolean(false);
        AtomicInteger integerValue = new AtomicInteger(2);
        AtomicInteger saves = new AtomicInteger();
        List<String> events = new ArrayList<>();
        StorageEventHandler storage = () -> {
            saves.incrementAndGet();
            events.add("storage");
        };
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
        manager.registerConfigEntryPoint("test", builder -> {
            var enabled = builder.createBooleanOption(new ResourceLocation("test", "enabled"))
                    .setName(new TextComponentString("Enabled"))
                    .setTooltip(new TextComponentString("Enabled tooltip"))
                    .setStorageHandler(storage)
                    .setDefaultValue(false)
                    .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                    .setBinding(booleanValue::set, booleanValue::get);
            var distance = builder.createIntegerOption(new ResourceLocation("test", "distance"))
                    .setName(new TextComponentString("Distance"))
                    .setTooltip(new TextComponentString("Distance tooltip"))
                    .setStorageHandler(storage)
                    .setDefaultValue(2)
                    .setRange(2, 16, 2)
                    .setValueFormatter(value -> new TextComponentString(Integer.toString(value)))
                    .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                    .setBinding(integerValue::set, integerValue::get);
            var page = builder.createOptionPage().setName(new TextComponentString("General"))
                    .addOption(enabled).addOption(distance);
            builder.registerModOptions("test", "Test", "1.0")
                    .addPage(page)
                    .registerFlagHook((flags, state) -> events.add("flag:" + flags.size()),
                            OptionFlag.REQUIRES_RENDERER_RELOAD.getId());
        });
        Config config = manager.freeze();
        BooleanOption enabled = config.getOption(new ResourceLocation("test", "enabled"), BooleanOption.class);
        IntegerOption distance = config.getOption(new ResourceLocation("test", "distance"), IntegerOption.class);

        enabled.modifyValue(true);
        distance.modifyValue(10);
        assertTrue(config.hasPendingChanges());
        Config.ApplyResult result = config.applyChanges();

        assertTrue(booleanValue.get());
        assertEquals(10, integerValue.get());
        assertEquals(1, saves.get());
        assertEquals(List.of("storage", "flag:1"), events);
        assertEquals(1, result.flags().size());
        assertFalse(config.hasPendingChanges());

        enabled.modifyValue(false);
        config.undoChanges();
        assertTrue(enabled.getPendingValue());
        enabled.resetToDefault();
        assertFalse(enabled.getPendingValue());
        config.discardChanges();
        assertTrue(enabled.getPendingValue());
        assertTrue(booleanValue.get());
    }

    @Test
    void validatesEveryPendingValueBeforeWritingAnyBinding() {
        AtomicBoolean firstValue = new AtomicBoolean(false);
        AtomicInteger firstWrites = new AtomicInteger();
        AtomicInteger secondValue = new AtomicInteger(2);
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
        manager.registerConfigEntryPoint("test", builder -> {
            var first = builder.createBooleanOption(new ResourceLocation("test", "first"))
                    .setName(new TextComponentString("First"))
                    .setTooltip(new TextComponentString("First tooltip"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(false)
                    .setBinding(value -> {
                        firstWrites.incrementAndGet();
                        firstValue.set(value);
                    }, firstValue::get);
            OptionBinding<Integer> rejectingBinding = new OptionBinding<>() {
                @Override
                public Integer validate(Integer value) {
                    throw new IllegalArgumentException("value rejected");
                }

                @Override
                public void save(Integer value) {
                    secondValue.set(value);
                }

                @Override
                public Integer load() {
                    return secondValue.get();
                }
            };
            var second = builder.createIntegerOption(new ResourceLocation("test", "second"))
                    .setName(new TextComponentString("Second"))
                    .setTooltip(new TextComponentString("Second tooltip"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(2)
                    .setRange(0, 10, 1)
                    .setValueFormatter(value -> new TextComponentString(Integer.toString(value)))
                    .setBinding(rejectingBinding);
            builder.registerModOptions("test", "Test", "1.0").addPage(
                    builder.createOptionPage().setName(new TextComponentString("General"))
                            .addOption(first).addOption(second));
        });
        Config config = manager.freeze();
        config.getOption(new ResourceLocation("test", "first"), BooleanOption.class).modifyValue(true);
        config.getOption(new ResourceLocation("test", "second"), IntegerOption.class).modifyValue(4);

        ConfigApplyException exception = assertThrows(ConfigApplyException.class, config::applyChanges);

        assertEquals(new ResourceLocation("test", "second"), exception.optionId());
        assertEquals(0, firstWrites.get());
        assertFalse(firstValue.get());
        assertEquals(2, secondValue.get());
        assertTrue(config.hasPendingChanges());
    }

    @Test
    void rollsBackEveryBindingWhenSecondSaveFails() {
        AtomicBoolean first = new AtomicBoolean(false);
        AtomicBoolean second = new AtomicBoolean(false);
        AtomicInteger secondSaves = new AtomicInteger();
        Config config = twoBooleanConfig(
                new AtomicBinding(first),
                new OptionBinding<>() {
                    @Override
                    public void save(Boolean value) {
                        second.set(value);
                        if (secondSaves.getAndIncrement() == 0) {
                            throw new IllegalStateException("second save failed");
                        }
                    }

                    @Override
                    public Boolean load() {
                        return second.get();
                    }
                },
                () -> { }, null);
        BooleanOption firstOption = config.getOption(new ResourceLocation("test", "first"), BooleanOption.class);
        BooleanOption secondOption = config.getOption(new ResourceLocation("test", "second"), BooleanOption.class);
        firstOption.modifyValue(true);
        secondOption.modifyValue(true);

        ConfigApplyException exception = assertThrows(ConfigApplyException.class, config::applyChanges);

        assertEquals("binding-save", exception.phase());
        assertFalse(first.get());
        assertFalse(second.get());
        assertFalse(firstOption.getAppliedValue());
        assertFalse(secondOption.getAppliedValue());
        assertTrue(firstOption.getPendingValue());
        assertTrue(secondOption.getPendingValue());
    }

    @Test
    void rollsBackBindingsAndRepersistsWhenStorageFails() {
        AtomicBoolean first = new AtomicBoolean(false);
        AtomicBoolean second = new AtomicBoolean(false);
        AtomicInteger storageCalls = new AtomicInteger();
        StorageEventHandler storage = () -> {
            if (storageCalls.getAndIncrement() == 0) {
                throw new IllegalStateException("storage failed");
            }
        };
        Config config = twoBooleanConfig(new AtomicBinding(first), new AtomicBinding(second), storage, null);
        config.getOption(new ResourceLocation("test", "first"), BooleanOption.class).modifyValue(true);
        config.getOption(new ResourceLocation("test", "second"), BooleanOption.class).modifyValue(true);

        ConfigApplyException exception = assertThrows(ConfigApplyException.class, config::applyChanges);

        assertEquals("storage", exception.phase());
        assertFalse(first.get());
        assertFalse(second.get());
        assertEquals(2, storageCalls.get());
        assertTrue(config.hasPendingChanges());
    }

    @Test
    void rollsBackBindingsWhenApplyHookFails() {
        AtomicBoolean first = new AtomicBoolean(false);
        AtomicBoolean second = new AtomicBoolean(false);
        AtomicInteger storageCalls = new AtomicInteger();
        Config config = twoBooleanConfig(new AtomicBinding(first), new AtomicBinding(second),
                storageCalls::incrementAndGet,
                state -> { throw new IllegalStateException("hook failed"); });
        config.getOption(new ResourceLocation("test", "first"), BooleanOption.class).modifyValue(true);
        config.getOption(new ResourceLocation("test", "second"), BooleanOption.class).modifyValue(true);

        ConfigApplyException exception = assertThrows(ConfigApplyException.class, config::applyChanges);

        assertEquals("apply-hook", exception.phase());
        assertFalse(first.get());
        assertFalse(second.get());
        assertEquals(2, storageCalls.get());
        assertTrue(config.hasPendingChanges());
    }

    @Test
    void aggregatesRollbackFailuresWithoutHidingTheApplyFailure() {
        AtomicBoolean first = new AtomicBoolean(false);
        AtomicBoolean second = new AtomicBoolean(false);
        OptionBinding<Boolean> rollbackFailing = new OptionBinding<>() {
            @Override
            public void save(Boolean value) {
                if (!value) {
                    throw new IllegalStateException("rollback failed");
                }
                first.set(true);
            }

            @Override
            public Boolean load() {
                return first.get();
            }
        };
        OptionBinding<Boolean> applyFailing = new OptionBinding<>() {
            @Override
            public void save(Boolean value) {
                throw new IllegalStateException("apply failed");
            }

            @Override
            public Boolean load() {
                return second.get();
            }
        };
        Config config = twoBooleanConfig(rollbackFailing, applyFailing, () -> { }, null);
        config.getOption(new ResourceLocation("test", "first"), BooleanOption.class).modifyValue(true);
        config.getOption(new ResourceLocation("test", "second"), BooleanOption.class).modifyValue(true);

        ConfigApplyException exception = assertThrows(ConfigApplyException.class, config::applyChanges);

        assertEquals("apply failed", exception.getCause().getMessage());
        assertEquals(2, exception.getSuppressed().length);
        assertTrue(Arrays.stream(exception.getSuppressed())
                .anyMatch(failure -> failure.getMessage().equals("rollback failed")));
        assertTrue(Arrays.stream(exception.getSuppressed())
                .anyMatch(failure -> failure.getMessage().equals("apply failed")));
    }

    private static Config twoBooleanConfig(OptionBinding<Boolean> firstBinding,
                                           OptionBinding<Boolean> secondBinding,
                                           StorageEventHandler storage,
                                           Consumer<ConfigState> hook) {
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
        manager.registerCoreConfigEntryPoint("test", builder -> {
            var first = builder.createBooleanOption(new ResourceLocation("test", "first"))
                    .setName(new TextComponentString("First"))
                    .setTooltip(new TextComponentString("First tooltip"))
                    .setStorageHandler(storage)
                    .setDefaultValue(false)
                    .setBinding(firstBinding);
            var second = builder.createBooleanOption(new ResourceLocation("test", "second"))
                    .setName(new TextComponentString("Second"))
                    .setTooltip(new TextComponentString("Second tooltip"))
                    .setStorageHandler(storage)
                    .setDefaultValue(false)
                    .setBinding(secondBinding);
            if (hook != null) {
                second.setApplyHook(hook);
            }
            builder.registerModOptions("test", "Test", "1.0").addPage(
                    builder.createOptionPage().setName(new TextComponentString("General"))
                            .addOption(first).addOption(second));
        });
        return manager.freeze();
    }

    private record AtomicBinding(AtomicBoolean value) implements OptionBinding<Boolean> {
        @Override
        public void save(Boolean newValue) {
            this.value.set(newValue);
        }

        @Override
        public Boolean load() {
            return this.value.get();
        }
    }
}
