package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.gui.render.ScissorScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Scrollable owner/page sidebar with selected state and external page navigation. */
public final class PageListWidget extends AbstractWidget {
    private final GuiScreen parent;
    private final Consumer<Page> pageConsumer;
    private final List<Entry> entries = new ArrayList<>();
    private final ScrollState scroll = new ScrollState(null);
    private final ScrollbarWidget scrollbar;
    private Page selected;

    public PageListWidget(GuiRect bounds, List<ModOptions> owners, GuiScreen parent, Consumer<Page> pageConsumer) {
        super(bounds);
        this.parent = parent;
        this.pageConsumer = pageConsumer;
        int contentY = 0;
        for (ModOptions owner : owners) {
            int headerHeight = this.font.FONT_HEIGHT * 3;
            this.entries.add(new Entry(owner, null, contentY, headerHeight));
            contentY += headerHeight;
            for (Page page : owner.pages()) {
                this.entries.add(new Entry(owner, page, contentY, Layout.ENTRY_HEIGHT));
                contentY += Layout.ENTRY_HEIGHT;
            }
            contentY += Layout.INNER_MARGIN;
        }
        this.scroll.setContext(bounds.height(), contentY);
        this.scrollbar = new ScrollbarWidget(new GuiRect(bounds.right() - Layout.SCROLLBAR_WIDTH, bounds.y(),
                Layout.SCROLLBAR_WIDTH, bounds.height()), this.scroll);
    }

    public void render(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int bands = Math.max(1, this.bounds.height());
        for (int i = 0; i < bands; i++) {
            float t = i / (float) bands;
            Gui.drawRect(this.bounds.x(), this.bounds.y() + i, this.bounds.right(), this.bounds.y() + i + 1,
                    interpolate(Colors.BACKGROUND_LIGHT, Colors.BACKGROUND_DEFAULT, t));
        }
        try (ScissorScope ignored = ScissorScope.open(this.bounds, screenWidth, screenHeight)) {
            for (Entry entry : this.entries) {
                int y = this.bounds.y() + entry.contentY - this.scroll.amount();
                if (y + entry.height <= this.bounds.y() || y >= this.bounds.bottom()) {
                    continue;
                }
                GuiRect row = new GuiRect(this.bounds.x(), y,
                        this.bounds.width() - Layout.SCROLLBAR_WIDTH, entry.height);
                if (entry.page == null) {
                    this.renderOwner(entry.owner, row);
                } else {
                    this.renderPage(entry, row, mouseX, mouseY);
                }
            }
        }
        this.scrollbar.render();
    }

    private void renderOwner(ModOptions owner, GuiRect row) {
        int textX = row.x() + 8;
        if (owner.icon() != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(owner.icon());
            if (owner.iconMonochrome()) {
                ColorTheme theme = owner.theme();
                GlStateManager.color(((theme.theme() >>> 16) & 0xFF) / 255.0F,
                        ((theme.theme() >>> 8) & 0xFF) / 255.0F, (theme.theme() & 0xFF) / 255.0F, 1.0F);
            }
            int iconSize = Math.max(1, row.height() - Layout.ICON_MARGIN * 2);
            Gui.drawScaledCustomSizeModalRect(row.x() + Layout.ICON_MARGIN,
                    row.y() + (row.height() - iconSize) / 2, 0.0F, 0.0F,
                    iconSize, iconSize, iconSize, iconSize, iconSize, iconSize);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            textX = row.x() + Layout.ICON_MARGIN * 2 + iconSize;
        }
        String name = this.font.trimStringToWidth(owner.name(), Math.max(1, row.right() - textX - 4));
        String version = this.font.trimStringToWidth(owner.version(), Math.max(1, row.right() - textX - 4));
        this.font.drawString(name, textX, row.y() + 5, Colors.FOREGROUND);
        this.font.drawString(version, textX, row.y() + 15, Colors.FOREGROUND_DISABLED);
    }

    private static int interpolate(int start, int end, float amount) {
        int a = (int) (((start >>> 24) & 255) * (1 - amount) + ((end >>> 24) & 255) * amount);
        int r = (int) (((start >>> 16) & 255) * (1 - amount) + ((end >>> 16) & 255) * amount);
        int g = (int) (((start >>> 8) & 255) * (1 - amount) + ((end >>> 8) & 255) * amount);
        int b = (int) ((start & 255) * (1 - amount) + (end & 255) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private void renderPage(Entry entry, GuiRect row, int mouseX, int mouseY) {
        boolean selectedEntry = entry.page == this.selected;
        if (row.contains(mouseX, mouseY)) {
            Gui.drawRect(row.x(), row.y(), row.right(), row.bottom(), Colors.BACKGROUND_HOVER);
        }
        if (selectedEntry) {
            Gui.drawRect(row.right() - Layout.PAGE_ENTRY_SELECTION_BAR_WIDTH, row.y(), row.right(), row.bottom(),
                    entry.owner.theme().themeHighlight());
        }
        String prefix = entry.page instanceof ExternalPage ? "> " : "";
        String text = this.font.trimStringToWidth(prefix + entry.page.name().getUnformattedText(),
                Math.max(1, row.width() - 18));
        int textColor = selectedEntry ? entry.owner.theme().themeHighlight()
                : (row.contains(mouseX, mouseY) ? Colors.FOREGROUND : entry.owner.theme().themeDisabled());
        this.font.drawString(text, row.x() + 8, row.y() + (row.height() - this.font.FONT_HEIGHT) / 2, textColor);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (this.scrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || !this.bounds.contains(mouseX, mouseY)) {
            return false;
        }
        for (Entry entry : this.entries) {
            int y = this.bounds.y() + entry.contentY - this.scroll.amount();
            GuiRect row = new GuiRect(this.bounds.x(), y,
                    this.bounds.width(), entry.height);
            if (entry.page == null && row.contains(mouseX, mouseY)) {
                List<Page> pages = entry.owner.pages();
                if (!pages.isEmpty()) {
                    this.activatePage(pages.get(0));
                }
                this.playClickSound();
                return true;
            }
            if (entry.page != null && row.contains(mouseX, mouseY)) {
                this.activatePage(entry.page);
                this.playClickSound();
                return true;
            }
        }
        return false;
    }

    private void activatePage(Page page) {
        if (page instanceof ExternalPage externalPage) {
            externalPage.screenConsumer().accept(this.parent);
            return;
        }
        this.selected = page;
        this.pageConsumer.accept(page);
    }

    public void mouseScrolled(int delta) {
        this.scroll.scroll(delta);
    }

    public void mouseDragged(int mouseY) {
        this.scrollbar.mouseDragged(mouseY);
    }

    public void mouseReleased() {
        this.scrollbar.mouseReleased();
    }

    public void select(Page page) {
        this.selected = page;
        for (Entry entry : this.entries) {
            if (entry.page == page) {
                if (entry.contentY < this.scroll.amount()) {
                    this.scroll.scrollTo(entry.contentY);
                } else if (entry.contentY + entry.height > this.scroll.amount() + this.bounds.height()) {
                    this.scroll.scrollTo(entry.contentY + entry.height - this.bounds.height());
                }
                return;
            }
        }
    }

    private record Entry(ModOptions owner, Page page, int contentY, int height) {
    }
}
