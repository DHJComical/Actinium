package net.coderbot.iris.shaderpack;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites the {@code MC_VERSION} environment define.
 *
 * <p>Actinium reports the real Minecraft version (1.12.2, {@code MC_VERSION 11202}) to shader packs, but some
 * properties files only carry their modern content behind newer {@code MC_VERSION} conditionals. Re-evaluating such a
 * file as another version needs the same "substitute MC_VERSION, keep every other define" rewrite, which lives here;
 * {@link IdMap} uses it to evaluate ID maps without a 1.12 section as a modern version.
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
}
