package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.junit.jupiter.api.Test;
import org.taumc.celeritas.api.OptionGroupConstructionEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CeleritasLegacyControlBridgeTest {
    @Test
    void exposesCurrentConcreteControlsAsEquivalentLegacyControls() {
        var currentTickOption = CurrentOptionFixtures.tickBox();
        var tickOption = LegacyOptionMapper.createOptionView(CurrentOptionMapper.describeOption(currentTickOption));
        TickBoxControl tickBox = assertInstanceOf(TickBoxControl.class, tickOption.getControl());
        assertSame(tickOption, tickBox.getOption());
        assertTrue(tickBox.getOption().getValue());
        assertSame(currentTickOption.getStorage().getData(), tickOption.getStorage().getData());

        var sliderOption = LegacyOptionMapper.createOptionView(
                CurrentOptionMapper.describeOption(CurrentOptionFixtures.slider()));
        SliderControl slider = assertInstanceOf(SliderControl.class, sliderOption.getControl());
        assertSame(sliderOption, slider.getOption());
        assertEquals(4, slider.getOption().getValue());
        assertEquals(0, slider.getMin());
        assertEquals(10, slider.getMax());
        assertEquals(2, slider.getInterval());
        assertEquals(TextComponent.literal("value:4"), slider.getFormatter().format(4));

        var cyclingOption = LegacyOptionMapper.createOptionView(
                CurrentOptionMapper.describeOption(CurrentOptionFixtures.cycling()));
        CyclingControl<?> cycling = assertInstanceOf(CyclingControl.class, cyclingOption.getControl());
        assertSame(cyclingOption, cycling.getOption());
        assertEquals(CurrentOptionFixtures.Mode.FIRST, cycling.getOption().getValue());
        assertArrayEquals(CurrentOptionFixtures.Mode.values(), cycling.getAllowedValues());
        assertArrayEquals(new TextComponent[] {
                TextComponent.literal("First mode"),
                TextComponent.literal("Second mode")
        }, cycling.getNames());
    }

    @Test
    void rejectsUnknownCurrentControls() {
        var current = CurrentOptionFixtures.unknownControl();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CurrentOptionMapper.describeOption(current));
        assertEquals("Unsupported current option control: "
                + current.getControl().getClass().getName(), exception.getMessage());
    }

    @Test
    void installingTwiceBridgesEachCurrentGroupEventOnce() {
        AtomicInteger calls = new AtomicInteger();
        EventHandlerRegistrar.Handler<OptionGroupConstructionEvent> handler = event -> {
            calls.incrementAndGet();
            assertEquals("compat_group", event.getId().getPath());
            assertInstanceOf(SliderControl.class, event.getOptions().getFirst().getControl());
        };
        OptionGroupConstructionEvent.BUS.addListener(handler);
        try {
            CeleritasLegacyEventBridge.install();
            CeleritasLegacyEventBridge.install();
            OptionGroup.createBuilder()
                    .setId(OptionIdentifier.create("compat_test", "compat_group"))
                    .add(CurrentOptionFixtures.slider())
                    .build();
            assertEquals(1, calls.get());
        } finally {
            OptionGroupConstructionEvent.BUS.removeListener(handler);
        }
    }
}
