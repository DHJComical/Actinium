package net.caffeinemc.mods.sodium.client.gui.text;

import net.minecraft.util.text.TextComponentString;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientTranslatedTextTest {
    @Test
    void resolvesComponentArgumentsToStringsAtClientFormatBoundary() {
        String translated = ClientTranslatedText.translate("test.key",
                new Object[] { new TextComponentString("nested"), 7 },
                (key, arguments) -> key + ":" + arguments[0] + ":" + arguments[1]);

        assertEquals("test.key:nested:7", translated);
    }

    @Test
    void explicitTranslatorIsEvaluatedAgainWheneverTextIsRead() {
        AtomicReference<String> translated = new AtomicReference<>("first language");
        ClientTranslatedText text = new ClientTranslatedText("test.key",
                (key, arguments) -> translated.get());

        assertEquals("first language", text.getUnformattedText());
        translated.set("second language");
        assertEquals("second language", text.getUnformattedText());
    }

    @Test
    void copyDeepCopiesComponentArguments() {
        TextComponentString argument = new TextComponentString("nested");
        ClientTranslatedText text = new ClientTranslatedText("test.key",
                (key, arguments) -> arguments[0].toString(), argument);
        text.setStyle(text.getStyle().setBold(true));
        text.appendText(" sibling");

        ClientTranslatedText copy = text.createCopy();
        argument.appendText(" changed");
        text.getStyle().setBold(false);
        text.getSiblings().getFirst().appendText(" changed");

        assertEquals("nested changed sibling changed", text.getUnformattedText());
        assertEquals("nested sibling", copy.getUnformattedText());
        assertNotSame(text.getStyle(), copy.getStyle());
        assertNotSame(text.getSiblings().getFirst(), copy.getSiblings().getFirst());
        assertEquals((boolean) Boolean.TRUE, copy.getStyle().getBold());
    }

    @Test
    void rejectsBlankTranslationKeys() {
        assertThrows(IllegalArgumentException.class, () -> new ClientTranslatedText(" "));
        assertThrows(IllegalArgumentException.class,
                () -> ClientTranslatedText.translate(" ", new Object[0], (key, arguments) -> "unused"));
    }

    @Test
    void rejectsNullTranslationResults() {
        ClientTranslatedText text = new ClientTranslatedText("test.key", (key, arguments) -> null);

        assertThrows(NullPointerException.class, text::getUnformattedText);
    }
}
