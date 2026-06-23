package me.decce.gnetum.compat.celeritas;

import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;

public final class GnetumCeleritasOptionsListener {
    private GnetumCeleritasOptionsListener() {
    }

    public static void onCeleritasOptionsConstruct(OptionGUIConstructionEvent event) {
        event.addPage(GnetumCeleritasOptionPages.page());
    }
}
