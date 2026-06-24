package net.coderbot.iris.shaderpack.materialmap;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
public class BlockEntry {
	private final NamespacedId id;
	private final Set<Integer> metas;
	private final Map<String, String> stateProperties;
	private final Map<String, PropertiesTokenizer.NbtValue> nbtProperties;

	public BlockEntry(NamespacedId id, Set<Integer> metas) {
		this(id, metas, Collections.emptyMap(), Collections.emptyMap());
	}

	public BlockEntry(NamespacedId id, Set<Integer> metas, Map<String, String> stateProperties) {
		this(id, metas, stateProperties, Collections.emptyMap());
	}

	public BlockEntry(NamespacedId id, Set<Integer> metas, Map<String, String> stateProperties,
	                  Map<String, PropertiesTokenizer.NbtValue> nbtProperties) {
		this.id = id;
		this.metas = metas;
		this.stateProperties = stateProperties;
		this.nbtProperties = nbtProperties;
	}

	public boolean hasStateProperties() {
		return !stateProperties.isEmpty();
	}

	public boolean hasNbtProperties() {
		return !nbtProperties.isEmpty();
	}

	/**
	 * Parses a block ID entry. All tokenization is delegated to {@link PropertiesTokenizer}.
	 *
	 * @param entry The string representation of the entry. Must not be empty.
	 */
	@NotNull
	public static BlockEntry parse(@NotNull String entry) {
		if (entry.isEmpty()) {
			throw new IllegalArgumentException("Called BlockEntry::parse with an empty string");
		}

		PropertiesTokenizer.ParsedBlockIdentifier parsed = PropertiesTokenizer.parseBlockIdentifier(entry);
		return new BlockEntry(parsed.id(), parsed.metas(), parsed.stateProperties(), parsed.nbtProperties());
	}

    @Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final BlockEntry that = (BlockEntry) o;
		return Objects.equals(id, that.id)
			&& Objects.equals(metas, that.metas)
			&& Objects.equals(stateProperties, that.stateProperties)
			&& Objects.equals(nbtProperties, that.nbtProperties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, metas, stateProperties, nbtProperties);
	}
}
