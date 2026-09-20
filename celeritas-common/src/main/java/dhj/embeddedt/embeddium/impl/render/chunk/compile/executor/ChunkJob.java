package dhj.embeddedt.embeddium.impl.render.chunk.compile.executor;

import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import dhj.embeddedt.embeddium.impl.util.task.CancellationToken;

public interface ChunkJob extends CancellationToken {
    void execute(ChunkBuildContext context);

    boolean isStarted();
}
