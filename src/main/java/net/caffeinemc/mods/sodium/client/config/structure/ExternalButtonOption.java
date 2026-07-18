package net.caffeinemc.mods.sodium.client.config.structure;

import lombok.Getter;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

/** Non-stateful option used as an external screen navigation command. */
public final class ExternalButtonOption extends Option {
    private final ITextComponent tooltip;
    /**
     * -- GETTER --
     * Returns the screen-opening command owned by the integration layer.
     */
    @Getter
    private final Consumer<GuiScreen> screenConsumer;

    public ExternalButtonOption(ResourceLocation id, Collection<ResourceLocation> dependencies, ITextComponent name,
                                DependentValue<Boolean> enabled, ITextComponent tooltip,
                                Consumer<GuiScreen> screenConsumer) {
        super(id, dependencies, name, enabled);
        this.tooltip = Objects.requireNonNull(tooltip, "External button tooltip must not be null");
        this.screenConsumer = Objects.requireNonNull(screenConsumer, "External button screen consumer must not be null");
    }

    @Override
    public ITextComponent getTooltip() {
        return this.tooltip;
    }

    /** Returns the upstream-named screen consumer contract. */
    public Consumer<GuiScreen> getCurrentScreenConsumer() {
        return this.screenConsumer;
    }
}
