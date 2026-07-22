package org.taumc.celeritas.api.options.structure;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.binding.GenericBinding;
import org.taumc.celeritas.api.options.binding.OptionBinding;
import org.taumc.celeritas.api.options.control.Control;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Standalone implementation of the legacy option model.
 */
public class OptionImpl<S, T> implements Option<T> {
    private final OptionStorage<S> storage;
    private final OptionIdentifier<T> id;
    private final TextComponent name;
    private final TextComponent tooltip;
    private final OptionBinding<S, T> binding;
    private Control<T> control;
    private final OptionImpact impact;
    private final EnumSet<OptionFlag> flags;
    private final BooleanSupplier enabled;
    private T modifiedValue;

    private OptionImpl(OptionStorage<S> storage, OptionIdentifier<T> id, TextComponent name,
                       TextComponent tooltip, OptionBinding<S, T> binding,
                       OptionImpact impact, EnumSet<OptionFlag> flags, BooleanSupplier enabled) {
        this.storage = storage;
        this.id = id;
        this.name = name;
        this.tooltip = tooltip;
        this.binding = binding;
        this.impact = impact;
        this.flags = flags.clone();
        this.enabled = enabled;
    }

    public static <S, T> Builder<S, T> createBuilder(Class<T> type, OptionStorage<S> storage) {
        return new Builder<>(type, storage);
    }

    @Override
    public OptionIdentifier<T> getId() {
        return id;
    }

    @Override
    public TextComponent getName() {
        return name;
    }

    @Override
    public TextComponent getTooltip() {
        return tooltip;
    }

    @Override
    public OptionImpact getImpact() {
        return impact;
    }

    @Override
    public Control<T> getControl() {
        return control;
    }

    @Override
    public T getValue() {
        return modifiedValue != null ? modifiedValue : binding.getValue(storage.getData());
    }

    @Override
    public void setValue(T value) {
        modifiedValue = value;
    }

    @Override
    public void reset() {
        modifiedValue = null;
    }

    @Override
    public OptionStorage<?> getStorage() {
        return storage;
    }

    @Override
    public boolean isAvailable() {
        return enabled.getAsBoolean();
    }

    @Override
    public boolean hasChanged() {
        return modifiedValue != null && !Objects.equals(binding.getValue(storage.getData()), modifiedValue);
    }

    @Override
    public void applyChanges() {
        if (modifiedValue != null) {
            binding.setValue(storage.getData(), modifiedValue);
            modifiedValue = null;
        }
    }

    @Override
    public Collection<OptionFlag> getFlags() {
        return flags;
    }

    public static final class Builder<S, T> {
        private final Class<T> type;
        private final OptionStorage<S> storage;
        private OptionIdentifier<T> id;
        private TextComponent name;
        private TextComponent tooltip;
        private OptionBinding<S, T> binding;
        private Function<OptionImpl<S, T>, Control<T>> control;
        private OptionImpact impact;
        private final EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);
        private BooleanSupplier enabled = () -> true;

        private Builder(Class<T> type, OptionStorage<S> storage) {
            this.type = Objects.requireNonNull(type, "type");
            this.storage = Objects.requireNonNull(storage, "storage");
        }

        public Builder<S, T> setId(OptionIdentifier<T> id) {
            this.id = Objects.requireNonNull(id, "id");
            return this;
        }

        public Builder<S, T> setName(TextComponent name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        public Builder<S, T> setTooltip(TextComponent tooltip) {
            this.tooltip = Objects.requireNonNull(tooltip, "tooltip");
            return this;
        }

        public Builder<S, T> setBinding(BiConsumer<S, T> setter, Function<S, T> getter) {
            this.binding = new GenericBinding<>(setter, getter);
            return this;
        }

        public Builder<S, T> setBinding(OptionBinding<S, T> binding) {
            this.binding = Objects.requireNonNull(binding, "binding");
            return this;
        }

        public Builder<S, T> setControl(Function<OptionImpl<S, T>, Control<T>> control) {
            this.control = Objects.requireNonNull(control, "control");
            return this;
        }

        public Builder<S, T> setImpact(OptionImpact impact) {
            this.impact = impact;
            return this;
        }

        public Builder<S, T> setEnabledPredicate(BooleanSupplier predicate) {
            this.enabled = Objects.requireNonNull(predicate, "predicate");
            return this;
        }

        public Builder<S, T> setEnabled(boolean value) {
            this.enabled = () -> value;
            return this;
        }

        public Builder<S, T> setFlags(OptionFlag... flags) {
            Collections.addAll(this.flags, flags);
            return this;
        }

        public OptionImpl<S, T> build() {
            if (id == null) {
                id = OptionIdentifier.EMPTY.cast();
            } else {
                if (name == null) {
                    name = TextComponent.translatable(id.getModId() + ".options." + id.getPath() + ".name");
                }
                if (tooltip == null) {
                    tooltip = TextComponent.translatable(id.getModId() + ".options." + id.getPath() + ".tooltip");
                }
            }
            Objects.requireNonNull(name, "Name must be specified or inferred from a specified ID");
            Objects.requireNonNull(tooltip, "Tooltip must be specified or inferred from a specified ID");
            Objects.requireNonNull(binding, "Option binding must be specified");
            Objects.requireNonNull(control, "Control must be specified");
            OptionImpl<S, T> result = new OptionImpl<>(storage, id, name, tooltip, binding, impact, flags, enabled);
            result.control = Objects.requireNonNull(control, "Control must be specified").apply(result);
            return result;
        }
    }
}
