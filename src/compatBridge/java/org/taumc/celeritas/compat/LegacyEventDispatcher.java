package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;
import org.taumc.celeritas.api.OptionGroupConstructionEvent;
import org.taumc.celeritas.api.OptionPageConstructionEvent;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionPage;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatches legacy GUI events while preserving neutral model identity for unchanged entries.
 */
final class LegacyEventDispatcher {
    private LegacyEventDispatcher() {
    }

    static List<OptionModel<?>> dispatchGroup(IdentifierModel<Void> id, List<OptionModel<?>> source) {
        List<Option<?>> options = new ArrayList<>(source.size());
        Map<Option<?>, OptionModel<?>> originals = new IdentityHashMap<>();
        for (OptionModel<?> model : source) {
            Option<?> option = LegacyOptionMapper.createOptionView(model);
            options.add(option);
            originals.put(option, model);
        }
        OptionGroupConstructionEvent event =
                new OptionGroupConstructionEvent(LegacyOptionMapper.createIdentifier(id), options);
        OptionGroupConstructionEvent.BUS.post(event);
        List<OptionModel<?>> result = new ArrayList<>(options.size());
        for (Option<?> option : options) {
            OptionModel<?> original = originals.get(option);
            result.add(original != null ? original : LegacyOptionMapper.describeOption(option));
        }
        return result;
    }

    static List<OptionGroupModel> dispatchPage(IdentifierModel<Void> id, TextComponent name) {
        OptionPageConstructionEvent event =
                new OptionPageConstructionEvent(LegacyOptionMapper.createIdentifier(id), name);
        OptionPageConstructionEvent.BUS.post(event);
        return event.getAdditionalGroups().stream()
                .map(LegacyOptionMapper::describeGroup)
                .toList();
    }

    static List<OptionPageModel> dispatchGui(List<OptionPageModel> source) {
        List<OptionPage> pages = new ArrayList<>(source.size());
        Map<OptionPage, OptionPageModel> originals = new IdentityHashMap<>();
        for (OptionPageModel model : source) {
            OptionPage page = LegacyOptionMapper.createPageView(model);
            pages.add(page);
            originals.put(page, model);
        }
        OptionGUIConstructionEvent.BUS.post(new OptionGUIConstructionEvent(pages));
        List<OptionPageModel> result = new ArrayList<>(pages.size());
        for (OptionPage page : pages) {
            OptionPageModel original = originals.get(page);
            result.add(original != null ? original : LegacyOptionMapper.describePage(page));
        }
        return result;
    }
}
