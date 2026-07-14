package net.coderbot.iris.shaderpack;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Identifies the vanilla dimensions using the namespaced keys expected by modern shader packs.
 */
public enum DimensionId {
	OVERWORLD(0, "minecraft:overworld", "overworld"),
	NETHER(-1, "minecraft:the_nether", "nether", "the_nether", "minecraft:nether"),
	END(1, "minecraft:the_end", "the end", "the_end", "end", "minecraft:end");

	private static final String LEGACY_DIMENSION_PREFIX = "legacy:dimension_";
	private final int numericId;
	private final String canonicalId;
	private final Set<String> aliases;

	DimensionId(int numericId, String canonicalId, String... aliases) {
		this.numericId = numericId;
		this.canonicalId = canonicalId;
		this.aliases = Arrays.stream(aliases)
			.map(alias -> alias.toLowerCase(Locale.ROOT))
			.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Converts known vanilla aliases to a stable namespaced key while preserving custom dimension names.
	 *
	 * @param dimensionName the provider name or shader-pack dimension identifier
	 * @return the canonical vanilla key, or the original custom dimension name
	 */
	public static String canonicalize(String dimensionName) {
		Objects.requireNonNull(dimensionName, "dimensionName");
		String normalizedName = dimensionName.toLowerCase(Locale.ROOT);

		for (DimensionId dimension : values()) {
			if (dimension.canonicalId.equals(normalizedName) || dimension.aliases.contains(normalizedName)) {
				return dimension.canonicalId;
			}
		}

		return dimensionName;
	}

	/**
	 * Normalizes a provider name, using the numeric ID only when the provider does not supply one.
	 *
	 * @param dimensionName the provider name, which may be null or blank
	 * @param numericId the legacy numeric dimension ID
	 * @return the canonical provider key or a stable key derived from the numeric ID
	 */
	public static String canonicalize(String dimensionName, int numericId) {
		if (dimensionName != null && !dimensionName.isBlank()) {
			return canonicalize(dimensionName);
		}

		for (DimensionId dimension : values()) {
			if (dimension.numericId == numericId) {
				return dimension.canonicalId;
			}
		}

		return LEGACY_DIMENSION_PREFIX + numericId;
	}

	/**
	 * Returns this vanilla dimension's stable shader-pack key.
	 */
	public String getCanonicalId() {
		return canonicalId;
	}

	/**
	 * Returns this vanilla dimension's fixed legacy numeric ID.
	 */
	public int getNumericId() {
		return numericId;
	}
}
