package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

import net.minecraft.util.ResourceLocation;
import org.embeddedt.embeddium.api.options.structure.Option;

public record SearchResultEntry(String tabKey, ResourceLocation optionId, Option<?> option) {
}
