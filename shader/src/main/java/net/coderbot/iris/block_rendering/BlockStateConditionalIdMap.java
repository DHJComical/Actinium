package net.coderbot.iris.block_rendering;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves shader block IDs from runtime-visible blockstate properties.
 */
public class BlockStateConditionalIdMap {
	private record StateCondition(Set<Integer> metas, Map<String, String> stateProperties, int shaderId) {
	}

	private final Reference2ObjectMap<Block, List<StateCondition>> conditionsByBlock = new Reference2ObjectOpenHashMap<>();

	public void addCondition(Block block, Set<Integer> metas, Map<String, String> stateProperties, int shaderId) {
		if (stateProperties.isEmpty()) {
			return;
		}

		Set<Integer> conditionMetas = metas.isEmpty() ? Collections.emptySet() : Set.copyOf(metas);
		Map<String, String> conditionStateProperties = Map.copyOf(stateProperties);
		conditionsByBlock.computeIfAbsent(block, ignored -> new ArrayList<>())
			.add(new StateCondition(conditionMetas, conditionStateProperties, shaderId));
	}

	public boolean isEmpty() {
		return conditionsByBlock.isEmpty();
	}

	public int resolve(Block block, int metadata, IBlockState state) {
		List<StateCondition> conditions = conditionsByBlock.get(block);
		if (conditions == null) {
			return -1;
		}

		for (StateCondition condition : conditions) {
			if (!condition.metas().isEmpty() && !condition.metas().contains(metadata)) {
				continue;
			}

			if (matchesStateProperties(state, condition.stateProperties())) {
				return condition.shaderId();
			}
		}

		return -1;
	}

	private static boolean matchesStateProperties(IBlockState state, Map<String, String> stateProperties) {
		Map<IProperty<?>, Comparable<?>> properties = state.getProperties();

		for (Map.Entry<String, String> required : stateProperties.entrySet()) {
			IProperty<?> property = findProperty(properties, required.getKey());
			if (property == null) {
				return false;
			}

			Comparable<?> value = properties.get(property);
			if (value == null || !required.getValue().equalsIgnoreCase(getPropertyValueName(property, value))) {
				return false;
			}
		}

		return true;
	}

	private static IProperty<?> findProperty(Map<IProperty<?>, Comparable<?>> properties, String propertyName) {
		for (IProperty<?> property : properties.keySet()) {
			if (property.getName().equalsIgnoreCase(propertyName)) {
				return property;
			}
		}

		return null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static String getPropertyValueName(IProperty property, Comparable value) {
		return property.getName(value);
	}
}
