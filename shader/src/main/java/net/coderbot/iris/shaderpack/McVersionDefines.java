package net.coderbot.iris.shaderpack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Reads and rewrites the {@code MC_VERSION} environment define.
 *
 * <p>Actinium reports the real Minecraft version (1.12.2, {@code MC_VERSION 11202}) to shader packs, but some
 * properties files only carry their modern content behind newer {@code MC_VERSION} conditionals. Re-evaluating such a
 * file as another version needs the same "substitute MC_VERSION, keep every other define" rewrite, which lives here:
 * {@link IdMap} evaluates ID maps without a 1.12 section as a modern version, and {@link IrisEraBufferBlendAdopter}
 * evaluates shaders.properties as an Iris-era version to recover gated per-buffer blend directives.
 */
final class McVersionDefines {
	static final String MC_VERSION = "MC_VERSION";

	private McVersionDefines() {
	}

	/**
	 * Returns a copy of {@code defines} that evaluates as {@code mcVersion}: every {@code MC_VERSION} define is dropped,
	 * the remaining defines keep their order, and a single {@code MC_VERSION} define is appended.
	 */
	static List<StringPair> withMcVersion(Iterable<StringPair> defines, int mcVersion) {
		final List<StringPair> rewritten = new ArrayList<>();

		for (StringPair define : defines) {
			if (!MC_VERSION.equals(define.getKey())) {
				rewritten.add(define);
			}
		}

		rewritten.add(new StringPair(MC_VERSION, Integer.toString(mcVersion)));

		return rewritten;
	}

	/**
	 * Returns the {@code MC_VERSION} that {@code defines} evaluate as. The last definition wins, matching the
	 * preprocessor, which replaces a macro that is added again. Empty when {@code MC_VERSION} is not defined.
	 *
	 * @throws IllegalStateException if the effective value is not an integer
	 */
	static OptionalInt mcVersionOf(Iterable<StringPair> defines) {
		String value = null;

		for (StringPair define : defines) {
			if (MC_VERSION.equals(define.getKey())) {
				value = define.getValue();
			}
		}

		if (value == null) {
			return OptionalInt.empty();
		}

		try {
			return OptionalInt.of(Integer.parseInt(value));
		} catch (NumberFormatException e) {
			throw new IllegalStateException("MC_VERSION define is not an integer: '" + value + "'", e);
		}
	}
}
