package com.dhj.actinium.compat.sodium;

import org.embeddedt.embeddium.api.options.structure.OptionPage;

import java.util.List;

/**
 * Extension boundary implemented by the optional Celeritas compatibility jar.
 * The Actinium jar only knows the current option page type.
 */
@FunctionalInterface
public interface LegacyOptionPageProvider {
    /** Appends pages supplied by an optional legacy compatibility implementation. */
    void appendPages(List<OptionPage> pages);
}
