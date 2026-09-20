package dhj.embeddedt.embeddium.impl.render.chunk.sprite;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import dhj.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import dhj.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import dhj.embeddedt.embeddium.impl.render.chunk.lists.SectionTicker;
import dhj.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;

import java.util.List;
import java.util.function.Consumer;

public class GenericSectionSpriteTicker<T> implements SectionTicker {
    private static final Object[] NO_SPRITES = new Object[0];

    private static final int PRUNE_INTERVAL = 1024;

    private volatile ReferenceOpenHashSet<T> sprites = new ReferenceOpenHashSet<>();

    private final Consumer<T> markActive;

    private final Reference2ObjectOpenHashMap<RenderRegion, CachedRegionSprites> regionCache = new Reference2ObjectOpenHashMap<>();

    // Scratch set for deduplicating a region's sprites while recomputing its cache entry
    private final ReferenceOpenHashSet<Object> scratch = new ReferenceOpenHashSet<>();

    private int searchIndex;

    private static final class CachedRegionSprites {
        int revision;
        Object[] sprites;
        int lastSeenSearch;
    }

    public GenericSectionSpriteTicker(Consumer<T> markActive) {
        this.markActive = markActive;
    }

    @Override
    public void tickVisibleRenders() {
        this.sprites.forEach(this.markActive);
    }

    @Override
    public String getDebugString() {
        return "A: " + this.sprites.size();
    }

    @Override
    public void onRenderListUpdated(List<ChunkRenderList> renderLists) {
        var spriteSet = new ReferenceOpenHashSet<T>(this.sprites.size());
        int search = ++this.searchIndex;

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < renderLists.size(); i++) {
            ChunkRenderList renderList = renderLists.get(i);

            if (renderList.getSectionsWithSpritesCount() == 0) {
                continue;
            }

            var region = renderList.getRegion();
            var cached = this.regionCache.get(region);

            if (cached == null) {
                cached = new CachedRegionSprites();
                cached.revision = region.getDataRevision() - 1;
                this.regionCache.put(region, cached);
            }

            if (cached.revision != region.getDataRevision()) {
                this.recompute(region, cached);
            }

            cached.lastSeenSearch = search;

            var regionSprites = cached.sprites;
            //noinspection ForLoopReplaceableByForEach
            for (int j = 0; j < regionSprites.length; j++) {
                //noinspection unchecked
                spriteSet.add((T) regionSprites[j]);
            }
        }

        if ((search % PRUNE_INTERVAL) == 0) {
            this.regionCache.values().removeIf(entry -> search - entry.lastSeenSearch > PRUNE_INTERVAL);
        }

        this.sprites = spriteSet;
    }

    private void recompute(RenderRegion region, CachedRegionSprites cached) {
        var scratch = this.scratch;

        for (int sectionIndex = 0; sectionIndex < RenderRegion.REGION_SIZE; sectionIndex++) {
            var section = region.getSection(sectionIndex);

            if (section == null) {
                continue;
            }

            if (!(section.getBuiltContext() instanceof MinecraftBuiltRenderSectionData<?, ?> mcData)) {
                continue;
            }

            var sprites = mcData.animatedSprites;

            if (!sprites.isEmpty()) {
                scratch.addAll(sprites);
            }
        }

        cached.revision = region.getDataRevision();
        cached.sprites = scratch.isEmpty() ? NO_SPRITES : scratch.toArray();
        scratch.clear();
    }
}
