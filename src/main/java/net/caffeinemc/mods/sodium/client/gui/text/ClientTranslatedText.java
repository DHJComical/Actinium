package net.caffeinemc.mods.sodium.client.gui.text;

import lombok.Getter;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentBase;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Client-only translated text which resolves against every active resource-pack domain.
 * Minecraft's shared chat language map does not reliably include auxiliary mod domains on 1.12.2.
 */
public final class ClientTranslatedText extends TextComponentBase {
    /**
     * -- GETTER --
     * Returns the translation key used by the active client locale.
     */
    @Getter
    private final String key;
    private final Object[] arguments;
    private final BiFunction<String, Object[], String> translator;

    public ClientTranslatedText(String key, Object... arguments) {
        this(key, I18n::format, arguments);
    }

    /** Creates client text with an explicit translation boundary for integration tests and adapters. */
    public ClientTranslatedText(String key, BiFunction<String, Object[], String> translator, Object... arguments) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Client translation key must not be blank");
        }
        this.key = key;
        this.arguments = arguments == null ? new Object[0] : Arrays.copyOf(arguments, arguments.length);
        this.translator = Objects.requireNonNull(translator, "Client translator must not be null");
    }

    @Override
    public String getUnformattedComponentText() {
        return translate(this.key, this.arguments, this.translator);
    }

    @Override
    public ClientTranslatedText createCopy() {
        Object[] copiedArguments = Arrays.stream(this.arguments)
                .map(argument -> argument instanceof ITextComponent component ? component.createCopy() : argument)
                .toArray();
        ClientTranslatedText copy = new ClientTranslatedText(this.key, this.translator, copiedArguments);
        copy.setStyle(this.getStyle().createShallowCopy());
        for (ITextComponent sibling : this.getSiblings()) {
            copy.appendSibling(sibling.createCopy());
        }
        return copy;
    }

    static String translate(String key, Object[] arguments, BiFunction<String, Object[], String> translator) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Client translation key must not be blank");
        }
        Objects.requireNonNull(translator, "Client translator must not be null");
        // Client I18n formats strings rather than component trees, so component arguments are resolved at read time.
        Object[] resolved = Arrays.stream(arguments == null ? new Object[0] : arguments)
                .map(argument -> argument instanceof ITextComponent component
                        ? component.getUnformattedText()
                        : argument)
                .toArray();
        return Objects.requireNonNull(translator.apply(key, resolved),
                "Client translator returned null for key '" + key + "'");
    }
}
