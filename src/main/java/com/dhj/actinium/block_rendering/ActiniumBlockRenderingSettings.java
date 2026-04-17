package com.dhj.actinium.block_rendering;

import com.dhj.actinium.celeritas.ActiniumBlockRenderLayer;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ActiniumBlockRenderingSettings {
    private static final String MODERN_SETTINGS_CLASS = "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings";
    private static final String LEGACY_SETTINGS_CLASS = "net.coderbot.iris.block_rendering.BlockRenderingSettings";

    public static final ActiniumBlockRenderingSettings INSTANCE = new ActiniumBlockRenderingSettings();

    private final @Nullable Object settingsInstance;
    private final @Nullable Method getBlockTypeIdsMethod;
    private final @Nullable Method getBlockMetaMatchesMethod;
    private final @Nullable Method shouldUseSeparateAoMethod;
    private final @Nullable Method shouldUseExtendedVertexFormatMethod;
    private final @Nullable Method getVertexFormatMethod;
    private @Nullable Reference2ObjectMap<Block, Int2IntMap> localBlockMetaMatches;
    private @Nullable Map<Block, ActiniumBlockRenderLayer> localBlockTypeIds;
    private @Nullable Boolean localUseSeparateAo;
    private @Nullable Boolean localUseExtendedVertexFormat;
    private boolean reloadRequired;

    private ActiniumBlockRenderingSettings() {
        Object resolvedSettings = getSingleton(MODERN_SETTINGS_CLASS);
        if (resolvedSettings == null) {
            resolvedSettings = getSingleton(LEGACY_SETTINGS_CLASS);
        }

        this.settingsInstance = resolvedSettings;
        this.getBlockTypeIdsMethod = findMethod(resolvedSettings != null ? resolvedSettings.getClass() : null, "getBlockTypeIds");
        this.getBlockMetaMatchesMethod = findMethod(resolvedSettings != null ? resolvedSettings.getClass() : null, "getBlockMetaMatches");
        this.shouldUseSeparateAoMethod = findMethod(resolvedSettings != null ? resolvedSettings.getClass() : null, "shouldUseSeparateAo");
        this.shouldUseExtendedVertexFormatMethod = findMethod(resolvedSettings != null ? resolvedSettings.getClass() : null, "shouldUseExtendedVertexFormat");
        this.getVertexFormatMethod = findMethod(resolvedSettings != null ? resolvedSettings.getClass() : null, "getVertexFormat");
    }

    public boolean shouldUseSeparateAo() {
        if (this.localUseSeparateAo != null) {
            return this.localUseSeparateAo;
        }

        Object result = invoke(this.shouldUseSeparateAoMethod, this.settingsInstance);
        return result instanceof Boolean value && value;
    }

    public boolean shouldUseExtendedVertexFormat() {
        if (this.localUseExtendedVertexFormat != null) {
            return this.localUseExtendedVertexFormat;
        }

        Object result = invoke(this.shouldUseExtendedVertexFormatMethod, this.settingsInstance);
        if (result instanceof Boolean value) {
            return value;
        }

        result = invoke(this.getVertexFormatMethod, this.settingsInstance);
        if (result == null) {
            return false;
        }

        String typeName = result.getClass().getName().toLowerCase(Locale.ROOT);
        return !typeName.contains("compact");
    }

    public @Nullable Map<Block, ActiniumBlockRenderLayer> getBlockTypeIds() {
        if (this.localBlockTypeIds != null) {
            return this.localBlockTypeIds;
        }

        Object result = invoke(this.getBlockTypeIdsMethod, this.settingsInstance);
        if (!(result instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return null;
        }

        Map<Block, ActiniumBlockRenderLayer> converted = new HashMap<>();

        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof Block block)) {
                continue;
            }

            ActiniumBlockRenderLayer layer = convertLayer(entry.getValue());
            if (layer != null) {
                converted.put(block, layer);
            }
        }

        return converted.isEmpty() ? null : converted;
    }

    public int getBlockStateId(Block block, int metadata) {
        int localMatch = resolveBlockStateId(this.localBlockMetaMatches, block, metadata);
        if (localMatch != Integer.MIN_VALUE) {
            return localMatch;
        }

        Object result = invoke(this.getBlockMetaMatchesMethod, this.settingsInstance);

        if (result instanceof Reference2ObjectMap<?, ?> referenceMap) {
            Object metaMap = referenceMap.get(block);
            if (metaMap instanceof Int2IntMap intMap && intMap.containsKey(metadata)) {
                return intMap.get(metadata);
            }
        } else if (result instanceof Map<?, ?> rawMap) {
            Object metaMap = rawMap.get(block);
            if (metaMap instanceof Int2IntMap intMap && intMap.containsKey(metadata)) {
                return intMap.get(metadata);
            }
            if (metaMap instanceof Map<?, ?> genericMetaMap) {
                Object mappedId = genericMetaMap.get(metadata);
                if (mappedId instanceof Number number) {
                    return number.intValue();
                }
            }
        }

        return Block.getIdFromBlock(block);
    }

    public boolean isReloadRequired() {
        return this.reloadRequired;
    }

    public void clearLocalOverrides() {
        boolean changed = this.localBlockMetaMatches != null
                || this.localBlockTypeIds != null
                || this.localUseSeparateAo != null
                || this.localUseExtendedVertexFormat != null;

        this.localBlockMetaMatches = null;
        this.localBlockTypeIds = null;
        this.localUseSeparateAo = null;
        this.localUseExtendedVertexFormat = null;

        if (changed) {
            this.reloadRequired = true;
        }
    }

    public void setBlockMetaMatches(@Nullable Reference2ObjectMap<Block, Int2IntMap> blockMetaMatches) {
        if (Objects.equals(this.localBlockMetaMatches, blockMetaMatches)) {
            return;
        }

        this.localBlockMetaMatches = blockMetaMatches;
        this.reloadRequired = true;
    }

    public void setBlockTypeIds(@Nullable Map<Block, ActiniumBlockRenderLayer> blockTypeIds) {
        Map<Block, ActiniumBlockRenderLayer> normalized = blockTypeIds == null || blockTypeIds.isEmpty()
                ? null
                : Collections.unmodifiableMap(new HashMap<>(blockTypeIds));

        if (Objects.equals(this.localBlockTypeIds, normalized)) {
            return;
        }

        this.localBlockTypeIds = normalized;
        this.reloadRequired = true;
    }

    public void setUseSeparateAo(boolean useSeparateAo) {
        if (Objects.equals(this.localUseSeparateAo, useSeparateAo)) {
            return;
        }

        this.localUseSeparateAo = useSeparateAo;
        this.reloadRequired = true;
    }

    public void setUseExtendedVertexFormat(boolean useExtendedVertexFormat) {
        if (Objects.equals(this.localUseExtendedVertexFormat, useExtendedVertexFormat)) {
            return;
        }

        this.localUseExtendedVertexFormat = useExtendedVertexFormat;
        this.reloadRequired = true;
    }

    public void clearReloadRequired() {
        this.reloadRequired = false;
    }

    public void reloadRendererIfRequired() {
        if (!this.reloadRequired) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.renderGlobal != null) {
            minecraft.renderGlobal.loadRenderers();
        }

        this.clearReloadRequired();
    }

    public static @Nullable ActiniumBlockRenderLayer convertLayer(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof ActiniumBlockRenderLayer layer) {
            return layer;
        }

        String name;
        if (value instanceof Enum<?> enumValue) {
            name = enumValue.name();
        } else {
            name = value.toString();
        }

        return switch (name.toUpperCase(Locale.ROOT)) {
            case "SOLID" -> ActiniumBlockRenderLayer.SOLID;
            case "CUTOUT" -> ActiniumBlockRenderLayer.CUTOUT;
            case "CUTOUT_MIPPED" -> ActiniumBlockRenderLayer.CUTOUT_MIPPED;
            case "TRANSLUCENT", "TRIPWIRE" -> ActiniumBlockRenderLayer.TRANSLUCENT;
            default -> null;
        };
    }

    private static int resolveBlockStateId(@Nullable Reference2ObjectMap<Block, Int2IntMap> blockMetaMatches, Block block, int metadata) {
        if (blockMetaMatches == null) {
            return Integer.MIN_VALUE;
        }

        Int2IntMap intMap = blockMetaMatches.get(block);
        if (intMap != null && intMap.containsKey(metadata)) {
            return intMap.get(metadata);
        }

        return Integer.MIN_VALUE;
    }

    private static @Nullable Object getSingleton(String className) {
        try {
            Class<?> type = Class.forName(className);
            Field instanceField = type.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            return instanceField.get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable Method findMethod(@Nullable Class<?> type, String name) {
        if (type == null) {
            return null;
        }

        try {
            Method method = type.getMethod(name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable Object invoke(@Nullable Method method, @Nullable Object instance) {
        if (method == null) {
            return null;
        }

        try {
            return method.invoke(instance);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
