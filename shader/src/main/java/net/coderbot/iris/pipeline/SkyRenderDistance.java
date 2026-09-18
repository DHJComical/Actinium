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

	/**
	 * Returns the render boundary in blocks reported to shader packs through the {@code far} uniform.
	 *
	 * <p>Shader packs use {@code far} to place the transition between the vanilla terrain and the Distant Horizons LODs,
	 * so it has to describe the terrain that is actually rendered. Celeritas only publishes a chunk as ready once the
	 * chunk rings configured on its neighbor gate are loaded, and the chunks beyond the configured render distance are
	 * never loaded, so the outermost rings never render and are removed here.</p>
	 *
	 * <p>The sky rendering floor deliberately does not apply: reporting more terrain than exists would place the
	 * transition beyond the rendered terrain and leave a gap between the vanilla terrain and the LODs. The floor
	 * belongs to the sky rendering paths, which apply it through {@link #effectiveChunks(int)}.</p>
	 *
	 * @param renderDistanceChunks the configured render distance in chunks
	 * @param unrenderedOuterRings the number of outermost chunk rings that are loaded but never rendered (0 when chunks
	 *                             render as soon as they load)
	 * @return the boundary in blocks, at least one chunk
	 */
	public static int farBlocks(int renderDistanceChunks, int unrenderedOuterRings) {
		if (unrenderedOuterRings < 0) {
			throw new IllegalArgumentException("unrenderedOuterRings must be nonnegative, was " + unrenderedOuterRings);
		}

		return Math.max(1, renderDistanceChunks - unrenderedOuterRings) * 16;
	}
}
