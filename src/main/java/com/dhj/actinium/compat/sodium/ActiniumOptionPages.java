package com.dhj.actinium.compat.sodium;

import com.dhj.actinium.gui.ActiniumGameOptionPages;
import com.dhj.actinium.runtime.ActiniumRuntime;
import net.irisshaders.iris.compat.sodium.IrisConfigEntryPoint;
import org.embeddedt.embeddium.impl.gui.options.CommonOptionPages;
import org.embeddedt.embeddium.api.options.structure.OptionPage;

import java.util.ArrayList;
import java.util.List;

/**
 * Actinium's own built-in option page collection: returns embeddium
 * {@link OptionPage} forms of the Actinium video option pages and the Iris
 * compatibility pages directly, without any Sodium config model conversion.
 */
public final class ActiniumOptionPages {
    private ActiniumOptionPages() {
    }

    /** Builds all built-in pages Actinium currently exposes (embeddium model). */
    public static List<OptionPage> builtInPages() {
        List<OptionPage> pages = new ArrayList<>(List.of(
                ActiniumGameOptionPages.general(),
                ActiniumGameOptionPages.quality(),
                CommonOptionPages.performance(ActiniumRuntime.options()),
                ActiniumGameOptionPages.advanced(),
                ActiniumGameOptionPages.debug()));
        pages.addAll(new IrisConfigEntryPoint().createPages());
        return List.copyOf(pages);
    }
}
