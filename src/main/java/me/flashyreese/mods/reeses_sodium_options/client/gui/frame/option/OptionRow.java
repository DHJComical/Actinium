package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import com.dhj.actinium.gui.rso.compat.GuiEventListener;
import com.dhj.actinium.gui.rso.compat.NarratableEntry;
import com.dhj.actinium.gui.rso.compat.Renderable;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoOption;

public interface OptionRow extends Renderable, GuiEventListener, NarratableEntry {
    RsoOption getOption();

    LayoutBounds getDimensions();

    void releaseActionButtonLayoutHold();

    boolean handleBackNavigation();

    boolean undoFocusedActionButton();

    void clearActionButtonFocus();
}
