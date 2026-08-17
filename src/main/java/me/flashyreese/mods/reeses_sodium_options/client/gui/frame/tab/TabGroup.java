package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoModOptions;

import java.util.List;

record TabGroup(String id, RsoModOptions modOptions, List<Tab<?>> tabs) {
}
