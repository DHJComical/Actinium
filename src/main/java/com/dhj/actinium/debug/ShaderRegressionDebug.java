package com.dhj.actinium.debug;

import com.dhj.actinium.runtime.ActiniumRuntime;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.coderbot.iris.block_rendering.BlockMaterialMapping;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ShaderRegressionDebug {
    private static final Logger LOGGER = LogManager.getLogger("ActiniumShaderDebug");
    private static final Map<String, Integer> ENTITY_COLOR_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> ENTITY_PHASE_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> ITEM_STATE_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> TERRAIN_STATE_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> BLOCK_MAP_COUNTS = new ConcurrentHashMap<>();

    private ShaderRegressionDebug() {
    }

    public static boolean isEnabled() {
        String override = System.getProperty("actinium.glDebug");
        if (override != null) {
            return Boolean.parseBoolean(override);
        }

        try {
            return ActiniumRuntime.options().debug.enableActiniumGlDebug;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static void logEntityColor(String stage, EntityLivingBase entity, float r, float g, float b, float a) {
        if (!isEnabled()) {
            return;
        }

        String label = stage + ":" + entity.getClass().getName();
        int count = ENTITY_COLOR_COUNTS.merge(label, 1, Integer::sum);
        if (count > 6) {
            return;
        }

        LOGGER.info(
                "entity-color stage={} entity={} count={} color=[{},{},{},{}] hurtTime={} deathTime={} phase={} renderedEntity={} renderedItem={}",
                stage,
                entity.getClass().getName(),
                count,
                r,
                g,
                b,
                a,
                entity.hurtTime,
                entity.deathTime,
                GbufferPrograms.getCurrentPhase(),
                CapturedRenderingState.INSTANCE.getCurrentRenderedEntity(),
                CapturedRenderingState.INSTANCE.getCurrentRenderedItem()
        );
    }

    public static void logEntityPhase(String stage, Entity entity, Render<?> renderer, String previousPhase, boolean beganEntityPhase) {
        if (!isEnabled()) {
            return;
        }

        String label = stage + ":" + entity.getClass().getName() + ":" + renderer.getClass().getName() + ":" + previousPhase + ":" + beganEntityPhase;
        int count = ENTITY_PHASE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 6) {
            return;
        }

        LOGGER.info(
                "entity-phase stage={} entity={} renderer={} count={} previousPhase={} beganEntityPhase={} currentPhase={} renderedEntity={} renderedItem={}",
                stage,
                entity.getClass().getName(),
                renderer.getClass().getName(),
                count,
                previousPhase,
                beganEntityPhase,
                GbufferPrograms.getCurrentPhase(),
                CapturedRenderingState.INSTANCE.getCurrentRenderedEntity(),
                CapturedRenderingState.INSTANCE.getCurrentRenderedItem()
        );
    }

    public static void logItemState(String stage, ItemStack stack) {
        if (!isEnabled()) {
            return;
        }

        Vector4f color = CapturedRenderingState.INSTANCE.getCurrentEntityColor();
        boolean tinted = Math.abs(color.w()) > 0.0001F
                || Math.abs(color.x()) > 0.0001F
                || Math.abs(color.y()) > 0.0001F
                || Math.abs(color.z()) > 0.0001F;
        String label = stage + ":" + tinted;
        int count = ITEM_STATE_COUNTS.merge(label, 1, Integer::sum);
        int maxCount = tinted ? 8 : 3;
        if (count > maxCount) {
            return;
        }

        String itemName = "empty";
        if (stack != null && !stack.isEmpty() && stack.getItem() != null) {
            itemName = String.valueOf(stack.getItem().getRegistryName());
        }

        LOGGER.info(
                "item-state stage={} item={} count={} entityColor=[{},{},{},{}] phase={} renderedEntity={} renderedItem={}",
                stage,
                itemName,
                count,
                color.x(),
                color.y(),
                color.z(),
                color.w(),
                GbufferPrograms.getCurrentPhase(),
                CapturedRenderingState.INSTANCE.getCurrentRenderedEntity(),
                CapturedRenderingState.INSTANCE.getCurrentRenderedItem()
        );
    }

    public static void logTerrainState(
            String stage,
            Block block,
            BlockPos pos,
            BlockRenderLayer layer,
            int metadata,
            int shaderMetadata,
            int vanillaBlockId,
            int shaderBlockId,
            short renderType,
            byte lightValue
    ) {
        if (!isEnabled()) {
            return;
        }

        String label = stage
                + ":" + String.valueOf(block.getRegistryName())
                + ":" + layer
                + ":" + metadata
                + ":" + shaderMetadata
                + ":" + vanillaBlockId
                + ":" + shaderBlockId
                + ":" + renderType;
        int count = TERRAIN_STATE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 4) {
            return;
        }

        LOGGER.info(
                "terrain-state stage={} block={} count={} pos=[{},{},{}] layer={} metadata={} shaderMetadata={} vanillaBlockId={} shaderBlockId={} renderType={} lightValue={}",
                stage,
                String.valueOf(block.getRegistryName()),
                count,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                layer,
                metadata,
                shaderMetadata,
                vanillaBlockId,
                shaderBlockId,
                renderType,
                lightValue
        );
    }

    public static void logTerrainIdResolution(
            Block block,
            BlockPos pos,
            int metadata,
            int shaderMetadata,
            int providerId,
            int actualStateId,
            int nbtId,
            int resolvedId
    ) {
        if (!isEnabled()) {
            return;
        }

        String label = "resolve:"
                + String.valueOf(block.getRegistryName())
                + ":" + metadata
                + ":" + shaderMetadata
                + ":" + providerId
                + ":" + actualStateId
                + ":" + nbtId
                + ":" + resolvedId;
        int count = TERRAIN_STATE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 4) {
            return;
        }

        LOGGER.info(
                "terrain-id-resolve block={} count={} pos=[{},{},{}] metadata={} shaderMetadata={} providerId={} actualStateId={} nbtId={} resolvedId={}",
                String.valueOf(block.getRegistryName()),
                count,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                metadata,
                shaderMetadata,
                providerId,
                actualStateId,
                nbtId,
                resolvedId
        );
    }

    public static void logBlockMetaMap(String label, Block block, int... metadataKeys) {
        if (!isEnabled() || block == null) {
            return;
        }

        int count = BLOCK_MAP_COUNTS.merge(label, 1, Integer::sum);
        if (count > 1) {
            return;
        }

        Reference2ObjectMap<Block, Int2IntMap> blockMetaMatches = BlockRenderingSettings.INSTANCE.getBlockMetaMatches();
        Int2IntMap metaMap = blockMetaMatches != null ? blockMetaMatches.get(block) : null;
        StringBuilder builder = new StringBuilder();
        if (metadataKeys != null) {
            for (int i = 0; i < metadataKeys.length; i++) {
                int metadata = metadataKeys[i];
                if (i > 0) {
                    builder.append(", ");
                }
                int direct = metaMap != null ? metaMap.get(metadata) : -1;
                int resolved = metaMap != null ? BlockMaterialMapping.resolveId(metaMap, metadata) : -1;
                builder.append(metadata)
                        .append("->")
                        .append(direct)
                        .append("/")
                        .append(resolved);
            }
        }

        LOGGER.info(
                "block-meta-map label={} block={} present={} samples=[{}]",
                label,
                String.valueOf(block.getRegistryName()),
                metaMap != null,
                builder
        );
    }
}
