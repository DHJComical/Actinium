package net.caffeinemc.mods.sodium.client.gui.widgets;

import lombok.Getter;
import lombok.Setter;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.caffeinemc.mods.sodium.client.gui.input.FocusTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.init.SoundEvents;

/** Base widget with stable dimensions and keyboard focus. */
public abstract class AbstractWidget implements FocusTarget {
    protected final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
    @Getter
    @Setter
    protected GuiRect bounds;
    protected boolean focused;

    protected AbstractWidget(GuiRect bounds) {
        this.bounds = bounds;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    protected void playClickSound() {
        Minecraft.getMinecraft().getSoundHandler()
                .playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
