package net.caffeinemc.mods.sodium.client.gui;

/** Shared dimensions for the Sodium settings surface. */
public final class Layout {
    public static final int BUTTON_SHORT = 20;
    public static final int BUTTON_LONG = 65;
    public static final int INNER_MARGIN = 5;
    public static final int OPTION_GROUP_MARGIN = 3;
    public static final int OPTION_PAGE_MARGIN = 6;
    public static final int OPTION_MOD_MARGIN = 12;
    public static final int OPTION_LEFT_INSET = 3;
    public static final int SCROLLBAR_WIDTH = 7;
    public static final int OPTION_LIST_SCROLLBAR_OFFSET = 5;
    public static final int PAGE_LIST_WIDTH = 125;
    public static final int OPTION_WIDTH = 210;
    public static final int MIN_TOOLTIP_WIDTH = 100;
    public static final int MAX_TOOLTIP_WIDTH = 200;
    public static final int TOOLTIP_OUTER_MARGIN = 3;
    public static final int CONTENT_BORDER_MIN_WIDTH = 100;
    public static final int CONTENT_BORDER_HEIGHT = 12;
    public static final int CONTENT_MIN_HEIGHT = 300;
    public static final int ENTRY_HEIGHT = 18;
    public static final int GROUP_HEADER_HEIGHT = ENTRY_HEIGHT;
    public static final int TEXT_LINE_SPACING = 2;
    public static final int TEXT_LEFT_PADDING = 8;
    public static final int REGULAR_TEXT_BASELINE_OFFSET = -4;
    public static final int ICON_MARGIN = 4;
    public static final int ICON_TEXT_BASELINE_OFFSET = -3;
    public static final int CONTROL_ICON_SIZE = 10;
    public static final int PAGE_ENTRY_SELECTION_BAR_WIDTH = 3;
    public static final int PAGE_ENTRY_LABEL_END_PADDING = 14;
    public static final int SECTION_FOCUS_LEAD_ROWS = 3;

    private Layout() {
    }

    public static ScreenLayout calculate(int screenWidth, int screenHeight) {
        if (screenWidth < 160 || screenHeight < 120) {
            throw new IllegalArgumentException("Sodium settings require at least a 160x120 GUI viewport");
        }
        int base = PAGE_LIST_WIDTH + INNER_MARGIN + OPTION_WIDTH + OPTION_LIST_SCROLLBAR_OFFSET
                + SCROLLBAR_WIDTH + TOOLTIP_OUTER_MARGIN;
        int minWidth = base + (MAX_TOOLTIP_WIDTH - MIN_TOOLTIP_WIDTH) / 2 + MIN_TOOLTIP_WIDTH;
        int maxWidth = base + MAX_TOOLTIP_WIDTH;
        int widthStart = minWidth + CONTENT_BORDER_MIN_WIDTH;
        int widthEnd = maxWidth + CONTENT_BORDER_MIN_WIDTH;
        boolean insetX = screenWidth > widthStart;
        int contentWidth = screenWidth;
        if (insetX) {
            contentWidth = screenWidth < widthEnd
                    ? minWidth + (int) ((screenWidth - widthStart) * (maxWidth - minWidth) / (float) (widthEnd - widthStart))
                    : maxWidth;
        }
        boolean insetY = insetX && screenHeight > CONTENT_MIN_HEIGHT + CONTENT_BORDER_HEIGHT;
        int contentHeight = insetY ? screenHeight - CONTENT_BORDER_HEIGHT : screenHeight;
        int contentX = (screenWidth - contentWidth) / 2;
        int contentY = (screenHeight - contentHeight) / 2;
        GuiRect content = new GuiRect(contentX, contentY, contentWidth, contentHeight);
        GuiRect search = new GuiRect(contentX, contentY, contentWidth, BUTTON_SHORT);
        int topClear = BUTTON_SHORT + (insetY ? INNER_MARGIN : 0);
        GuiRect pages = new GuiRect(contentX, contentY + topClear, PAGE_LIST_WIDTH, contentHeight - topClear);
        int minWidthToStack = PAGE_LIST_WIDTH + INNER_MARGIN * 2 + OPTION_WIDTH
                + OPTION_LIST_SCROLLBAR_OFFSET + SCROLLBAR_WIDTH + BUTTON_LONG;
        int maxWidthToStack = minWidthToStack + BUTTON_LONG * 2 + INNER_MARGIN;
        boolean stack = contentWidth > minWidthToStack && contentWidth < maxWidthToStack;
        boolean reserve = contentWidth < minWidthToStack;
        int optionY = contentY + BUTTON_SHORT + INNER_MARGIN;
        int footerTop = content.bottom() - INNER_MARGIN - BUTTON_SHORT;
        int optionHeight = Math.max(0, footerTop - optionY - INNER_MARGIN);
        int optionX = contentX + PAGE_LIST_WIDTH + INNER_MARGIN;
        int optionRight = content.right() - INNER_MARGIN;
        if (stack) {
            int closeX = content.right() - BUTTON_LONG - (insetX ? 0 : INNER_MARGIN);
            optionRight = Math.min(optionRight, closeX - INNER_MARGIN);
        }
        int optionWidth = Math.max(0, optionRight - optionX);
        GuiRect options = new GuiRect(optionX, optionY, optionWidth, optionHeight);
        GuiRect tooltip = options;
        int closeX = content.right() - BUTTON_LONG - (insetX ? 0 : INNER_MARGIN);
        int closeY = content.bottom() - (insetY ? 0 : INNER_MARGIN) - BUTTON_SHORT;
        GuiRect done = new GuiRect(closeX, closeY, BUTTON_LONG, BUTTON_SHORT);
        int dx = stack ? 0 : -(INNER_MARGIN + BUTTON_LONG);
        int dy = stack ? -(INNER_MARGIN + BUTTON_SHORT) : 0;
        int actionX = closeX + dx;
        int actionY = stack ? closeY + dy : content.bottom() - INNER_MARGIN - BUTTON_SHORT;
        GuiRect apply = new GuiRect(actionX, actionY, BUTTON_LONG, BUTTON_SHORT);
        GuiRect undo = new GuiRect(actionX + dx, actionY + dy, BUTTON_LONG, BUTTON_SHORT);
        return new ScreenLayout(content, search, pages, options, tooltip, true,
                reserve, stack, new ActionButtons(undo, apply, done));
    }

    public record ScreenLayout(GuiRect content, GuiRect search, GuiRect pages, GuiRect options, GuiRect tooltip,
                                boolean tooltipOverlay, boolean reserveBottomSpace, boolean stackActionButtons,
                                ActionButtons actionButtons) {
    }

    public record ActionButtons(GuiRect undo, GuiRect apply, GuiRect done) {
    }
}
