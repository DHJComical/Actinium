package com.dhj.actinium.render.entity;

import com.dhj.actinium.mixin.vintage.core.terrain.AccessorChunk;
import net.minecraft.entity.Entity;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the per-frame entity collection contract of {@link EntityGatherer}.
 *
 * <p>The gatherer replaced {@code RenderGlobal}'s per-frame scan of {@code WorldClient.loadedEntityList}
 * with a walk over the entity maps of the chunks an {@link EntitySource} hands in, because mods may
 * spawn multipart entities that never enter the loaded entity list. These tests drive the real
 * gatherer with real {@link Chunk} instances (real {@link ClassInheritanceMultiMap} section storage,
 * real {@link Chunk#addEntity(Entity)} placement) and real {@link Entity} instances, so the pass
 * splitting, the multi-chunk/multi-section accumulation and the frame-to-frame list reuse are all
 * exercised against the code that runs in game.</p>
 */
class EntityGathererTest {
    /** First render pass index; the gatherer splits entities over the passes the game draws. */
    private static final int PASS_0 = 0;
    /** Second render pass index. */
    private static final int PASS_1 = 1;
    /** Vertical size of one entity-list section, i.e. the Y granularity of {@code Chunk.addEntity}. */
    private static final int SECTION_HEIGHT = 16;
    /** Number of entity-list sections a chunk spans over the 0..255 world height. */
    private static final int SECTION_COUNT = 16;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        // Building a Chunk goes through ForgeEventFactory.gatherCapabilities and an Entity registers
        // its data parameters, so the vanilla registries have to exist before the fixtures are built.
        Bootstrap.register();
    }

    @Test
    void splitsEntitiesOverThePassesTheyRenderIn() {
        ProbeChunk chunk = new ProbeChunk(0, 0);
        PassEntity firstPassOnly = addEntity(chunk, 2.0, 8.0, true, false);
        PassEntity bothPasses = addEntity(chunk, 6.0, 8.0, true, true);
        PassEntity secondPassOnly = addEntity(chunk, 10.0, 8.0, false, true);
        PassEntity noPass = addEntity(chunk, 14.0, 8.0, false, false);

        List<Entity>[] lists = new EntityGatherer().gather(new ChunkListSource(chunk));
        List<Entity> firstPass = lists[PASS_0];
        List<Entity> secondPass = lists[PASS_1];

        assertEquals(2, firstPass.size(), "pass 0 collects the pass 0 entity and the both-pass entity");
        assertEquals(2, secondPass.size(), "pass 1 collects the both-pass entity and the pass 1 entity");
        assertTrue(firstPass.contains(firstPassOnly));
        assertTrue(firstPass.contains(bothPasses));
        assertFalse(firstPass.contains(secondPassOnly));
        assertFalse(firstPass.contains(noPass));
        assertTrue(secondPass.contains(bothPasses));
        assertTrue(secondPass.contains(secondPassOnly));
        assertFalse(secondPass.contains(firstPassOnly));
        assertFalse(secondPass.contains(noPass));
        // An entity rendered in both passes is listed once per pass, not twice in the same pass.
        assertEquals(1, occurrencesOf(firstPass, bothPasses));
        assertEquals(1, occurrencesOf(secondPass, bothPasses));
    }

    @Test
    void collectsEntitiesFromEveryChunkAndSection() {
        ProbeChunk first = new ProbeChunk(0, 0);
        ProbeChunk second = new ProbeChunk(3, -2);
        PassEntity bottomOfFirst = addEntity(first, 8.0, 8.0, true, false);
        PassEntity middleOfFirst = addEntity(first, 8.0, 2 * SECTION_HEIGHT + 8.0, true, false);
        PassEntity bottomOfSecond = addEntity(second, 8.0, 8.0, true, false);
        PassEntity topOfSecond = addEntity(second, 8.0, (SECTION_COUNT - 1) * SECTION_HEIGHT + 8.0, true, false);

        // Pin the fixture: the entities really live in two chunks and in three different sections,
        // which is the state the gatherer has to walk across.
        assertSame(bottomOfFirst, entityOfSection(first, 0));
        assertSame(middleOfFirst, entityOfSection(first, 2));
        assertSame(bottomOfSecond, entityOfSection(second, 0));
        assertSame(topOfSecond, entityOfSection(second, SECTION_COUNT - 1));

        List<Entity> collected = new EntityGatherer().gather(new ChunkListSource(first, second))[PASS_0];

        assertEquals(4, collected.size());
        assertTrue(collected.contains(bottomOfFirst));
        assertTrue(collected.contains(middleOfFirst));
        assertTrue(collected.contains(bottomOfSecond));
        assertTrue(collected.contains(topOfSecond));
    }

    @Test
    void ignoresChunksThatHoldNoEntities() {
        ProbeChunk neverPopulated = new ProbeChunk(1, 1);
        ProbeChunk populated = new ProbeChunk(2, 2);
        PassEntity entity = addEntity(populated, 8.0, 8.0, true, true);

        assertFalse(neverPopulated.celeritas$getHasEntities(), "fixture: this chunk never received an entity");

        List<Entity>[] lists = new EntityGatherer().gather(new ChunkListSource(neverPopulated, populated));

        assertEquals(1, lists[PASS_0].size());
        assertSame(entity, lists[PASS_0].get(0));
        assertEquals(1, lists[PASS_1].size());
        assertSame(entity, lists[PASS_1].get(0));
    }

    @Test
    void toleratesTheProvidersEmptyChunk() {
        // EmptyChunk is the chunk the client provider returns for unloaded positions; it never
        // accepts entities and its section storage is the one that used to blow the gatherer up.
        EmptyProbeChunk emptyChunk = new EmptyProbeChunk(0, 0);
        ProbeChunk populated = new ProbeChunk(0, 1);
        PassEntity entity = addEntity(populated, 8.0, 8.0, true, false);

        assertTrue(emptyChunk.isEmpty(), "fixture: EmptyChunk reports itself as empty");
        assertFalse(emptyChunk.celeritas$getHasEntities(), "fixture: EmptyChunk never holds entities");

        List<Entity>[] lists = assertDoesNotThrow(
            () -> new EntityGatherer().gather(new ChunkListSource(emptyChunk, populated)));

        assertEquals(1, lists[PASS_0].size());
        assertSame(entity, lists[PASS_0].get(0));
        assertTrue(lists[PASS_1].isEmpty());
    }

    @Test
    void toleratesUnpopulatedSectionsInTheEntityListArray() {
        ProbeChunk chunk = new ProbeChunk(4, 4);
        PassEntity entity = addEntity(chunk, 8.0, 2 * SECTION_HEIGHT + 8.0, true, false);
        // The gatherer only receives the live section array, and it has to keep collecting from the
        // sections that do exist when other sections were never allocated.
        chunk.getEntityLists()[0] = null;
        chunk.getEntityLists()[5] = null;

        List<Entity>[] lists = assertDoesNotThrow(() -> new EntityGatherer().gather(new ChunkListSource(chunk)));

        assertEquals(1, lists[PASS_0].size());
        assertSame(entity, lists[PASS_0].get(0));
        assertTrue(lists[PASS_1].isEmpty());
    }

    @Test
    void reusesTheSameListsAndDropsThePreviousFrame() {
        EntityGatherer gatherer = new EntityGatherer();
        ProbeChunk firstFrameChunk = new ProbeChunk(0, 0);
        PassEntity firstFrameEntity = addEntity(firstFrameChunk, 8.0, 8.0, true, true);

        List<Entity>[] firstFrame = gatherer.gather(new ChunkListSource(firstFrameChunk));
        List<Entity> firstPassList = firstFrame[PASS_0];
        List<Entity> secondPassList = firstFrame[PASS_1];

        assertEquals(1, firstPassList.size());
        assertSame(firstFrameEntity, firstPassList.get(0));

        gatherer.clear();
        ProbeChunk secondFrameChunk = new ProbeChunk(5, 5);
        PassEntity secondFrameEntity = addEntity(secondFrameChunk, 8.0, 8.0, true, false);

        List<Entity>[] secondFrame = gatherer.gather(new ChunkListSource(secondFrameChunk));

        assertSame(firstFrame, secondFrame, "gather must hand back the reused backing array");
        assertSame(firstPassList, secondFrame[PASS_0], "the pass 0 list must be reused");
        assertSame(secondPassList, secondFrame[PASS_1], "the pass 1 list must be reused");
        assertEquals(1, secondFrame[PASS_0].size());
        assertSame(secondFrameEntity, secondFrame[PASS_0].get(0));
        assertTrue(secondFrame[PASS_1].isEmpty());
        assertFalse(secondFrame[PASS_0].contains(firstFrameEntity), "cleared entities must not leak into the next frame");
        assertFalse(secondFrame[PASS_1].contains(firstFrameEntity));
    }

    @Test
    void keepsAppendingUntilClearResetsTheLists() {
        EntityGatherer gatherer = new EntityGatherer();
        ProbeChunk firstChunk = new ProbeChunk(0, 0);
        PassEntity first = addEntity(firstChunk, 8.0, 8.0, true, false);
        ProbeChunk secondChunk = new ProbeChunk(1, 0);
        PassEntity second = addEntity(secondChunk, 8.0, 8.0, true, false);

        List<Entity>[] afterFirstFrame = gatherer.gather(new ChunkListSource(firstChunk));
        List<Entity>[] afterSecondFrame = gatherer.gather(new ChunkListSource(secondChunk));

        assertSame(afterFirstFrame, afterSecondFrame);
        assertEquals(2, afterSecondFrame[PASS_0].size(), "gather appends, the caller clears");
        assertTrue(afterSecondFrame[PASS_0].contains(first));
        assertTrue(afterSecondFrame[PASS_0].contains(second));

        gatherer.clear();

        assertTrue(afterSecondFrame[PASS_0].isEmpty(), "clear must empty the reused pass lists");
        assertTrue(afterSecondFrame[PASS_1].isEmpty());
    }

    @Test
    void collectsEntitiesThatOnlyTheChunkEntityMapKnows() {
        ProbeChunk chunk = new ProbeChunk(7, -3);
        // A multipart part: it is added to the chunk it lives in and nowhere else.
        PassEntity multipartPart = addEntity(chunk, 8.0, SECTION_HEIGHT + 8.0, true, true);
        // Stand-in for WorldClient.loadedEntityList, the list the gatherer deliberately no longer reads.
        PassEntity worldListOnlyEntity = new PassEntity(true, true);
        List<Entity> worldLevelLoadedEntities = Collections.singletonList(worldListOnlyEntity);

        List<Entity>[] lists = new EntityGatherer().gather(new ChunkListSource(chunk));

        assertEquals(1, lists[PASS_0].size());
        assertSame(multipartPart, lists[PASS_0].get(0));
        assertEquals(1, lists[PASS_1].size());
        assertSame(multipartPart, lists[PASS_1].get(0));
        assertTrue(chunk.getEntityLists()[1].contains(multipartPart), "fixture: the part lives in the chunk entity map");
        assertEquals(1, worldLevelLoadedEntities.size(), "fixture: the stand-in world list is not empty");
        // An entity no chunk knows about is not collected, so the chunk maps really are the only source.
        assertFalse(lists[PASS_0].contains(worldListOnlyEntity));
        assertFalse(lists[PASS_1].contains(worldListOnlyEntity));
    }

    /**
     * Places a pass-controlled entity inside the given chunk.
     *
     * <p>{@code Chunk.addEntity} derives the entity-list section from the Y coordinate and complains
     * when the X/Z coordinates do not fall inside the chunk, so the position is rebuilt from the
     * chunk coordinates.</p>
     */
    private static PassEntity addEntity(Chunk chunk, double localX, double posY, boolean firstPass, boolean secondPass) {
        PassEntity entity = new PassEntity(firstPass, secondPass);
        entity.setPosition(chunk.x * SECTION_HEIGHT + localX, posY, chunk.z * SECTION_HEIGHT);
        chunk.addEntity(entity);
        return entity;
    }

    /** Returns the single entity of one entity-list section, failing when the fixture is not set up as expected. */
    private static Entity entityOfSection(Chunk chunk, int section) {
        ClassInheritanceMultiMap<Entity> map = chunk.getEntityLists()[section];
        assertEquals(1, map.size(), "fixture: section " + section + " must hold exactly one entity");
        return map.iterator().next();
    }

    /** Counts the entries that are the same object as {@code target}, so duplicates are visible. */
    private static int occurrencesOf(List<Entity> entities, Entity target) {
        int count = 0;
        for (Entity candidate : entities) {
            if (candidate == target) {
                count++;
            }
        }
        return count;
    }

    /**
     * Entity fixture whose pass membership is chosen per instance.
     *
     * <p>The base class has to be complemented with the three abstract members it declares, and a
     * {@code null} world is fine here because the gatherer only asks for the pass membership.</p>
     */
    private static final class PassEntity extends Entity {
        private final boolean firstPass;
        private final boolean secondPass;

        PassEntity(boolean firstPass, boolean secondPass) {
            super(null);
            this.firstPass = firstPass;
            this.secondPass = secondPass;
        }

        @Override
        public boolean shouldRenderInPass(int pass) {
            if (pass == PASS_0) {
                return firstPass;
            }
            if (pass == PASS_1) {
                return secondPass;
            }
            return false;
        }

        @Override
        protected void entityInit() {
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
        }
    }

    /**
     * Real chunk that also provides the {@link AccessorChunk} accessor the gatherer reads.
     *
     * <p>{@code AccessorChunk} is a {@code @Mixin(Chunk.class)} accessor, so a plain JUnit run never
     * applies it and the gatherer's cast would fail on a bare {@link Chunk}. This subclass supplies
     * the accessor the mixin injects in game; the storage being scanned is still the real
     * {@link ClassInheritanceMultiMap} array of a real chunk.</p>
     *
     * <p>{@code Chunk.hasEntities} is raised by {@link Chunk#addEntity(Entity)} and written by the
     * public {@link Chunk#setHasEntities(boolean)}, both of which the mirror flag follows, because
     * the private field itself is unreadable without the accessor under test.</p>
     */
    private static final class ProbeChunk extends Chunk implements AccessorChunk {
        private boolean hasEntities;

        ProbeChunk(int chunkX, int chunkZ) {
            super(null, chunkX, chunkZ);
        }

        @Override
        public void addEntity(Entity entityIn) {
            super.addEntity(entityIn);
            this.hasEntities = true;
        }

        @Override
        public void setHasEntities(boolean hasEntitiesIn) {
            super.setHasEntities(hasEntitiesIn);
            this.hasEntities = hasEntitiesIn;
        }

        @Override
        public boolean celeritas$getHasEntities() {
            return this.hasEntities;
        }
    }

    /**
     * Real {@link EmptyChunk} that provides the same accessor.
     *
     * <p>An {@link EmptyChunk} never accepts an entity and reports itself as empty, so the entity flag
     * is false by construction rather than by a value chosen by the test.</p>
     */
    private static final class EmptyProbeChunk extends EmptyChunk implements AccessorChunk {
        EmptyProbeChunk(int chunkX, int chunkZ) {
            super(null, chunkX, chunkZ);
        }

        @Override
        public boolean celeritas$getHasEntities() {
            return false;
        }
    }

    /** Minimal {@link EntitySource} handing the gatherer the chunks a test wants it to scan. */
    private static final class ChunkListSource implements EntitySource {
        private final List<Chunk> chunks;

        ChunkListSource(Chunk... chunks) {
            this.chunks = Arrays.asList(chunks);
        }

        @Override
        public Iterable<Chunk> entityChunks() {
            return this.chunks;
        }
    }
}
