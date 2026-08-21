package net.coderbot.iris.gui.element.widget;

/**
 * Pure geometry shared by the shader option slider's rendering and dragging.
 *
 * <p>Motivation: the drag mapping must be the exact inverse of the render mapping,
 * otherwise the handle visibly lags behind or jumps ahead of the cursor while dragging.
 * Keeping both formulas in one dependency-free class lets that invariant be verified
 * by unit tests without loading Minecraft client classes.</p>
 */
final class SliderGeometry {
	/** Left inset of the slider track inside the widget, matching the widget texture border. */
	static final int TRACK_INSET = 4;

	/** Width of the draggable handle in its active (hovered) state. */
	static final int ACTIVE_SLIDER_WIDTH = 6;

	private SliderGeometry() {
	}

	/**
	 * X coordinate of the handle's left edge for a value index, mirroring the formula
	 * used to draw the handle so dragging can invert it exactly.
	 *
	 * @param x left edge of the widget
	 * @param width width of the widget
	 * @param index selected value index
	 * @param valueCount number of allowed values (must be at least 2)
	 */
	static int sliderXForIndex(int x, int width, int index, int valueCount) {
		// Range of x values the handle can occupy
		final int sliderSpace = (width - 8) - ACTIVE_SLIDER_WIDTH;

		return (x + TRACK_INSET) + (int) (((float) index / (valueCount - 1)) * sliderSpace);
	}

	/**
	 * Value index selected by a cursor x position: the nearest value to the cursor,
	 * using the same track geometry as {@link #sliderXForIndex}. Rounding (instead of
	 * the previous floor-onto-a-wider-range formula) keeps the handle centered under
	 * the cursor at both endpoints instead of lagging behind it.
	 *
	 * @param mouseX cursor x position
	 * @param x left edge of the widget
	 * @param width width of the widget
	 * @param valueCount number of allowed values
	 */
	static int valueIndexForMouseX(int mouseX, int x, int width, int valueCount) {
		final float mousePositionAcrossWidget = clamp((float) (mouseX - (x + TRACK_INSET)) / (width - 8), 0.0F, 1.0F);

		return Math.round(mousePositionAcrossWidget * (valueCount - 1));
	}

	private static float clamp(float value, float min, float max) {
		return value < min ? min : Math.min(value, max);
	}
}
