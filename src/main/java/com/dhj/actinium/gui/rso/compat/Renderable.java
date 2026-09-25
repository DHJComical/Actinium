package com.dhj.actinium.gui.rso.compat;

/**
 * Newer-Minecraft style renderable adapted to the 1.12.2 client.
 * Widgets extract their render state into the provided graphics context.
 */
public interface Renderable {

    /**
     * Extracts the render state of this element.
     *
     * @param guiGraphics the graphics context
     * @param mouseX      the GUI-space mouse X
     * @param mouseY      the GUI-space mouse Y
     * @param delta       the partial tick delta
     */
    void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta);
}
