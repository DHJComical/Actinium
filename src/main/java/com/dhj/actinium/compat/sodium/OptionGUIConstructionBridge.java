package com.dhj.actinium.compat.sodium;

import com.dhj.actinium.runtime.ActiniumRuntime;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.structure.OptionPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Collects option GUI page contributions while isolating each failing event listener. */
public final class OptionGUIConstructionBridge {
    private OptionGUIConstructionBridge() {
    }

    /**
     * Posts the event with complete built-in page context and appends
     * only newly contributed pages to the supplied mutable list.
     */
    public static void collectExtensions(List<OptionPage> mutablePages) {
        List<OptionPage> builtInPages = List.copyOf(mutablePages);
        Set<OptionPage> builtInIdentities = Collections.newSetFromMap(new IdentityHashMap<>());
        builtInIdentities.addAll(builtInPages);
        OptionGUIConstructionEvent event = new OptionGUIConstructionEvent(mutablePages);
        OptionGUIConstructionEvent.BUS.dispatchHandlers(handler -> {
            List<OptionPage> snapshot = List.copyOf(mutablePages);
            try {
                handler.acceptEvent(event);
                validatePages(mutablePages, builtInIdentities);
            } catch (RuntimeException exception) {
                mutablePages.clear();
                mutablePages.addAll(snapshot);
                ActiniumRuntime.logger().error(
                        "OptionGUIConstructionEvent entry '{}' failed and was isolated",
                        handler.getClass().getName(), exception);
            }
        });
        List<OptionPage> extensions = mutablePages.stream()
                .filter(page -> !builtInIdentities.contains(page))
                .toList();
        ActiniumRuntime.logger().info("Collected option GUI extensions: {}",
                extensions.stream().map(page -> page.getId().getModId() + "=" + 1).toList());
    }

    private static void validatePages(List<OptionPage> pages, Set<OptionPage> builtInPages) {
        Set<OptionIdentifier<?>> pageIds = new HashSet<>();
        for (OptionPage page : pages) {
            if (page == null) {
                throw new IllegalArgumentException("Option GUI listener inserted a null page");
            }
            if (page.getId() == null || page.getId().getModId().isBlank() || page.getId().getPath().isBlank()) {
                throw new IllegalArgumentException("Option GUI listener inserted a page without a stable ID");
            }
            if (!pageIds.add(page.getId())) {
                throw new IllegalArgumentException("Duplicate option page ID: " + page.getId());
            }
        }
        if (!pages.containsAll(builtInPages)) {
            throw new IllegalArgumentException("Option GUI listener removed a built-in Actinium page");
        }
    }
}
