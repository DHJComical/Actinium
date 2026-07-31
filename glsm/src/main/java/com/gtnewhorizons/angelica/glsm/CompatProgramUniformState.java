package com.gtnewhorizons.angelica.glsm;

/**
 * Records compat uniform locations and successful uploads for one linked OpenGL program.
 *
 * <p>Uniform values belong to a program object. Keeping the uploaded generations beside that program preserves valid
 * uploads when rendering returns to a program after another program was active.</p>
 */
final class CompatProgramUniformState {

    private static final int MODEL_VIEW = 1 << 0;
    private static final int PROJECTION = 1 << 1;
    private static final int TEXTURE_MATRIX = 1 << 2;
    private static final int FRAGMENT = 1 << 3;
    private static final int COLOR = 1 << 4;
    private static final int LIGHTING = 1 << 5;
    private static final int CLIP_PLANE = 1 << 6;

    private final int[] locations;
    private volatile boolean valid = true;
    private int initialized;
    private int modelViewGeneration;
    private int projectionGeneration;
    private int textureMatrixGeneration;
    private int fragmentGeneration;
    private int colorGeneration;
    private int lightingGeneration;
    private int clipPlaneGeneration;

    CompatProgramUniformState(int[] locations) {
        this.locations = locations;
    }

    int[] getLocations() {
        return locations;
    }

    boolean isValid() {
        return valid;
    }

    void invalidate() {
        valid = false;
    }

    boolean needsModelViewUpload(int generation) {
        return needsUpload(MODEL_VIEW, generation, modelViewGeneration);
    }

    void markModelViewUploaded(int generation) {
        if (!valid) return;
        modelViewGeneration = generation;
        initialized |= MODEL_VIEW;
    }

    boolean needsProjectionUpload(int generation) {
        return needsUpload(PROJECTION, generation, projectionGeneration);
    }

    void markProjectionUploaded(int generation) {
        if (!valid) return;
        projectionGeneration = generation;
        initialized |= PROJECTION;
    }

    boolean needsTextureMatrixUpload(int generation) {
        return needsUpload(TEXTURE_MATRIX, generation, textureMatrixGeneration);
    }

    void markTextureMatrixUploaded(int generation) {
        if (!valid) return;
        textureMatrixGeneration = generation;
        initialized |= TEXTURE_MATRIX;
    }

    boolean needsFragmentUpload(int generation) {
        return needsUpload(FRAGMENT, generation, fragmentGeneration);
    }

    void markFragmentUploaded(int generation) {
        if (!valid) return;
        fragmentGeneration = generation;
        initialized |= FRAGMENT;
    }

    boolean needsColorUpload(int generation) {
        return needsUpload(COLOR, generation, colorGeneration);
    }

    void markColorUploaded(int generation) {
        if (!valid) return;
        colorGeneration = generation;
        initialized |= COLOR;
    }

    boolean needsLightingUpload(int generation) {
        return needsUpload(LIGHTING, generation, lightingGeneration);
    }

    void markLightingUploaded(int generation) {
        if (!valid) return;
        lightingGeneration = generation;
        initialized |= LIGHTING;
    }

    boolean needsClipPlaneUpload(int generation) {
        return needsUpload(CLIP_PLANE, generation, clipPlaneGeneration);
    }

    void markClipPlaneUploaded(int generation) {
        if (!valid) return;
        clipPlaneGeneration = generation;
        initialized |= CLIP_PLANE;
    }

    private boolean needsUpload(int category, int generation, int uploadedGeneration) {
        return (initialized & category) == 0 || uploadedGeneration != generation;
    }
}
