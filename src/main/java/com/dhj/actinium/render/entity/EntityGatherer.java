package com.dhj.actinium.render.entity;

import com.dhj.actinium.mixin.vintage.core.terrain.AccessorChunk;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;

/**
 * Collects the entities each render pass has to draw, once per frame.
 *
 * <p>The entities are read from the chunk entity maps instead of {@code WorldClient.loadedEntityList},
 * because mods may create multipart entities that never enter the loaded entity list (see
 * {@link EntitySource}).</p>
 */
public class EntityGatherer {
    public static final int NUM_PASSES = 2;

    /**
     * Initial capacity of every per-pass list. A modest fixed value keeps a typical frame from
     * growing the backing arrays over and over without trying to predict an exact entity count.
     */
    private static final int INITIAL_PASS_CAPACITY = 64;

    private final List<Entity>[] entityLists;

    @SuppressWarnings("unchecked")
    public EntityGatherer() {
        this.entityLists = new List[NUM_PASSES];

        for (int i = 0; i < NUM_PASSES; i++) {
            this.entityLists[i] = new ArrayList<>(INITIAL_PASS_CAPACITY);
        }
    }

    public void clear() {
        for (int i = 0; i < NUM_PASSES; i++) {
            entityLists[i].clear();
        }
    }

    /**
     * Appends the entities of the supplied chunks to the per-pass lists and returns those lists.
     *
     * <p>Collection is append-only: the caller has to invoke {@link #clear()} before every frame it
     * wants a fresh result from, otherwise entities are collected twice. The returned array is the
     * reused backing array, so it must not be kept across a {@link #clear()} call.</p>
     *
     * <p>Each entity is tested with {@link Entity#shouldRenderInPass(int)} once per pass and added
     * to every pass it renders in, exactly like the pass splitting the game expects.</p>
     *
     * @param source the chunks to scan for entities
     * @return the reused per-pass entity lists
     */
    public List<Entity>[] gather(EntitySource source) {
        List<Entity>[] entityLists = this.entityLists;

        for (Chunk chunk : source.entityChunks()) {
            if (!((AccessorChunk)chunk).celeritas$getHasEntities()) {
                continue;
            }

            ClassInheritanceMultiMap<Entity>[] entityMaps = chunk.getEntityLists();

            // Safety net only: the current Chunk constructor always fills the entity array, but this
            // runs on the render path and a chunk without entity storage has to be skipped, not thrown on.
            if (entityMaps == null) {
                continue;
            }

            for (ClassInheritanceMultiMap<Entity> entityMap : entityMaps) {
                if (entityMap == null || entityMap.isEmpty()) {
                    continue;
                }

                for (Entity entity : entityMap) {
                    for (int pass = 0; pass < NUM_PASSES; pass++) {
                        if (entity.shouldRenderInPass(pass)) {
                            entityLists[pass].add(entity);
                        }
                    }
                }
            }
        }

        return entityLists;
    }
}
