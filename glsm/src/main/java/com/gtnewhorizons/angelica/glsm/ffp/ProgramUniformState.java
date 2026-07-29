package com.gtnewhorizons.angelica.glsm.ffp;

/**
 * Records the FFP uniform state successfully uploaded to one linked program.
 *
 * <p>Uniform values belong to a program object in OpenGL. Keeping these markers beside the program allows a previously
 * used variant to retain its uploaded values while another variant is active.</p>
 */
final class ProgramUniformState {

    private static final int MODEL_VIEW = 1 << 0;
    private static final int PROJECTION = 1 << 1;
    private static final int TEXTURE_MATRIX = 1 << 2;
    private static final int LIGHTING = 1 << 3;
    private static final int FRAGMENT = 1 << 4;
    private static final int COLOR = 1 << 5;
    private static final int NORMAL = 1 << 6;
    private static final int TEX_COORD = 1 << 7;
    private static final int TEX_GEN = 1 << 8;
    private static final int CLIP_PLANE = 1 << 9;
    private static final int LIGHTMAP = 1 << 10;
    private static final int LINE_WIDTH = 1 << 11;
    private static final int VIEWPORT = 1 << 12;

    private int initialized;
    private int modelViewGeneration;
    private int projectionGeneration;
    private int textureMatrixGeneration;
    private int lightingGeneration;
    private int fragmentGeneration;
    private int colorGeneration;
    private int normalGeneration;
    private int texCoordGeneration;
    private int texGenGeneration;
    private int clipPlaneGeneration;
    private float lightmapX;
    private float lightmapY;
    private float lineWidth;
    private int viewportWidth;
    private int viewportHeight;

    boolean needsModelViewUpload(int generation) {
        return needsUpload(MODEL_VIEW, generation, modelViewGeneration);
    }

    void markModelViewUploaded(int generation) {
        modelViewGeneration = generation;
        initialized |= MODEL_VIEW;
    }

    boolean needsProjectionUpload(int generation) {
        return needsUpload(PROJECTION, generation, projectionGeneration);
    }

    void markProjectionUploaded(int generation) {
        projectionGeneration = generation;
        initialized |= PROJECTION;
    }

    boolean needsTextureMatrixUpload(int generation) {
        return needsUpload(TEXTURE_MATRIX, generation, textureMatrixGeneration);
    }

    void markTextureMatrixUploaded(int generation) {
        textureMatrixGeneration = generation;
        initialized |= TEXTURE_MATRIX;
    }

    boolean needsLightingUpload(int generation) {
        return needsUpload(LIGHTING, generation, lightingGeneration);
    }

    void markLightingUploaded(int generation) {
        lightingGeneration = generation;
        initialized |= LIGHTING;
    }

    boolean needsFragmentUpload(int generation) {
        return needsUpload(FRAGMENT, generation, fragmentGeneration);
    }

    void markFragmentUploaded(int generation) {
        fragmentGeneration = generation;
        initialized |= FRAGMENT;
    }

    boolean needsColorUpload(int generation) {
        return needsUpload(COLOR, generation, colorGeneration);
    }

    void markColorUploaded(int generation) {
        colorGeneration = generation;
        initialized |= COLOR;
    }

    boolean needsNormalUpload(int generation) {
        return needsUpload(NORMAL, generation, normalGeneration);
    }

    void markNormalUploaded(int generation) {
        normalGeneration = generation;
        initialized |= NORMAL;
    }

    boolean needsTexCoordUpload(int generation) {
        return needsUpload(TEX_COORD, generation, texCoordGeneration);
    }

    void markTexCoordUploaded(int generation) {
        texCoordGeneration = generation;
        initialized |= TEX_COORD;
    }

    boolean needsTexGenUpload(int generation) {
        return needsUpload(TEX_GEN, generation, texGenGeneration);
    }

    void markTexGenUploaded(int generation) {
        texGenGeneration = generation;
        initialized |= TEX_GEN;
    }

    boolean needsClipPlaneUpload(int generation) {
        return needsUpload(CLIP_PLANE, generation, clipPlaneGeneration);
    }

    void markClipPlaneUploaded(int generation) {
        clipPlaneGeneration = generation;
        initialized |= CLIP_PLANE;
    }

    boolean needsLightmapUpload(float x, float y) {
        return !isInitialized(LIGHTMAP) || lightmapX != x || lightmapY != y;
    }

    void markLightmapUploaded(float x, float y) {
        lightmapX = x;
        lightmapY = y;
        initialized |= LIGHTMAP;
    }

    boolean needsLineWidthUpload(float width) {
        return !isInitialized(LINE_WIDTH) || lineWidth != width;
    }

    void markLineWidthUploaded(float width) {
        lineWidth = width;
        initialized |= LINE_WIDTH;
    }

    boolean needsViewportUpload(int width, int height) {
        return !isInitialized(VIEWPORT) || viewportWidth != width || viewportHeight != height;
    }

    void markViewportUploaded(int width, int height) {
        viewportWidth = width;
        viewportHeight = height;
        initialized |= VIEWPORT;
    }

    private boolean needsUpload(int category, int generation, int uploadedGeneration) {
        return !isInitialized(category) || uploadedGeneration != generation;
    }

    private boolean isInitialized(int category) {
        return (initialized & category) != 0;
    }
}
