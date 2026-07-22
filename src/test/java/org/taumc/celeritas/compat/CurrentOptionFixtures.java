package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.control.Control;
import org.embeddedt.embeddium.api.options.control.ControlElement;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionImpl;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.util.Dim2i;

final class CurrentOptionFixtures {
    enum Mode {
        FIRST,
        SECOND
    }

    private CurrentOptionFixtures() {
    }

    static Option<Boolean> tickBox() {
        Box<Boolean> box = new Box<>(true);
        return builder("tick_box", Boolean.class, box)
                .setControl(TickBoxControl::new)
                .build();
    }

    static Option<Integer> slider() {
        Box<Integer> box = new Box<>(4);
        return builder("slider", Integer.class, box)
                .setControl(option -> new SliderControl(option, 0, 10, 2,
                        value -> TextComponent.literal("value:" + value)))
                .build();
    }

    static Option<Mode> cycling() {
        Box<Mode> box = new Box<>(Mode.FIRST);
        TextComponent[] names = {
                TextComponent.literal("First mode"),
                TextComponent.literal("Second mode")
        };
        return builder("cycling", Mode.class, box)
                .setControl(option -> new CyclingControl<>(option, Mode.values(), names))
                .build();
    }

    static Option<Integer> unknownControl() {
        Box<Integer> box = new Box<>(1);
        return builder("unknown", Integer.class, box)
                .setControl(option -> new Control<>() {
                    @Override
                    public Option<Integer> getOption() {
                        return option;
                    }

                    @Override
                    public ControlElement<Integer> createElement(Dim2i dim) {
                        throw new AssertionError("Unknown control must be rejected before element creation");
                    }

                    @Override
                    public int getMaxWidth() {
                        return 1;
                    }
                })
                .build();
    }

    private static <T> OptionImpl.Builder<Box<T>, T> builder(String path, Class<T> type, Box<T> box) {
        OptionStorage<Box<T>> storage = () -> box;
        return OptionImpl.createBuilder(type, storage)
                .setId(OptionIdentifier.create("compat_test", path, type))
                .setName(TextComponent.literal(path))
                .setTooltip(TextComponent.literal(path + " tooltip"))
                .setBinding((data, value) -> data.value = value, data -> data.value);
    }

    private static final class Box<T> {
        private T value;

        private Box(T value) {
            this.value = value;
        }
    }
}
