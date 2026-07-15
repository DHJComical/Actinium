package com.dhj.actinium.render;

import net.coderbot.iris.uniforms.CapturedRenderingState;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Temporarily hides the portal material ID while a precomposed portal is submitted to a shader pack.
 */
final class EndPortalBlockEntityIdScope {
    private static final int NEUTRAL_BLOCK_ENTITY_ID = 0;

    private EndPortalBlockEntityIdScope() {
    }

    /** Runs one final portal draw with the dispatcher's neutral block-entity ID. */
    static void run(Runnable draw) {
        CapturedRenderingState state = CapturedRenderingState.INSTANCE;
        run(state::getCurrentRenderedBlockEntity, state::setCurrentBlockEntity, draw);
    }

    /**
     * Applies and restores the neutral ID through injectable accessors so notification behavior is testable.
     */
    static void run(IntSupplier idReader, IntConsumer idWriter, Runnable draw) {
        int previousId = idReader.getAsInt();
        try {
            idWriter.accept(NEUTRAL_BLOCK_ENTITY_ID);
            draw.run();
        } finally {
            if (idReader.getAsInt() != previousId) {
                idWriter.accept(previousId);
            }
        }
    }
}
