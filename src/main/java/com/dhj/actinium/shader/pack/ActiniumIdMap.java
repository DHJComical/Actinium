package com.dhj.actinium.shader.pack;

import com.dhj.actinium.celeritas.ActiniumBlockRenderLayer;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class ActiniumIdMap {
    public static final ActiniumIdMap EMPTY = new ActiniumIdMap(new Reference2ObjectOpenHashMap<>(), Collections.emptyMap(), new Object2IntOpenHashMap<>());

    private final Reference2ObjectMap<Block, Int2IntMap> blockMetaMatches;
    private final Map<Block, ActiniumBlockRenderLayer> blockTypeIds;
    private final Object2IntMap<ActiniumNamespacedId> entityIds;

    private ActiniumIdMap(Reference2ObjectMap<Block, Int2IntMap> blockMetaMatches,
                          Map<Block, ActiniumBlockRenderLayer> blockTypeIds,
                          Object2IntMap<ActiniumNamespacedId> entityIds) {
        this.blockMetaMatches = blockMetaMatches;
        this.blockTypeIds = blockTypeIds;
        this.entityIds = entityIds;
    }

    public static ActiniumIdMap parse(Properties properties) {
        return parse(properties, new Properties());
    }

    public static ActiniumIdMap parse(Properties blockProperties, Properties entityProperties) {
        Reference2ObjectMap<Block, Int2IntMap> blockMetaMatches = new Reference2ObjectOpenHashMap<>();
        Map<Block, ActiniumBlockRenderLayer> blockTypeIds = new HashMap<>();
        Object2IntMap<ActiniumNamespacedId> entityIds = new Object2IntOpenHashMap<>();
        entityIds.defaultReturnValue(-1);

        blockProperties.forEach((keyObject, valueObject) -> {
            String key = keyObject.toString();
            String value = valueObject.toString();

            if (key.startsWith("block.")) {
                parseBlockEntry(key.substring("block.".length()), value, blockMetaMatches);
                return;
            }

            if (key.startsWith("layer.")) {
                parseLayerEntry(key.substring("layer.".length()), value, blockTypeIds);
            }
        });

        entityProperties.forEach((keyObject, valueObject) -> {
            String key = keyObject.toString();
            String value = valueObject.toString();

            if (key.startsWith("entity.")) {
                parseEntityEntry(key.substring("entity.".length()), value, entityIds);
            }
        });

        return new ActiniumIdMap(blockMetaMatches, blockTypeIds, entityIds);
    }

    public @Nullable Reference2ObjectMap<Block, Int2IntMap> getBlockMetaMatches() {
        return this.blockMetaMatches.isEmpty() ? null : this.blockMetaMatches;
    }

    public @Nullable Map<Block, ActiniumBlockRenderLayer> getBlockTypeIds() {
        return this.blockTypeIds.isEmpty() ? null : this.blockTypeIds;
    }

    public @Nullable Object2IntMap<ActiniumNamespacedId> getEntityIds() {
        return this.entityIds.isEmpty() ? null : this.entityIds;
    }

    private static void parseBlockEntry(String idString, String value, Reference2ObjectMap<Block, Int2IntMap> blockMetaMatches) {
        int intId;

        try {
            intId = Integer.parseInt(idString);
        } catch (NumberFormatException ignored) {
            return;
        }

        for (String token : parseIdentifierList(value)) {
            ParsedBlockEntry entry = ParsedBlockEntry.parse(token);
            if (entry == null) {
                continue;
            }

            ResourceLocation location = new ResourceLocation(entry.namespace(), entry.path());
            Block block = Block.getBlockFromName(location.toString());

            if (block == null || block == Blocks.AIR) {
                continue;
            }

            Int2IntMap metadataMap = blockMetaMatches.get(block);
            if (metadataMap == null) {
                metadataMap = new Int2IntOpenHashMap();
                metadataMap.defaultReturnValue(-1);
                blockMetaMatches.put(block, metadataMap);
            }

            applyBlockEntry(metadataMap, block, entry, intId);
        }
    }

    private static void parseLayerEntry(String layerName, String value, Map<Block, ActiniumBlockRenderLayer> blockTypeIds) {
        ActiniumBlockRenderLayer layer = switch (layerName.toLowerCase(Locale.ROOT)) {
            case "solid" -> ActiniumBlockRenderLayer.SOLID;
            case "cutout" -> ActiniumBlockRenderLayer.CUTOUT;
            case "cutout_mipped" -> ActiniumBlockRenderLayer.CUTOUT_MIPPED;
            case "translucent" -> ActiniumBlockRenderLayer.TRANSLUCENT;
            default -> null;
        };

        if (layer == null) {
            return;
        }

        for (String token : parseIdentifierList(value)) {
            ParsedBlockEntry entry = ParsedBlockEntry.parse(token);
            if (entry == null) {
                continue;
            }

            ResourceLocation location = new ResourceLocation(entry.namespace(), entry.path());
            Block block = Block.getBlockFromName(location.toString());

            if (block == null || block == Blocks.AIR) {
                continue;
            }

            blockTypeIds.put(block, layer);
        }
    }

    private static void parseEntityEntry(String idString, String value, Object2IntMap<ActiniumNamespacedId> entityIds) {
        int intId;

        try {
            intId = Integer.parseInt(idString);
        } catch (NumberFormatException ignored) {
            return;
        }

        for (String token : parseIdentifierList(value)) {
            if (token.isEmpty() || token.contains("=")) {
                continue;
            }

            entityIds.put(new ActiniumNamespacedId(token), intId);
        }
    }

    private static List<String> parseIdentifierList(String value) {
        if (value.indexOf('"') == -1) {
            String[] parts = value.split("\\s+");
            List<String> result = new ArrayList<>(parts.length);

            for (String part : parts) {
                if (!part.isEmpty()) {
                    result.add(part);
                }
            }

            return result;
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            if (escaped) {
                current.append(character);
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(character) && !inQuotes) {
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(character);
            }
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    private static void applyBlockEntry(Int2IntMap metadataMap, Block block, ParsedBlockEntry entry, int intId) {
        if (!entry.propertyFilters().isEmpty()) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                if (matchesStateFilters(state, entry.propertyFilters())) {
                    metadataMap.putIfAbsent(block.getMetaFromState(state), intId);
                }
            }

            return;
        }

        if (entry.metadata().isEmpty()) {
            for (int meta = 0; meta < 16; meta++) {
                metadataMap.putIfAbsent(meta, intId);
            }
        } else {
            for (int meta : entry.metadata()) {
                metadataMap.putIfAbsent(meta, intId);
            }
        }
    }

    private static boolean matchesStateFilters(IBlockState state, Map<String, String> propertyFilters) {
        for (Map.Entry<String, String> entry : propertyFilters.entrySet()) {
            IProperty<?> property = findProperty(state, entry.getKey());

            if (property == null) {
                return false;
            }

            if (!entry.getValue().equalsIgnoreCase(getPropertyValueName(property, state.getValue(property)))) {
                return false;
            }
        }

        return true;
    }

    private static @Nullable IProperty<?> findProperty(IBlockState state, String propertyName) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (property.getName().equalsIgnoreCase(propertyName)) {
                return property;
            }
        }

        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String getPropertyValueName(IProperty property, Comparable value) {
        return property.getName(value);
    }

    private record ParsedBlockEntry(String namespace, String path, Set<Integer> metadata, Map<String, String> propertyFilters) {
        private static @Nullable ParsedBlockEntry parse(String token) {
            if (token.isEmpty()) {
                return null;
            }

            String[] parts = token.split(":");

            if (parts.length == 1) {
                return new ParsedBlockEntry("minecraft", parts[0], Collections.emptySet(), Collections.emptyMap());
            }

            if (parts.length == 2 && !startsWithDigit(parts[1]) && !parts[1].contains("=")) {
                return new ParsedBlockEntry(parts[0], parts[1], Collections.emptySet(), Collections.emptyMap());
            }

            int metadataStart;
            String namespace;
            String path;

            if (startsWithDigit(parts[1]) || parts[1].contains("=")) {
                namespace = "minecraft";
                path = parts[0];
                metadataStart = 1;
            } else {
                namespace = parts[0];
                path = parts[1];
                metadataStart = 2;
            }

            Set<Integer> metadata = new LinkedHashSet<>();
            Map<String, String> propertyFilters = new LinkedHashMap<>();

            for (int i = metadataStart; i < parts.length; i++) {
                String segment = parts[i];

                if (segment.contains("=")) {
                    int separatorIndex = segment.indexOf('=');

                    if (separatorIndex <= 0 || separatorIndex >= segment.length() - 1) {
                        continue;
                    }

                    propertyFilters.put(segment.substring(0, separatorIndex), segment.substring(separatorIndex + 1));
                    continue;
                }

                for (String metaToken : segment.split(",")) {
                    try {
                        metadata.add(Integer.parseInt(metaToken));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            return new ParsedBlockEntry(namespace, path, metadata, propertyFilters);
        }

        private static boolean startsWithDigit(String value) {
            return !value.isEmpty() && Character.isDigit(value.charAt(0));
        }
    }
}
