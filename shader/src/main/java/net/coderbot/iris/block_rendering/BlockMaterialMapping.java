package net.coderbot.iris.block_rendering;

import org.embeddedt.embeddium.api.shader.BlockRenderLayer;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.coderbot.iris.shaderpack.materialmap.BlockEntry;
import net.coderbot.iris.shaderpack.materialmap.BlockRenderType;
import net.coderbot.iris.shaderpack.materialmap.FlatteningMap;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockMaterialMapping {
	/**
	 * Creates a two-level map structure for block material IDs.
	 * Based on Iris's BlockState mapping approach adapted for 1.7.10's metadata system.
	 */
	public static Reference2ObjectMap<Block, Int2IntMap> createBlockMetaIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap) {
		Reference2ObjectMap<Block, Int2IntMap> blockMatches = new Reference2ObjectOpenHashMap<>();

		blockPropertiesMap.forEach((intId, entries) -> {
			for (BlockEntry entry : entries) {
				addBlockMetasWithFlattening(entry, blockMatches, intId);
			}
		});

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

	private static void addBlockMetasWithFlattening(BlockEntry entry, Reference2ObjectMap<Block, Int2IntMap> idMap, int intId) {
		List<BlockEntry> flattenedEntries = resolveFlattenedEntries(entry);
		if (flattenedEntries != null) {
			for (BlockEntry flattenedEntry : flattenedEntries) {
				addBlockMetas(flattenedEntry, idMap, intId);
			}
			return;
		}

		addBlockMetas(entry, idMap, intId);
	}

	/**
	 * Adds block+metadata combinations to the material ID map.
	 * Based on Iris's addBlockStates method, adapted for 1.7.10 metadata system.
	 */
	private static void addBlockMetas(BlockEntry entry, Reference2ObjectMap<Block, Int2IntMap> idMap, int intId) {
		final NamespacedId id = entry.getId();
		final ResourceLocation resourceLocation = new ResourceLocation(id.getNamespace(), id.getName());

		final Block block = Block.REGISTRY.getObject(resourceLocation);

		// If the block doesn't exist, by default the registry will return AIR. That probably isn't what we want.
		// TODO: Assuming that Registry.BLOCK.getDefaultId() == "minecraft:air" here
		if (block == null || block == Blocks.AIR) {
			return;
		}

		Set<Integer> metas = entry.getMetas();

		Int2IntMap metaMap = idMap.get(block);
		if (metaMap == null) {
			metaMap = new Int2IntOpenHashMap();
			metaMap.defaultReturnValue(-1);
			idMap.put(block, metaMap);
		}

		if (metas.isEmpty()) {
			// Add all metadata values (0-15) if there aren't any specific ones
			for (int meta = 0; meta < 16; meta++) {
				metaMap.putIfAbsent(meta, intId);
			}
		} else {
			// Add only specific metadata values
			for (int meta : metas) {
				metaMap.putIfAbsent(meta, intId);
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
