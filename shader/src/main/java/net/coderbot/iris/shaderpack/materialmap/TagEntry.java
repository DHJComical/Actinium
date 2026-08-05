package net.coderbot.iris.shaderpack.materialmap;

import net.coderbot.iris.Iris;

import java.util.HashMap;
import java.util.Map;

/**
 * Parsed block tag reference from block.properties.
 */
public record TagEntry(NamespacedId id, Map<String, String> propertyPredicates) {
	public static TagEntry parse(String entry) {
		if (entry.isEmpty()) {
			throw new IllegalArgumentException("Called TagEntry::parse with an empty string");
		}

		String tag = entry.startsWith("%") ? entry.substring(1) : entry;
		String[] splitStates = tag.split(":");

		if (splitStates.length == 1) {
			return new TagEntry(new NamespacedId("minecraft", tag), Map.of());
		}

		if (splitStates.length == 2 && !splitStates[1].contains("=")) {
			return new TagEntry(new NamespacedId(splitStates[0], splitStates[1]), Map.of());
		}

		int statesStart;
		NamespacedId id;
		if (splitStates[1].contains("=")) {
			statesStart = 1;
			id = new NamespacedId("minecraft", splitStates[0]);
		} else {
			statesStart = 2;
			id = new NamespacedId(splitStates[0], splitStates[1]);
		}

		Map<String, String> properties = new HashMap<>();
		for (int index = statesStart; index < splitStates.length; index++) {
			String[] propertyParts = splitStates[index].split("=");
			if (propertyParts.length != 2) {
				Iris.logger.warn("Warning: the block ID map entry \"{}\" could not be fully parsed:", entry);
				Iris.logger.warn("- Block state property filters must be of the form \"key=value\", but {} is not of that form!", splitStates[index]);
				continue;
			}
			properties.put(propertyParts[0], propertyParts[1]);
		}

		return new TagEntry(id, properties);
	}
}
