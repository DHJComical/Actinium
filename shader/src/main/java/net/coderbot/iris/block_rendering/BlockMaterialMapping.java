package net.coderbot.iris.block_rendering;

import org.embeddedt.embeddium.api.shader.BlockRenderLayer;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.coderbot.iris.shaderpack.materialmap.BlockEntry;
import net.coderbot.iris.shaderpack.materialmap.BlockRenderType;
import net.coderbot.iris.shaderpack.materialmap.FlatteningMap;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockMaterialMapping {
	public static final int SNOWY_META_BIT = 1 << 16;

	/**
	 * Creates a two-level map structure for block material IDs.
	 * Based on Iris's BlockState mapping approach adapted for 1.7.10's metadata system.
	 */
	public static Reference2ObjectMap<Block, Int2IntMap> createBlockMetaIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap) {
		Reference2ObjectMap<Block, Int2IntMap> blockMatches = new Reference2ObjectOpenHashMap<>();
		ReferenceSet<Block> snowyBlocks = new ReferenceOpenHashSet<>();
		NbtConditionalIdMap<Block> blockNbtMap = new NbtConditionalIdMap<>();

		blockPropertiesMap.forEach((intId, entries) -> {
			for (BlockEntry entry : entries) {
				addBlockMetasWithFlattening(entry, blockMatches, blockNbtMap, intId, snowyBlocks);
			}
		});

		BlockRenderingSettings.INSTANCE.setHasSnowyEntries(!snowyBlocks.isEmpty());
		BlockRenderingSettings.INSTANCE.setSnowyBlocks(snowyBlocks);
		BlockRenderingSettings.INSTANCE.setBlockNbtMap(blockNbtMap.isEmpty() ? null : blockNbtMap);
		return blockMatches;
	}

	public static Map<Block, BlockRenderLayer> createBlockTypeMap(Map<NamespacedId, BlockRenderType> blockPropertiesMap) {
		Map<Block, BlockRenderLayer> blockTypeIds = new Reference2ReferenceOpenHashMap<>();

		blockPropertiesMap.forEach((id, blockType) -> {
			final Block block = resolveBlockOrNull(id);

			if (block == null || block == Blocks.AIR) {
				return;
			}

			final BlockRenderLayer layer = convertBlockToRenderLayer(blockType);
			if (layer != null) {
				blockTypeIds.put(block, layer);
			}
		});

		return blockTypeIds;
	}

	private static BlockRenderLayer convertBlockToRenderLayer(BlockRenderType type) {
		if (type == null) {
			return null;
		}

		return switch (type) {
			case SOLID -> BlockRenderLayer.SOLID;
			case CUTOUT -> BlockRenderLayer.CUTOUT;
			case CUTOUT_MIPPED -> BlockRenderLayer.CUTOUT_MIPPED;
			case TRANSLUCENT -> BlockRenderLayer.TRANSLUCENT;
		};
	}

	private static void addBlockMetasWithFlattening(BlockEntry entry, Reference2ObjectMap<Block, Int2IntMap> idMap,
	                                                NbtConditionalIdMap<Block> blockNbtMap, int intId,
	                                                ReferenceSet<Block> snowyBlocks) {
		List<BlockEntry> flattenedEntries = resolveFlattenedEntries(entry);
		if (flattenedEntries != null) {
			Map<String, String> inheritedRuntimeProperties = extractRuntimeStateProperties(entry.getStateProperties());
			for (BlockEntry flattenedEntry : flattenedEntries) {
				addBlockMetas(flattenedEntry, idMap, blockNbtMap, intId, inheritedRuntimeProperties, snowyBlocks);
			}
			return;
		}

		addBlockMetas(entry, idMap, blockNbtMap, intId, entry.getStateProperties(), snowyBlocks);
	}

	/**
	 * Adds block+metadata combinations to the material ID map.
	 * Based on Iris's addBlockStates method, adapted for 1.7.10 metadata system.
	 */
	private static void addBlockMetas(BlockEntry entry, Reference2ObjectMap<Block, Int2IntMap> idMap,
	                                  NbtConditionalIdMap<Block> blockNbtMap, int intId,
	                                  Map<String, String> effectiveStateProperties, ReferenceSet<Block> snowyBlocks) {
		final NamespacedId id = entry.getId();
		final ResourceLocation resourceLocation = new ResourceLocation(id.getNamespace(), id.getName());

		final Block block = Block.REGISTRY.getObject(resourceLocation);

		// If the block doesn't exist, by default the registry will return AIR. That probably isn't what we want.
		// TODO: Assuming that Registry.BLOCK.getDefaultId() == "minecraft:air" here
		if (block == null || block == Blocks.AIR) {
			return;
		}

		if (entry.hasNbtProperties()) {
			blockNbtMap.addCondition(block, entry.getNbtProperties(), intId);
			return;
		}

		Set<Integer> metas = entry.getMetas();
		Map<String, String> stateProperties = effectiveStateProperties;
		int extraBits = 0;
		String snowy = stateProperties.get("snowy");
		if (snowy != null) {
			snowyBlocks.add(block);
			if ("true".equalsIgnoreCase(snowy)) {
				extraBits |= SNOWY_META_BIT;
			}
			stateProperties = withoutStateProperty(stateProperties, "snowy");
		}

		if (!stateProperties.isEmpty()) {
			Set<Integer> matchedMetas = resolveMetasFromStateProperties(block, stateProperties);
			if (matchedMetas.isEmpty()) {
				return;
			}

			if (metas.isEmpty()) {
				metas = matchedMetas;
			} else {
				Set<Integer> intersection = new HashSet<>(metas);
				intersection.retainAll(matchedMetas);
				if (intersection.isEmpty()) {
					return;
				}
				metas = Set.copyOf(intersection);
			}
		}

		Int2IntMap metaMap = idMap.get(block);
		if (metaMap == null) {
			metaMap = new Int2IntOpenHashMap();
			metaMap.defaultReturnValue(-1);
			idMap.put(block, metaMap);
		}

		if (metas.isEmpty()) {
			// Add all metadata values (0-15) if there aren't any specific ones
			for (int meta = 0; meta < 16; meta++) {
				metaMap.putIfAbsent(meta | extraBits, intId);
			}
		} else {
			// Add only specific metadata values
			for (int meta : metas) {
				metaMap.putIfAbsent(meta | extraBits, intId);
			}
		}
	}

	private static List<BlockEntry> resolveFlattenedEntries(BlockEntry entry) {
		NamespacedId id = entry.getId();
		if (!"minecraft".equals(id.getNamespace())) {
			return null;
		}

		return FlatteningMap.toLegacy(id.getName(), entry.getStateProperties());
	}

	private static Set<Integer> resolveMetasFromStateProperties(Block block, Map<String, String> stateProperties) {
		Set<Integer> metas = new HashSet<>();

		for (int meta = 0; meta < 16; meta++) {
			IBlockState state;
			try {
				state = block.getStateFromMeta(meta);
			} catch (RuntimeException ignored) {
				continue;
			}

			if (matchesStateProperties(state, stateProperties)) {
				metas.add(meta);
			}
		}

		return metas;
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

	private static Map<String, String> extractRuntimeStateProperties(Map<String, String> stateProperties) {
		String snowy = stateProperties.get("snowy");
		if (snowy == null) {
			return Collections.emptyMap();
		}

		return Collections.singletonMap("snowy", snowy);
	}

	private static Map<String, String> withoutStateProperty(Map<String, String> stateProperties, String propertyName) {
		if (!stateProperties.containsKey(propertyName)) {
			return stateProperties;
		}

		if (stateProperties.size() == 1) {
			return Collections.emptyMap();
		}

		Map<String, String> copy = new java.util.LinkedHashMap<>(stateProperties);
		copy.remove(propertyName);
		return copy;
	}

	private static Block resolveBlockOrNull(NamespacedId id) {
		final ResourceLocation resourceLocation = new ResourceLocation(id.getNamespace(), id.getName());
		final Block block = Block.getBlockFromName(resourceLocation.toString());
		if (block != null && block != Blocks.AIR) {
			return block;
		}

		List<BlockEntry> flattenedEntries = FlatteningMap.toLegacy(id.getName());
		if (flattenedEntries == null || flattenedEntries.isEmpty()) {
			return block;
		}

		BlockEntry legacyEntry = flattenedEntries.get(0);
		return Block.getBlockFromName(new ResourceLocation(legacyEntry.getId().getNamespace(), legacyEntry.getId().getName()).toString());
	}
}
