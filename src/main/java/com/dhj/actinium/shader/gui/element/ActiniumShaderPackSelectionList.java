package com.dhj.actinium.shader.gui.element;

import com.dhj.actinium.shader.gui.screen.ActiniumShaderPackScreen;
import com.dhj.actinium.shader.pack.ActiniumShaderPack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ActiniumShaderPackSelectionList extends GuiSlot {
    private final ActiniumShaderPackScreen screen;
    private final List<Entry> entries = new ArrayList<>();

    private final ToggleEntry toggleEntry;
    private @Nullable PackEntry selectedEntry;
    private @Nullable PackEntry appliedEntry;

    public ActiniumShaderPackSelectionList(ActiniumShaderPackScreen screen, Minecraft minecraft, int width, int height, int top, int bottom) {
        super(minecraft, width, height, top, bottom, 20);
        this.screen = screen;
        this.toggleEntry = new ToggleEntry();
    }

    public void refresh(List<ActiniumShaderPack> packs, @Nullable String selectedPackName, @Nullable String appliedPackName, boolean shadersEnabled) {
        this.entries.clear();
        this.selectedEntry = null;
        this.appliedEntry = null;
        this.toggleEntry.shadersEnabled = shadersEnabled;
        this.entries.add(this.toggleEntry);

        for (ActiniumShaderPack pack : packs) {
            PackEntry entry = new PackEntry(pack);
            this.entries.add(entry);

            if (pack.name().equals(selectedPackName)) {
                this.selectedEntry = entry;
            }

            if (pack.name().equals(appliedPackName)) {
                this.appliedEntry = entry;
            }
        }

        if (this.selectedEntry == null && !packs.isEmpty()) {
            Entry firstPackEntry = this.entries.size() > 1 ? this.entries.get(1) : null;
            if (firstPackEntry instanceof PackEntry packEntry) {
                this.selectedEntry = packEntry;
            }
        }

        this.screen.onSelectionChanged();
    }

    public boolean areShadersEnabled() {
        return this.toggleEntry.shadersEnabled;
    }

    public void setShadersEnabled(boolean shadersEnabled) {
        this.toggleEntry.shadersEnabled = shadersEnabled;
        this.screen.onSelectionChanged();
    }

    public @Nullable String getSelectedPackName() {
        return this.selectedEntry == null ? null : this.selectedEntry.pack.name();
    }

    public void setAppliedSelection(@Nullable String appliedPackName, boolean shadersEnabled) {
        this.toggleEntry.shadersEnabled = shadersEnabled;
        this.appliedEntry = null;

        if (appliedPackName != null) {
            for (Entry entry : this.entries) {
                if (entry instanceof PackEntry packEntry && appliedPackName.equals(packEntry.pack.name())) {
                    this.appliedEntry = packEntry;
                    break;
                }
            }
        }

        this.screen.onSelectionChanged();
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int slotIndex = this.getSlotIndexFromScreenCoords(mouseX, mouseY);

        if (slotIndex < 0 || slotIndex >= this.entries.size() || mouseButton != 0) {
            return false;
        }

        boolean isDoubleClick = slotIndex == this.selectedElement && Minecraft.getSystemTime() - this.lastClicked < 250L;
        this.elementClicked(slotIndex, isDoubleClick, mouseX, mouseY);
        this.selectedElement = slotIndex;
        this.lastClicked = Minecraft.getSystemTime();
        return true;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    protected int getSize() {
        return this.entries.size();
    }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
        Entry entry = this.entries.get(slotIndex);

        if (entry instanceof ToggleEntry) {
            this.toggleEntry.shadersEnabled = !this.toggleEntry.shadersEnabled;
            this.screen.onSelectionChanged();
            return;
        }

        if (entry instanceof PackEntry packEntry) {
            this.selectedEntry = packEntry;

            if (!this.toggleEntry.shadersEnabled) {
                this.toggleEntry.shadersEnabled = true;
            }

            this.screen.onSelectionChanged();

            if (isDoubleClick) {
                this.screen.applyChanges(true);
            }
        }
    }

    @Override
    protected boolean isSelected(int slotIndex) {
        Entry entry = this.entries.get(slotIndex);
        return entry == this.selectedEntry;
    }

    @Override
    protected void drawBackground() {
    }

    @Override
    protected void drawSlot(int index, int x, int y, int slotHeight, int mouseX, int mouseY, float partialTicks) {
        Entry entry = this.entries.get(index);
        boolean hovered = this.getSlotIndexFromScreenCoords(mouseX, mouseY) == index;
        int centerX = x + (this.getListWidth() / 2) - 2;

        if (entry instanceof ToggleEntry toggle) {
            String label;
            if (!toggle.allowEnableShadersButton) {
                label = I18n.format("options.actinium.shaders.nonePresent");
            } else if (toggle.shadersEnabled) {
                label = I18n.format("options.actinium.shaders.enabled");
            } else {
                label = I18n.format("options.actinium.shaders.disabled");
            }

            if (hovered) {
                label = TextFormatting.BOLD + label;
            }

            this.mc.fontRenderer.drawStringWithShadow(label, centerX - (this.mc.fontRenderer.getStringWidth(label) / 2), y + 5, 0xFFFFFF);
            return;
        }

        if (entry instanceof PackEntry packEntry) {
            String name = packEntry.pack.name();
            int color = 0xFFFFFF;

            if (this.mc.fontRenderer.getStringWidth(name) > this.getListWidth() - 3) {
                name = this.mc.fontRenderer.trimStringToWidth(name, this.getListWidth() - 8) + "...";
            }

            if (hovered) {
                name = TextFormatting.BOLD + name;
            }

            if (packEntry == this.appliedEntry && this.toggleEntry.shadersEnabled) {
                color = 0xFFF263;
            } else if (!this.toggleEntry.shadersEnabled && !hovered) {
                color = 0xA2A2A2;
            }

            this.mc.fontRenderer.drawStringWithShadow(name, centerX - (this.mc.fontRenderer.getStringWidth(name) / 2), y + 5, color);
        }
    }

    @Override
    public int getListWidth() {
        return Math.min(308, this.width - 50);
    }

    @Override
    protected int getScrollBarX() {
        return this.width - 6;
    }

    public boolean hasAnyPacks() {
        return this.entries.size() > 1;
    }

    public void refreshToggleAvailability() {
        this.toggleEntry.allowEnableShadersButton = this.hasAnyPacks();
        if (!this.toggleEntry.allowEnableShadersButton) {
            this.toggleEntry.shadersEnabled = false;
        }
        this.screen.onSelectionChanged();
    }

    private abstract static class Entry {
    }

    private static final class ToggleEntry extends Entry {
        private boolean shadersEnabled;
        private boolean allowEnableShadersButton;
    }

    private static final class PackEntry extends Entry {
        private final ActiniumShaderPack pack;

        private PackEntry(ActiniumShaderPack pack) {
            this.pack = pack;
        }
    }
}
