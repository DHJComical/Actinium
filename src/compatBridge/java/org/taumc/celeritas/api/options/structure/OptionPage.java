package org.taumc.celeritas.api.options.structure;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.OptionPageConstructionEvent;
import org.taumc.celeritas.api.options.OptionIdentifier;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Legacy option page backed by the current page implementation.
 */
public class OptionPage {
    private final OptionIdentifier<Void> id;
    private final TextComponent name;
    private final List<OptionGroup> groups;
    private final List<Option<?>> options;

    public OptionPage(OptionIdentifier<Void> id, TextComponent name, List<OptionGroup> groups) {
        this(id, name, groups, true);
    }

    protected OptionPage(OptionIdentifier<Void> id, TextComponent name, List<OptionGroup> groups,
                         boolean dispatchConstructionEvent) {
        this.id = id;
        this.name = name;
        this.groups = dispatchConstructionEvent ? collectExtraGroups(groups) : groups;
        this.options = this.groups.stream().flatMap(group -> group.getOptions().stream()).toList();
    }

    private List<OptionGroup> collectExtraGroups(List<OptionGroup> groups) {
        OptionPageConstructionEvent event = new OptionPageConstructionEvent(id, name);
        OptionPageConstructionEvent.BUS.post(event);
        List<OptionGroup> extraGroups = event.getAdditionalGroups();
        return extraGroups.isEmpty() ? groups
                : Stream.of(groups.stream(), extraGroups.stream()).flatMap(Function.identity()).toList();
    }

    public OptionIdentifier<Void> getId() {
        return id;
    }

    public TextComponent getName() {
        return name;
    }

    public List<OptionGroup> getGroups() {
        return groups;
    }

    public List<Option<?>> getOptions() {
        return options;
    }
}
