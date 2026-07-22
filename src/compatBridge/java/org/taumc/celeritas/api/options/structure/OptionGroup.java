package org.taumc.celeritas.api.options.structure;

import org.taumc.celeritas.api.OptionGroupConstructionEvent;
import org.taumc.celeritas.api.options.OptionIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Legacy option group backed by the current Embeddium group.
 */
public class OptionGroup {
    public static final OptionIdentifier<Void> DEFAULT_ID = OptionIdentifier.create("celeritas", "empty");

    protected OptionGroup(OptionIdentifier<Void> id, List<Option<?>> options) {
        this.id = id;
        this.options = List.copyOf(options);
    }

    public final OptionIdentifier<Void> id;
    private final List<Option<?>> options;

    public OptionIdentifier<Void> getId() {
        return id;
    }

    public List<Option<?>> getOptions() {
        return options;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final List<Option<?>> options = new ArrayList<>();
        private OptionIdentifier<Void> id;

        public Builder setId(OptionIdentifier<Void> id) {
            this.id = id;
            return this;
        }

        public Builder add(Option<?> option) {
            options.add(option);
            return this;
        }

        public Builder addConditionally(boolean condition, Supplier<Option<?>> option) {
            if (condition) add(option.get());
            return this;
        }

        public OptionGroup build() {
            OptionIdentifier<Void> groupId = id == null ? DEFAULT_ID : id;
            OptionGroupConstructionEvent.BUS.post(new OptionGroupConstructionEvent(groupId, options));
            return new OptionGroup(groupId, List.copyOf(options));
        }
    }
}
