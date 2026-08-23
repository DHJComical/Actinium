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
import java.util.concurrent.CopyOnWriteArrayList;

/** Collects legacy GUI page contributions while isolating each failing event listener. */
public final class OptionGUIConstructionBridge {
    private static final List<LegacyOptionPageProvider> LEGACY_PROVIDERS = new CopyOnWriteArrayList<>();
    private OptionGUIConstructionBridge() {
    }

    public static void registerLegacyProvider(LegacyOptionPageProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Legacy option page provider must not be null");
        }
        LEGACY_PROVIDERS.add(provider);
    }

    /**
     * Posts the retained event with complete built-in page context and appends
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
                        "Legacy OptionGUIConstructionEvent entry '{}' failed and was isolated",
                        handler.getClass().getName(), exception);
            }
        });
        for (LegacyOptionPageProvider provider : LEGACY_PROVIDERS) {
            List<OptionPage> snapshot = List.copyOf(mutablePages);
            try {
                provider.appendPages(mutablePages);
                validatePages(mutablePages, builtInIdentities);
            } catch (RuntimeException exception) {
                mutablePages.clear();
                mutablePages.addAll(snapshot);
                ActiniumRuntime.logger().error("Legacy Celeritas page provider failed and was isolated", exception);
            }
        }
        List<OptionPage> extensions = mutablePages.stream()
                .filter(page -> !builtInIdentities.contains(page))
                .toList();
        ActiniumRuntime.logger().info("Collected legacy option GUI extensions: {}",
                extensions.stream().map(page -> page.getId().getModId() + "=" + 1).toList());
    }

    private static void validatePages(List<OptionPage> pages, Set<OptionPage> builtInPages) {
        Set<OptionIdentifier<?>> pageIds = new HashSet<>();
        for (OptionPage page : pages) {
            if (page == null) {
                throw new IllegalArgumentException("Legacy option GUI listener inserted a null page");
            }
            if (page.getId() == null || page.getId().getModId().isBlank() || page.getId().getPath().isBlank()) {
                throw new IllegalArgumentException("Legacy option GUI listener inserted a page without a stable ID");
            }
            if (!pageIds.add(page.getId())) {
                throw new IllegalArgumentException("Duplicate legacy option page ID: " + page.getId());
            }
        }
        if (!pages.containsAll(builtInPages)) {
            throw new IllegalArgumentException("Legacy option GUI listener removed a built-in Actinium page");
        }
    }
}
