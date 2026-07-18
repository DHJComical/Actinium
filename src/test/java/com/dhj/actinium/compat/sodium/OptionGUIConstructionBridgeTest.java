package com.dhj.actinium.compat.sodium;

import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionImpl;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptionGUIConstructionBridgeTest {
    @Test
    void rollsBackFailingListenerAndKeepsLaterContribution() {
        OptionPage builtIn = page("actinium", "general");
        EventHandlerRegistrar.Handler<OptionGUIConstructionEvent> broken = event -> {
            event.addPage(page("broken", "partial"));
            throw new IllegalStateException("listener failed");
        };
        EventHandlerRegistrar.Handler<OptionGUIConstructionEvent> working =
                event -> event.addPage(page("testmod", "extra"));
        OptionGUIConstructionEvent.BUS.addListener(broken);
        OptionGUIConstructionEvent.BUS.addListener(working);
        try {
            Map<String, List<OptionPage>> extensions =
                    OptionGUIConstructionBridge.collectExtensions(List.of(builtIn));

            assertEquals(List.of("testmod"), List.copyOf(extensions.keySet()));
            assertEquals("testmod:extra", extensions.get("testmod").getFirst().getId().toString());
        } finally {
            OptionGUIConstructionEvent.BUS.removeListener(broken);
            OptionGUIConstructionEvent.BUS.removeListener(working);
        }
    }

    private static OptionPage page(String namespace, String path) {
        MutableStorage storage = new MutableStorage();
        var option = OptionImpl.createBuilder(boolean.class, storage)
                .setId(OptionIdentifier.create(namespace, path + "_enabled", boolean.class))
                .setName(TextComponent.literal("Enabled"))
                .setTooltip(TextComponent.literal("Enabled tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding((data, value) -> data.value = value, data -> data.value)
                .build();
        return new OptionPage(OptionIdentifier.create(namespace, path), TextComponent.literal(path),
                List.of(OptionGroup.createBuilder().add(option).build()));
    }

    private static final class MutableStorage implements OptionStorage<MutableStorage> {
        private boolean value = true;

        @Override
        public MutableStorage getData() {
            return this;
        }
    }
}
