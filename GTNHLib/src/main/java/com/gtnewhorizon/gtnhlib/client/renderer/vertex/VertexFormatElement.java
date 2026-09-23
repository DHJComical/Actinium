package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import java.util.function.IntConsumer;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers.IVertexAttributeWriter;

import lombok.Getter;

@Getter
public class VertexFormatElement {

    protected final Type type;
    protected final Usage usage;
    protected final int index;
    protected final int count;
    protected final int byteSize;
    @Beta
    protected final @NotNull IVertexAttributeWriter writer;
    protected final int vertexBit;

    public VertexFormatElement(int index, Type type, Usage usage, int count, int vertexBit,
            @NotNull IVertexAttributeWriter writer) {
        this(index, type, usage, count, vertexBit, writer, 0);
    }

    public VertexFormatElement(int index, Type type, Usage usage, int count, int vertexBit,
            @NotNull IVertexAttributeWriter writer, int padding) {
        this.index = index;
        this.type = type;
        this.usage = usage;
        this.count = count;
        this.byteSize = type.getSize() * count + padding;
        this.vertexBit = vertexBit;
        this.writer = writer;
    }

    public void setupBufferState(long offset, int stride) {
        this.usage.setupBufferState(this.count, this.type.getGlType(), stride, offset, this.index);
    }

    public void clearBufferState() {
        this.usage.clearBufferState(this.index);
    }

    public enum Usage {

        POSITION("Position", 0, false, (size, type, stride, pointer, index) -> {
            GL11.glVertexPointer(size, type, stride, pointer);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        }, index -> GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY)),
        NORMAL("Normal", 4, true, (size, type, stride, pointer, index) -> {
            GL11.glNormalPointer(type, stride, pointer);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        }, index -> GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY)),
        COLOR("Vertex Color", 1, true, (size, type, stride, pointer, index) -> {
            GL11.glColorPointer(size, type, stride, pointer);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        }, index -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY)),
        PRIMARY_UV("UV 0", 2, false, (size, type, stride, pointer, index) -> {
            GL11.glTexCoordPointer(size, type, stride, pointer);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        }, index -> GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY)),
        SECONDARY_UV("UV 1-31", 3, false, (size, type, stride, pointer, index) -> {
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + index);
            GL11.glTexCoordPointer(size, type, stride, pointer);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
        }, index -> {
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + index);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
        }),
        PADDING("Padding", -1, false, (size, type, stride, pointer, index) -> {}, index -> {}),
        GENERIC("Generic", -1, false, (size, type, stride, pointer, index) -> {
            GL20.glEnableVertexAttribArray(index);
            GL20.glVertexAttribPointer(index, size, type, false, stride, pointer);
        }, GL20::glDisableVertexAttribArray);

        @Getter
        private final String name;
        @Getter
        private final int attributeLocation;
        @Getter
        private final boolean normalized;
        private final SetupState setupState;
        private final IntConsumer clearState;

        Usage(String name, int attributeLocation, boolean normalized, SetupState setupState, IntConsumer clearState) {
            this.name = name;
            this.attributeLocation = attributeLocation;
            this.normalized = normalized;
            this.setupState = setupState;
            this.clearState = clearState;
        }

        private void setupBufferState(int size, int type, int stride, long pointer, int index) {
            this.setupState.setupBufferState(size, type, stride, pointer, index);
        }

        public void clearBufferState(int index) {
            this.clearState.accept(index);
        }

        /**
         * Resolves the generic vertex-attribute location for this usage at the given element
         * {@code index}. For the UV usages the {@code index} is the legacy texture unit the element
         * feeds (see {@link #setupBufferState}); the index-less usages ignore it. No two distinct
         * (usage, index) pairs resolve to the same location, so per-unit UV sets never collide
         * (locked by VertexAttributeLayoutTest).
         *
         * @param index element index (the legacy texture unit for UV usages; ignored elsewhere)
         * @return the attribute location in {@code [0, 15]}, or {@code -1} for {@link #PADDING}
         */
        public int getAttributeLocation(int index) {
            return switch (this) {
                // SECONDARY_UV feeds legacy texture units 1..3 (unit 0 is PRIMARY_UV), each with
                // its own slot. This table is the single source of truth for issue #175: the vanilla
                // vertex-format path used to resolve units 2/3 to -1 (no slot), so those coordinates
                // were never written and the FFP shader sampled a per-draw constant instead. Sharing
                // one table keeps the vertex-format path, the client-array path and both shader
                // generators from disagreeing about where a texture unit is read from.
                case SECONDARY_UV -> uvAttributeLocation(requireSecondaryUvUnit(index));
                // GENERIC binds straight to the caller-chosen slot.
                case GENERIC -> index;
                // POSITION/COLOR/NORMAL/PADDING/PRIMARY_UV are index-less; PRIMARY_UV is always
                // texture unit 0.
                default -> this.attributeLocation;
            };
        }

        /**
         * Maps a legacy texture unit (0..3) to its dedicated UV attribute location. Units 0/1 are
         * the primary-UV and lightmap slots (locations 2/3); units 2/3 are the extended
         * multi-texture UV slots (locations 5/6; location 4 is NORMAL). Shared by the GL texcoord
         * dispatch, the FFP/compat shader generators and the vertex-format layout so every consumer
         * agrees on one unit-to-location table.
         *
         * @param textureUnit legacy texture unit (0..3)
         * @return the attribute location in {@code [0, 15]}, or {@code -1} if the unit is unsupported
         */
        public static int uvAttributeLocation(int textureUnit) {
            return switch (textureUnit) {
                case 0 -> 2;
                case 1 -> 3;
                case 2 -> 5;
                case 3 -> 6;
                default -> -1;
            };
        }

        private static int requireSecondaryUvUnit(int index) {
            if (index < 1 || index > 3) {
                throw new IllegalArgumentException(
                    "SECONDARY_UV feeds legacy texture units 1..3 but got " + index + " (unit 0 is PRIMARY_UV)");
            }
            return index;
        }

        interface SetupState {

            void setupBufferState(int size, int type, int stride, long pointer, int index);
        }
    }

    @Getter
    public enum Type {

        FLOAT(4, "Float", GL11.GL_FLOAT),
        UBYTE(1, "Unsigned Byte", GL11.GL_UNSIGNED_BYTE),
        BYTE(1, "Byte", GL11.GL_BYTE),
        USHORT(2, "Unsigned Short", GL11.GL_UNSIGNED_SHORT),
        SHORT(2, "Short", GL11.GL_SHORT),
        UINT(4, "Unsigned Int", GL11.GL_UNSIGNED_INT),
        INT(4, "Int", GL11.GL_INT);

        private final int size;
        private final String name;
        private final int glType;

        Type(int size, String name, int glType) {
            this.size = size;
            this.name = name;
            this.glType = glType;
        }
    }
}
