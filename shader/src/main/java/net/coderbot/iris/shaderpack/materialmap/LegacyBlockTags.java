package net.coderbot.iris.shaderpack.materialmap;

import net.coderbot.iris.Iris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * Resolves shaderpack block tags against blocks available in Minecraft 1.12.2.
 */
public final class LegacyBlockTags {
	private static final List<String> SLABS = List.of(
		"oak_slab", "spruce_slab", "birch_slab", "jungle_slab", "acacia_slab", "dark_oak_slab",
		"stone_slab", "sandstone_slab", "petrified_oak_slab", "cobblestone_slab", "brick_slab",
		"stone_brick_slab", "nether_brick_slab", "quartz_slab", "red_sandstone_slab", "purpur_slab"
	);

	private static final List<String> DOORS = List.of(
		"oak_door", "iron_door", "spruce_door", "birch_door", "jungle_door", "acacia_door", "dark_oak_door"
	);

	private static final List<String> TRAPDOORS = List.of(
		"oak_trapdoor", "spruce_trapdoor", "birch_trapdoor", "jungle_trapdoor",
		"acacia_trapdoor", "dark_oak_trapdoor", "iron_trapdoor"
	);

	private static final List<String> STAIRS = List.of(
		"oak_stairs", "cobblestone_stairs", "brick_stairs", "stone_brick_stairs", "nether_brick_stairs",
		"sandstone_stairs", "spruce_stairs", "birch_stairs", "jungle_stairs", "quartz_stairs",
		"acacia_stairs", "dark_oak_stairs", "red_sandstone_stairs", "purpur_stairs"
	);

	private LegacyBlockTags() {
	}

	public static List<BlockEntry> resolve(TagEntry tag) {
		NamespacedId id = tag.id();
		if (!"minecraft".equals(id.getNamespace())) {
			return unsupported(id);
		}

		return switch (id.getName()) {
			case "small_flowers" -> List.of(block("yellow_flower"), block("red_flower"));
			case "flowers" -> List.of(
				block("yellow_flower"),
				block("red_flower"),
				block("double_plant", Set.of(0, 1, 4, 5, 8, 9, 12, 13))
			);
			case "saplings" -> List.of(block("sapling"));
			case "leaves" -> List.of(block("leaves"), block("leaves2"));
			case "slabs" -> modernEntries(SLABS, tag.propertyPredicates());
			case "doors" -> modernEntries(DOORS, tag.propertyPredicates());
			case "trapdoors" -> modernEntries(TRAPDOORS, tag.propertyPredicates());
			case "stairs" -> modernEntries(STAIRS, tag.propertyPredicates());
			default -> unsupported(id);
		};
	}

	private static List<BlockEntry> modernEntries(List<String> names, Map<String, String> predicates) {
		List<BlockEntry> result = new ArrayList<>();
		for (String name : names) {
			List<BlockEntry> legacy = FlatteningMap.toLegacy(name, predicates);
			if (legacy != null && !legacy.isEmpty()) {
				result.addAll(legacy);
			} else {
				result.add(new BlockEntry(new NamespacedId("minecraft", name), Collections.emptySet(), predicates, Collections.emptyMap()));
			}
		}
		return result;
	}

	private static List<BlockEntry> unsupported(NamespacedId id) {
		Iris.logger.debug("Legacy block tag {} is not supported on Minecraft 1.12.2", id);
		return Collections.emptyList();
	}

	private static BlockEntry block(String name) {
		return new BlockEntry(new NamespacedId("minecraft", name), Collections.emptySet());
	}

	private static BlockEntry block(String name, Set<Integer> metas) {
		return new BlockEntry(new NamespacedId("minecraft", name), metas);
	}
}
