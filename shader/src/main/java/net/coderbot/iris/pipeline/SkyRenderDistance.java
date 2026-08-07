package net.coderbot.iris.pipeline;

/**
 * Provides the effective render distance used for sky-related rendering.
 *
 * <p>Low render distances shrink the projection and fog ranges that drive 1.12.2's sky, cloud, and celestial geometry.
 * Keeping the sky-facing distance above the vanilla cloud threshold prevents the skybox, horizon, clouds, and celestial
 * objects from being clipped or masked.</p>
 */
public final class SkyRenderDistance {
	/**
	 * The minimum render distance used for stable sky, cloud, and celestial rendering.
	 */
	public static final int MINIMUM_RENDER_DISTANCE_CHUNKS = 8;

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
