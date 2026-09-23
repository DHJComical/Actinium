package net.coderbot.iris.shaderpack;

import com.gtnewhorizons.angelica.glsm.states.BlendState;
import net.coderbot.iris.gl.blending.BlendModeFunction;
import net.coderbot.iris.gl.blending.BufferBlendInformation;

/**
 * A parsed per-buffer blend directive ({@code blend.<program>.<buffer> = <value>}) from shaders.properties.
 *
 * <p>The regular shaders.properties evaluation and {@link IrisEraBufferBlendAdopter} both turn these directives into
 * {@link BufferBlendInformation}; parsing them in one place guarantees that an adopted directive means exactly what it
 * would have meant had the environment evaluation found it.
 *
 * @param key         the full properties key, e.g. {@code blend.gbuffers_water.colortex3}; kept for diagnostics
 * @param value       the raw properties value, {@code off} or four {@link BlendModeFunction} names; kept for diagnostics
 * @param program     the program the directive applies to, e.g. {@code gbuffers_water}
 * @param information the color attachment index and its blend state ({@code null} for {@code off}), as consumed by
 *                    the render pipelines
 */
record BufferBlendDirective(String key, String value, String program, BufferBlendInformation information) {
	private static final String PREFIX = "blend.";

	/**
	 * Returns whether {@code key} names a per-buffer blend directive ({@code blend.<program>.<buffer>}) rather than a
	 * program-wide one ({@code blend.<program>}).
	 */
	static boolean isPerBufferKey(String key) {
		return key.startsWith(PREFIX) && key.indexOf('.', PREFIX.length()) != -1;
	}

	/**
	 * Parses a per-buffer blend directive. The buffer is {@code colortexN} or a legacy name ({@code gcolor} through
	 * {@code gaux4}); the value is {@code off} or {@code <srcRGB> <dstRGB> <srcAlpha> <dstAlpha>}. Malformed directives
	 * fail exactly like upstream Iris: a buffer that does not resolve to an index throws {@code RuntimeException}, an
	 * unknown blend function name throws {@code NoSuchElementException}.
	 */
	static BufferBlendDirective parse(String key, String value) {
		if (!isPerBufferKey(key)) {
			throw new IllegalArgumentException("Not a per-buffer blend directive: " + key);
		}

		final String[] parts = key.substring(PREFIX.length()).split("\\.");
		int index = PackRenderTargetDirectives.LEGACY_RENDER_TARGETS.indexOf(parts[1]);

		if (index == -1 && parts[1].startsWith("colortex")) {
			final String id = parts[1].substring("colortex".length());

			try {
				index = Integer.parseInt(id);
			} catch (NumberFormatException e) {
				throw new RuntimeException("Failed to parse buffer blend!", e);
			}
		}

		if (index == -1) {
			throw new RuntimeException("Failed to parse buffer blend! index = " + index);
		}

		if ("off".equals(value)) {
			return new BufferBlendDirective(key, value, parts[0], new BufferBlendInformation(index, null));
		}

		final String[] modeArray = value.split(" ");
		final int[] modes = new int[modeArray.length];

		int i = 0;
		for (String modeName : modeArray) {
			modes[i] = BlendModeFunction.fromString(modeName).get().getGlId();
			i++;
		}

		return new BufferBlendDirective(key, value, parts[0],
			new BufferBlendInformation(index, new BlendState(modes[0], modes[1], modes[2], modes[3])));
	}

	/**
	 * The color attachment index the directive applies to, after resolving legacy buffer names.
	 */
	int index() {
		return information.getIndex();
	}
}
