package org.taumc.celeritas.compat;

import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.api.options.control.*;
import org.taumc.celeritas.api.options.structure.Option;

final class LegacyControlMapper {
    private LegacyControlMapper() {
    }

    @SuppressWarnings("unchecked")
    static <T> ControlModel<T> describe(Control<T> source) {
        if (source instanceof TickBoxControl) {
            return (ControlModel<T>) TickBoxModel.INSTANCE;
        }
        if (source instanceof SliderControl slider) {
            return (ControlModel<T>) new SliderModel(slider.getMin(), slider.getMax(), slider.getInterval(),
                    value -> slider.getFormatter().format(value));
        }
        if (source instanceof CyclingControl<?> cycling) {
            return new CyclingModel<>((T[]) cycling.getAllowedValues(), cycling.getNames());
        }
        throw unsupported(source);
    }

    @SuppressWarnings("unchecked")
    static <T> Control<T> create(Option<T> option, ControlModel<T> model) {
        if (model instanceof TickBoxModel) {
            return (Control<T>) new TickBoxControl((Option<Boolean>) option);
        }
        if (model instanceof SliderModel slider) {
            ControlValueFormatter formatter = value -> slider.formatter().apply(value);
            return (Control<T>) new SliderControl((Option<Integer>) option, slider.min(), slider.max(),
                    slider.interval(), formatter);
        }
        if (model instanceof CyclingModel<?> cycling) {
            return new CyclingControl<>(option, (T[]) cycling.allowedValues(), cycling.names());
        }
        throw unsupported(model);
    }

    private static IllegalArgumentException unsupported(Object source) {
        String message = "Unsupported legacy option control: " + source.getClass().getName();
        CeleritasVintage.logger().error(message);
        return new IllegalArgumentException(message);
    }
}
