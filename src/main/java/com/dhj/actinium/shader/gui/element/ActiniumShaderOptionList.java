package com.dhj.actinium.shader.gui.element;

import com.dhj.actinium.shader.gui.screen.ActiniumShaderOptionScreen;
import com.dhj.actinium.shader.options.ActiniumShaderOption;
import com.dhj.actinium.shader.options.ActiniumShaderProfile;
import com.dhj.actinium.shader.options.ActiniumShaderProfileSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

public class ActiniumShaderOptionList extends GuiSlot {
    private static final int CELL_GAP = 6;

    private final ActiniumShaderOptionScreen screen;
    private final List<Row> rows = new ArrayList<>();
    private String hoveredCommentTitle = "";
    private String hoveredCommentBody = "";

    public ActiniumShaderOptionList(ActiniumShaderOptionScreen screen, Minecraft minecraft, int width, int height, int top, int bottom) {
        super(minecraft, width, height, top, bottom, 24);
        this.screen = screen;
        this.setShowSelectionBox(false);
    }

    public void rebuild() {
        this.rows.clear();
        this.hoveredCommentTitle = "";
        this.hoveredCommentBody = "";

        List<String> tokens = new ArrayList<>();

        for (String token : this.screen.getMenu().getScreenTokens(this.screen.getCurrentScreenId())) {
            if ("*".equals(token)) {
                tokens.addAll(this.screen.getMenu().getUnusedOptionIds());
            } else {
                tokens.add(token);
            }
        }

        int columns = this.screen.getMenu().getColumnCount(this.screen.getCurrentScreenId());
        List<CellWidget> currentRow = new ArrayList<>(columns);

        for (String token : tokens) {
            currentRow.add(this.createWidget(token));

            if (currentRow.size() >= columns) {
                this.rows.add(new Row(new ArrayList<>(currentRow), columns));
                currentRow.clear();
            }
        }

        if (!currentRow.isEmpty()) {
            while (currentRow.size() < columns) {
                currentRow.add(EmptyWidget.INSTANCE);
            }

            this.rows.add(new Row(new ArrayList<>(currentRow), columns));
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int rowIndex = this.getSlotIndexFromScreenCoords(mouseX, mouseY);

        if (rowIndex < 0 || rowIndex >= this.rows.size()) {
            return false;
        }

        Row row = this.rows.get(rowIndex);
        Layout layout = this.createLayout(row.columns);

        for (int cellIndex = 0; cellIndex < row.widgets.size(); cellIndex++) {
            int cellLeft = layout.left + cellIndex * (layout.cellWidth + CELL_GAP);

            if (mouseX >= cellLeft && mouseX <= cellLeft + layout.cellWidth) {
                boolean handled = row.widgets.get(cellIndex).mouseClicked(this.screen, cellLeft, mouseY, layout.cellWidth, this.slotHeight - 4, mouseX, mouseY, mouseButton);

                if (handled) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    public String getHoveredCommentTitle() {
        return this.hoveredCommentTitle;
    }

    public String getHoveredCommentBody() {
        return this.hoveredCommentBody;
    }

    @Override
    protected int getSize() {
        return this.rows.size();
    }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
    }

    @Override
    protected boolean isSelected(int slotIndex) {
        return false;
    }

    @Override
    protected void drawBackground() {
    }

    @Override
    protected void drawSlot(int index, int x, int y, int slotHeight, int mouseX, int mouseY, float partialTicks) {
        if (y < this.top || y + slotHeight > this.bottom) {
            return;
        }

        Row row = this.rows.get(index);
        Layout layout = this.createLayout(row.columns);

        if (index == 0) {
            this.hoveredCommentTitle = "";
            this.hoveredCommentBody = "";
        }

        for (int cellIndex = 0; cellIndex < row.widgets.size(); cellIndex++) {
            int cellLeft = layout.left + cellIndex * (layout.cellWidth + CELL_GAP);
            boolean hovered = mouseX >= cellLeft && mouseX <= cellLeft + layout.cellWidth && mouseY >= y && mouseY <= y + this.slotHeight;
            row.widgets.get(cellIndex).draw(this.screen, cellLeft, y + 2, layout.cellWidth, this.slotHeight - 4, mouseX, mouseY, hovered);

            if (hovered) {
                this.hoveredCommentTitle = row.widgets.get(cellIndex).getCommentTitle(this.screen);
                this.hoveredCommentBody = row.widgets.get(cellIndex).getCommentBody(this.screen);
            }
        }
    }

    @Override
    public int getListWidth() {
        return Math.min(420, this.width - 30);
    }

    @Override
    protected int getScrollBarX() {
        return (this.width + this.getListWidth()) / 2 - 6;
    }

    @Override
    protected void overlayBackground(int startY, int endY, int startAlpha, int endAlpha) {
        Gui.drawRect(0, startY, this.width, endY, 0xD0101010);
    }

    @Override
    protected void drawContainerBackground(Tessellator tessellator) {
        // Intentionally transparent in-world so shader changes remain visible behind the option list.
    }

    private CellWidget createWidget(String token) {
        if (token == null || token.isBlank() || "<empty>".equals(token)) {
            return EmptyWidget.INSTANCE;
        }

        if ("<profile>".equals(token)) {
            return new ProfileWidget(false);
        }

        if ("<profile2>".equals(token)) {
            return new ProfileWidget(true);
        }

        if (token.startsWith("[") && token.endsWith("]")) {
            return new LinkWidget(token.substring(1, token.length() - 1));
        }

        ActiniumShaderOption option = this.screen.getMenu().getOption(token);
        return option != null ? new OptionWidget(option) : new MissingWidget(token);
    }

    private Layout createLayout(int columns) {
        int width = this.getListWidth();
        int left = (this.width - width) / 2;
        int cellWidth = (width - ((columns - 1) * CELL_GAP)) / columns;
        return new Layout(left, cellWidth);
    }

    private record Layout(int left, int cellWidth) {
    }

    private record Row(List<CellWidget> widgets, int columns) {
    }

    private abstract static class CellWidget {
        protected void drawButton(int x, int y, int width, int height, boolean hovered) {
            int background = hovered ? 0xA0404040 : 0x90303030;
            Gui.drawRect(x, y, x + width, y + height, background);
            Gui.drawRect(x, y, x + width, y + 1, 0x40FFFFFF);
            Gui.drawRect(x, y + height - 1, x + width, y + height, 0x40000000);
        }

        protected String trim(Minecraft minecraft, String text, int width) {
            return minecraft.fontRenderer.getStringWidth(text) > width
                    ? minecraft.fontRenderer.trimStringToWidth(text, Math.max(1, width - 3)) + "..."
                    : text;
        }

        public abstract void draw(ActiniumShaderOptionScreen screen, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered);

        public boolean mouseClicked(ActiniumShaderOptionScreen screen, int x, int y, int width, int height, int mouseX, int mouseY, int button) {
            return false;
        }

        public String getCommentTitle(ActiniumShaderOptionScreen screen) {
            return "";
        }

        public String getCommentBody(ActiniumShaderOptionScreen screen) {
            return "";
        }
    }

    private static final class EmptyWidget extends CellWidget {
        private static final EmptyWidget INSTANCE = new EmptyWidget();

        @Override
        public void draw(ActiniumShaderOptionScreen screen, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
        }
    }

    private static final class MissingWidget extends CellWidget {
        private final String token;

        private MissingWidget(String token) {
            this.token = token;
        }

        @Override
        public void draw(ActiniumShaderOptionScreen screen, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            this.drawButton(x, y, width, height, hovered);
            Minecraft minecraft = Minecraft.getMinecraft();
            String label = this.trim(minecraft, this.token, width - 12);
            minecraft.fontRenderer.drawStringWithShadow(label, x + 6, y + 8, 0xFF9090);
        }

        @Override
        public String getCommentTitle(ActiniumShaderOptionScreen screen) {
            return this.token;
        }

        @Override
        public String getCommentBody(ActiniumShaderOptionScreen screen) {
            return I18n.format("options.actinium.shaderPack.options.missing");
        }
    }

    private static final class LinkWidget extends CellWidget {
        private final String targetScreenId;

        private LinkWidget(String targetScreenId) {
            this.targetScreenId = targetScreenId;
        }

        @Override
        public void draw(ActiniumShaderOptionScreen screen, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            this.drawButton(x, y, width, height, hovered);
            Minecraft minecraft = Minecraft.getMinecraft();
            String label = this.trim(minecraft, screen.getMenu().getScreenLabel(this.targetScreenId), width - 22);
            int labelWidth = minecraft.fontRenderer.getStringWidth(label);
            minecraft.fontRenderer.drawStringWithShadow(label, x + (width / 2) - (labelWidth / 2) - 4, y + 8, 0xFFFFFF);
            minecraft.fontRenderer.drawStringWithShadow(">", x + width - 12, y + 8, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(ActiniumShaderOptionScreen screen, int x, int y, int width, int height, int mouseX, int mouseY, int button) {
            if (button == 0 || button == 1) {
                screen.openSubScreen(this.targetScreenId);
                return true;
            }

            return false;
        }

        @Override
        public String getCommentTitle(ActiniumShaderOptionScreen screen) {
            return screen.getMenu().getScreenLabel(this.targetScreenId);
        }

        @Override
        public String getCommentBody(ActiniumShaderOptionScreen screen) {
            String comment = screen.getMenu().getScreenComment(this.targetScreenId);
            return comment != null ? comment : "";
        }
    }

    private static final class ProfileWidget extends CellWidget {
        private final boolean secondProfileSet;

        private ProfileWidget(boolean secondProfileSet) {
            this.secondProfileSet = secondProfileSet;
        }

        @Override
        public void draw(ActiniumShaderOptionScreen screen, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            this.drawButton(x, y, width, height, hovered);
            Minecraft minecraft = Minecraft.getMinecraft();
            String title = I18n.format(this.secondProfileSet ? "options.actinium.shaderPack.options.profile2" : "options.actinium.shaderPack.options.profile");
            ActiniumShaderProfileSet.ProfileResult result = screen.getProfileResult(this.secondProfileSet);
            String value = result.current() != null
                    ? screen.getMenu().getProfileLabel(result.current().name(), this.secondProfileSet)
                    : I18n.format("options.actinium.shaderPack.options.profileCustom");
            String trimmedTitle = this.trim(minecraft, title, width - 120);
            String trimmedValue = this.trim(minecraft, value, 110);
            minecraft.fontRenderer.drawStringWithShadow(trimmedTitle, x + 6, y + 8, 0xFFFFFF);
            minecraft.fontRenderer.drawStringWithShadow(trimmedValue, x + width - minecraft.fontRenderer.getStringWidth(trimmedValue) - 8, y + 8, 0xD0D0D0);
        }

        @Override
        public boolean mouseClicked(ActiniumShaderOptionScreen screen, int x, int y, int width, int height, int mouseX, int mouseY, int button) {
            ActiniumShaderProfileSet.ProfileResult result = screen.getProfileResult(this.secondProfileSet);
            ActiniumShaderProfile target = button == 1 ? result.previous() : result.next();

            if (target != null) {
                screen.applyProfile(target);
                return true;
            }

            return false;
        }

        @Override
        public String getCommentTitle(ActiniumShaderOptionScreen screen) {
            return I18n.format(this.secondProfileSet ? "options.actinium.shaderPack.options.profile2" : "options.actinium.shaderPack.options.profile");
        }

        @Override
        public String getCommentBody(ActiniumShaderOptionScreen screen) {
            String comment = screen.getMenu().getProfileComment(this.secondProfileSet);
            return comment != null ? comment : "";
        }
    }

    private static final class OptionWidget extends CellWidget {
        private final ActiniumShaderOption option;

        private OptionWidget(ActiniumShaderOption option) {
            this.option = option;
        }

        @Override
        public void draw(ActiniumShaderOptionScreen screen, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            this.drawButton(x, y, width, height, hovered);
            Minecraft minecraft = Minecraft.getMinecraft();
            String label = screen.getMenu().getOptionLabel(this.option);

            if (screen.isOptionModified(this.option)) {
                label = label + " *";
            }

            String currentValue = screen.getCurrentValue(this.option);
            String valueLabel;

            if (this.option.isBooleanOption()) {
                valueLabel = I18n.format(Boolean.parseBoolean(currentValue)
                        ? "options.actinium.shaderPack.options.on"
                        : "options.actinium.shaderPack.options.off");
            } else {
                valueLabel = screen.getMenu().getValueLabel(this.option, currentValue);
            }

            int valueWidth = Math.min(116, width / 2);
            String trimmedLabel = this.trim(minecraft, label, width - valueWidth - 18);
            String trimmedValue = this.trim(minecraft, valueLabel, valueWidth);

            minecraft.fontRenderer.drawStringWithShadow(trimmedLabel, x + 6, y + 8, 0xFFFFFF);
            minecraft.fontRenderer.drawStringWithShadow(trimmedValue, x + width - minecraft.fontRenderer.getStringWidth(trimmedValue) - 8, y + 8, this.option.isBooleanOption() && !Boolean.parseBoolean(currentValue) ? 0xA0A0A0 : 0xD0D0D0);

            if (this.option.isSlider() && !this.option.isBooleanOption() && this.option.getAllowedValues().size() > 1) {
                int sliderWidth = valueWidth - 12;
                int sliderLeft = x + width - valueWidth + 4;
                int sliderTop = y + height - 6;
                Gui.drawRect(sliderLeft, sliderTop, sliderLeft + sliderWidth, sliderTop + 2, 0x40303030);
                int index = Math.max(0, this.option.getAllowedValues().indexOf(currentValue));
                int knobX = sliderLeft + Math.round(((float) index / (float) (this.option.getAllowedValues().size() - 1)) * (sliderWidth - 4));
                Gui.drawRect(knobX, sliderTop - 1, knobX + 4, sliderTop + 3, 0xC0F2C94C);
            }
        }

        @Override
        public boolean mouseClicked(ActiniumShaderOptionScreen screen, int x, int y, int width, int height, int mouseX, int mouseY, int button) {
            if (GuiScreen.isShiftKeyDown()) {
                screen.resetOption(this.option);
                return true;
            }

            if (this.option.isSlider() && !this.option.isBooleanOption() && button == 0) {
                int valueWidth = Math.min(116, width / 2);
                int sliderLeft = x + width - valueWidth + 4;
                int sliderWidth = valueWidth - 12;
                screen.setSliderValue(this.option, mouseX, sliderLeft, sliderWidth);
                return true;
            }

            if (button == 0 || button == 1) {
                screen.cycleOption(this.option, button == 1 ? -1 : 1);
                return true;
            }

            return false;
        }

        @Override
        public String getCommentTitle(ActiniumShaderOptionScreen screen) {
            return screen.getMenu().getOptionLabel(this.option);
        }

        @Override
        public String getCommentBody(ActiniumShaderOptionScreen screen) {
            String comment = screen.getMenu().getOptionComment(this.option, screen.getCurrentScreenId());
            return comment != null ? comment : "";
        }
    }
}
