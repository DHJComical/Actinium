package net.coderbot.iris.pipeline;

/**
 * Provides the effective render distance used for sky-related rendering.
 *
 * <p>Vanilla 1.12.2 only draws the sky at four or more chunks, and its sky geometry derives far planes from that
 * threshold. Keeping the same minimum here prevents low render distances from shrinking the skybox, horizon, clouds,
 * and celestial objects into clipped or masked shapes.</p>
 */
public final class SkyRenderDistance {
	/**
	 * The minimum render distance required for vanilla sky, cloud, and celestial rendering.
	 */
	public static final int MINIMUM_RENDER_DISTANCE_CHUNKS = 4;

	private SkyRenderDistance() {
	}

	/**
	 * Returns the configured render distance, clamped to the minimum supported by vanilla sky rendering.
	 */
	public static int effectiveChunks(int renderDistanceChunks) {
		return Math.max(renderDistanceChunks, MINIMUM_RENDER_DISTANCE_CHUNKS);
	}

	/**
	 * Returns the effective render distance in blocks, matching the normal far plane scale of 16 blocks per chunk.
	 */
	public static int effectiveBlocks(int renderDistanceChunks) {
		return effectiveChunks(renderDistanceChunks) * 16;
	}
}
