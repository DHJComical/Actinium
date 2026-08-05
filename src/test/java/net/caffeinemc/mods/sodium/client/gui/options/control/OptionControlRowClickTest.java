package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.EnumOption;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionControlRowClickTest {
    private enum Mode {
        LOW,
        HIGH
    }

    private static final ResourceLocation BOOLEAN_ID = new ResourceLocation("row_click", "enabled");
    private static final ResourceLocation ENUM_ID = new ResourceLocation("row_click", "mode");
    private static final ResourceLocation INTEGER_ID = new ResourceLocation("row_click", "distance");

    @Test
    void booleanControlTogglesWhenClickingTheRowLabel() {
        Config config = config();
        BooleanOption option = config.getOption(BOOLEAN_ID, BooleanOption.class);
        BooleanControl control = new BooleanControl(option, ColorTheme.defaultFor("row_click"), null);
        control.setBounds(rowBounds());

        assertFalse(option.getPendingValue());
        assertTrue(control.mouseClicked(20, 19, 0));
        assertTrue(option.getPendingValue());
        assertTrue(control.mouseClicked(180, 19, 0));
        assertFalse(option.getPendingValue());
    }

    @Test
    void enumControlCyclesWhenClickingTheRowLabel() {
        Config config = config();
        EnumOption<Mode> option = config.getOption(ENUM_ID, EnumOption.class);
        EnumControl<Mode> control = new EnumControl<>(option, ColorTheme.defaultFor("row_click"), null, config);
        control.setBounds(rowBounds());

        assertEquals(Mode.LOW, option.getPendingValue());
        assertTrue(control.mouseClicked(20, 19, 0));
        assertEquals(Mode.HIGH, option.getPendingValue());
        assertTrue(control.mouseClicked(180, 19, 0));
        assertEquals(Mode.LOW, option.getPendingValue());
    }

    @Test
    void integerControlKeepsSliderOnlyClickHandling() {
        Config config = config();
        IntegerOption option = config.getOption(INTEGER_ID, IntegerOption.class);
        IntegerControl control = new IntegerControl(option, ColorTheme.defaultFor("row_click"), null);
        control.setBounds(rowBounds());

        assertEquals(2, option.getPendingValue());
        assertFalse(control.mouseClicked(20, 19, 0));
        assertEquals(2, option.getPendingValue());
        assertTrue(control.mouseClicked(110, 19, 0));
        assertTrue(option.getPendingValue() >= 2 && option.getPendingValue() <= 16);
    }

    private static Config config() {
        AtomicBoolean booleanValue = new AtomicBoolean(false);
        AtomicReference<Mode> enumValue = new AtomicReference<>(Mode.LOW);
        AtomicInteger integerValue = new AtomicInteger(2);
        ConfigManager manager = new ConfigManager(id -> new ConfigManager.ModMetadata(id, "1.0"), () -> "en_us");
        manager.registerCoreConfigEntryPoint("row_click", builder -> {
            var booleanOption = builder.createBooleanOption(BOOLEAN_ID)
                    .setName(new TextComponentString("Enabled"))
                    .setTooltip(new TextComponentString("Enables the feature"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(false)
                    .setBinding(booleanValue::set, booleanValue::get);
            var enumOption = builder.createEnumOption(ENUM_ID, Mode.class)
                    .setName(new TextComponentString("Mode"))
                    .setTooltip(new TextComponentString("Selects the mode"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(Mode.LOW)
                    .setAllowedValues(Set.of(Mode.LOW, Mode.HIGH))
                    .setElementNameProvider(value -> new TextComponentString(value.name()))
                    .setBinding(enumValue::set, enumValue::get);
            var integerOption = builder.createIntegerOption(INTEGER_ID)
                    .setName(new TextComponentString("Distance"))
                    .setTooltip(new TextComponentString("Selects the distance"))
                    .setStorageHandler(() -> { })
                    .setDefaultValue(2)
                    .setRange(2, 16, 2)
                    .setValueFormatter(value -> new TextComponentString(Integer.toString(value)))
                    .setBinding(integerValue::set, integerValue::get);
            builder.registerModOptions("row_click", "Row Click", "1.0").addPage(
                    builder.createOptionPage().setName(new TextComponentString("General"))
                            .addOption(booleanOption)
                            .addOption(enumOption)
                            .addOption(integerOption));
        });
        return manager.freeze();
    }

    private static GuiRect rowBounds() {
        return new GuiRect(10, 10, 200, 18);
    }
}
