package net.coderbot.iris.gui.element;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;

// TODO: look into GuiListExtended & GuiSelectStringEntries
public abstract class IrisGuiSlot extends GuiSlot {
    @Setter @Getter protected boolean renderBackground = true;
    boolean scrolling = false;

    protected IrisGuiSlot(Minecraft mc, int width, int height, int top, int bottom, int slotHeight) {
        super(mc, width, height, top, bottom, slotHeight);
        this.centerListVertically = false;
    }

    @Override
    protected void drawContainerBackground(Tessellator tessellator) {
        if (this.renderBackground) {
            super.drawContainerBackground(tessellator);
        }
    }

    @Override
    protected int getScrollBarX() {
        // Position the scrollbar at the rightmost edge of the screen.
        // By default, the scrollbar is positioned moderately offset from the center.
        return this.width - 6;
    }

    @Override
    protected void drawSelectionBox(int x, int y, int mouseX, int mouseY, float partialTicks) {
        final int oldPadding = this.headerPadding;
        this.headerPadding = 2;
        super.drawSelectionBox(x, y, mouseX, mouseY, partialTicks);
        this.headerPadding = oldPadding;
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
        // Do nothing
    }

    protected boolean elementClicked(int index, boolean doubleClick, int mouseX, int mouseY, int button) {
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.getEnabled()) {
            return false;
        }

        final int size = this.getSize();
        final int scrollBarX = this.getScrollBarX();
        final int elementLeft = this.width / 2 - this.getListWidth() / 2;
        final int elementRight = this.width / 2 + this.getListWidth() / 2;
        final int relativeY = mouseY - this.top - this.headerPadding + (int) this.amountScrolled - 4;
        boolean handled = false;

        if (mouseX <= this.left || mouseX >= this.right || mouseY <= this.top || mouseY >= this.bottom) {
            return handled;
        }

        if (mouseButton == 0 && mouseX >= scrollBarX && mouseX <= scrollBarX + 6) {
            scrolling = true;
            this.initialClickY = mouseY;
        } else if (mouseButton == 0 || mouseButton == 1) {
            final int index = relativeY / this.slotHeight;

            if (mouseX >= elementLeft && mouseX <= elementRight && index >= 0 && relativeY >= 0 && index < size) {
                final boolean doubleClick = index == this.selectedElement && Minecraft.getSystemTime() - this.lastClicked < 250L;

                handled = this.elementClicked(index, doubleClick, mouseX, mouseY, mouseButton);
                this.selectedElement = index;
                this.lastClicked = Minecraft.getSystemTime();
            } else if (mouseX >= elementLeft && mouseX <= elementRight && relativeY < 0) {
                this.clickedHeader(mouseX - elementLeft, mouseY - this.top + (int) this.amountScrolled - 4);
            }
        }

        return handled;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        scrolling = false;
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.scrolling) {
            this.amountScrolled -= (mouseY - this.initialClickY);
            this.initialClickY = mouseY;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
