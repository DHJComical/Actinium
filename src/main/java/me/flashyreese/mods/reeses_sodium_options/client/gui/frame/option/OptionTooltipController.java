package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import com.dhj.actinium.gui.rso.compat.Component;
import com.dhj.actinium.gui.rso.compat.FormattedCharSequence;
import com.dhj.actinium.gui.rso.compat.GuiGraphicsExtractor;
import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoModOptions;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoOption;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiThemes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class OptionTooltipController {
    private static final int DEFAULT_TOOLTIP_BORDER_COLOR = 0xFF94E4D3;
    private static final int TEXT_PADDING = 3;
    private static final int BOX_PADDING = 3;
    private static final int LINE_HEIGHT = 12;

    private final LayoutBounds viewportBounds;
    private final RsoModOptions modOptions;
    private final OptionStateStore optionStateStore;
    private final BoxRenderer boxRenderer;
    private long targetStartTime;
    private @Nullable OptionRow targetElement;

    OptionTooltipController(LayoutBounds viewportBounds, RsoModOptions modOptions, OptionStateStore optionStateStore, BoxRenderer boxRenderer) {
        this.viewportBounds = viewportBounds;
        this.modOptions = modOptions;
        this.optionStateStore = optionStateStore;
        this.boxRenderer = boxRenderer;
    }

    void render(GuiGraphicsExtractor guiGraphics, List<OptionRow> optionRows, int mouseX, int mouseY) {
        OptionRow targetElement = this.findTargetOptionRow(optionRows, mouseX, mouseY);
        if (targetElement != null && this.targetElement == targetElement) {
            if (this.targetStartTime == 0) {
                this.targetStartTime = System.currentTimeMillis();
            }

            this.renderTooltip(guiGraphics, targetElement);
        } else {
            this.targetStartTime = 0;
            this.targetElement = targetElement;
        }
    }

    private @Nullable OptionRow findTargetOptionRow(List<OptionRow> optionRows, int mouseX, int mouseY) {
        OptionRow hoveredElement = this.findHoveredOptionRow(optionRows, mouseX, mouseY);
        if (hoveredElement != null) {
            return hoveredElement;
        }

        OptionRow focusedElement = this.findFocusedOptionRow(optionRows);
        if (focusedElement != null) {
            return focusedElement;
        }

        return this.findSelectedSearchResultRow(optionRows);
    }

    private @Nullable OptionRow findHoveredOptionRow(List<OptionRow> optionRows, int mouseX, int mouseY) {
        if (!this.viewportBounds.contains(mouseX, mouseY)) {
            return null;
        }

        return optionRows.stream()
                .filter(this::isVisibleOptionRow)
                .filter(optionRow -> optionRow.isMouseOver(mouseX, mouseY))
                .findFirst()
                .orElse(null);
    }

    private @Nullable OptionRow findFocusedOptionRow(List<OptionRow> optionRows) {
        if (!BaseWidget.isKeyboardFocusVisible()) {
            return null;
        }

        return optionRows.stream()
                .filter(this::isVisibleOptionRow)
                .filter(OptionRow::isFocused)
                .findFirst()
                .orElse(null);
    }

    private @Nullable OptionRow findSelectedSearchResultRow(List<OptionRow> optionRows) {
        if (!this.optionStateStore.searchActive()) {
            return null;
        }

        return optionRows.stream()
                .filter(this::isVisibleOptionRow)
                .filter(this::isSelectedSearchResult)
                .findFirst()
                .orElse(null);
    }

    private boolean isSelectedSearchResult(OptionRow optionRow) {
        String id = optionRow.getOption().rso$getId();
        if (id == null || id.isEmpty()) {
            return false;
        }

        return this.optionStateStore.optionUiState(new net.minecraft.util.ResourceLocation(id)).isSelected();
    }

    private boolean isVisibleOptionRow(OptionRow optionRow) {
        return this.viewportBounds.overlaps(optionRow.getDimensions());
    }

    private void renderTooltip(GuiGraphicsExtractor guiGraphics, OptionRow element) {
        if (this.targetStartTime + ReeseSodiumOptionsConfig.config().getTooltipDelayMs() > System.currentTimeMillis()) {
            return;
        }

        LayoutBounds dim = element.getDimensions();
        int boxWidth = dim.width();
        int boxY = dim.getLimitY();
        int boxX = dim.x();
        List<FormattedCharSequence> tooltip = this.buildTooltip(element.getOption(), boxWidth);

        if (tooltip.isEmpty()) {
            return;
        }

        int boxHeight = (tooltip.size() * LINE_HEIGHT) + BOX_PADDING;
        int boxYLimit = boxY + boxHeight;
        int boxYCutoff = this.viewportBounds.getLimitY();

        if (boxYLimit > boxYCutoff) {
            boxY -= boxHeight + dim.height();
        }

        if (boxY < 0) {
            boxY = dim.getLimitY();
        }

        this.boxRenderer.drawRect(guiGraphics, boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000);
        int borderColor = ReeseSodiumOptionsConfig.config().isColorThemes() && ReeseSodiumOptionsConfig.config().isThemedTooltipBorders()
                ? GuiThemes.fromSodium(this.modOptions.theme()).theme
                : DEFAULT_TOOLTIP_BORDER_COLOR;
        this.boxRenderer.drawBorder(guiGraphics, boxX, boxY, boxX + boxWidth, boxY + boxHeight, borderColor);

        for (int i = 0; i < tooltip.size(); i++) {
            guiGraphics.text(new com.dhj.actinium.gui.rso.compat.Font(Minecraft.getMinecraft().fontRenderer), tooltip.get(i), boxX + TEXT_PADDING, boxY + TEXT_PADDING + (i * LINE_HEIGHT), 0xFFFFFFFF, true);
        }
    }

    private List<FormattedCharSequence> buildTooltip(RsoOption option, int boxWidth) {
        List<FormattedCharSequence> tooltip = new ArrayList<>();

        String optionId = option.rso$getId();
        if (ReeseSodiumOptionsConfig.config().isTooltipOptionIds() && optionId != null && !optionId.isEmpty()) {
            tooltip.add(FormattedCharSequence.forward(TextFormatting.GRAY + optionId));
            tooltip.add(FormattedCharSequence.forward(""));
        }

        tooltip.addAll(new com.dhj.actinium.gui.rso.compat.Font(Minecraft.getMinecraft().fontRenderer).split(Component.from(option.getTooltip()), boxWidth - (TEXT_PADDING * 2)));

        String impact = option.getImpactName();
        if (impact != null) {
            tooltip.add(FormattedCharSequence.forward(TextFormatting.GRAY + net.minecraft.client.resources.I18n.format(
                    "sodium.options.performance_impact_string", impact)));
        }

        return tooltip;
    }

    interface BoxRenderer {
        void drawRect(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int color);

        void drawBorder(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int color);
    }
}
