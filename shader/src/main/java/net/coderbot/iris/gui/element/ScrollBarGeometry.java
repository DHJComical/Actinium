package net.coderbot.iris.gui.element;

/**
 * Pure math for the shader list scrollbar drag scaling.
 *
 * <p>Motivation: dragging the handle must map "handle travels the whole track" to
 * "content scrolls from top to bottom", matching vanilla {@code GuiSlot}'s scroll
 * multiplier. Without this factor both the handle and the content move 1:1 with the
 * cursor, so on long lists the handle lags far behind the mouse and the bottom of the
 * list cannot be reached within one screen height of dragging.</p>
 */
final class ScrollBarGeometry {
	/** Minimum rendered handle height, matching vanilla GuiSlot's clamp. */
	private static final int MIN_HANDLE_HEIGHT = 32;

	private ScrollBarGeometry() {
	}

	/**
	 * Content pixels scrolled per cursor pixel while dragging the handle, so the handle
	 * crosses its full track exactly while the content crosses its full scroll range.
	 *
	 * @param contentHeight total list content height in GUI pixels
	 * @param viewportHeight visible list height (bottom - top) in GUI pixels
	 * @param headerPadding top padding that entries cannot scroll into
	 * @return the drag scale factor
	 */
	static float dragFactor(int contentHeight, int viewportHeight, int headerPadding) {
		int scrollable = contentHeight - (viewportHeight - headerPadding);
		if (scrollable < 1) {
			scrollable = 1;
		}

		// Handle height shrinks as the list grows, clamped like vanilla GuiSlot draws it
		final int handleHeight = Math.min(
			Math.max((int) ((float) viewportHeight * viewportHeight / contentHeight), MIN_HANDLE_HEIGHT),
			viewportHeight - 8);

		return (float) scrollable / (viewportHeight - handleHeight);
	}
}
