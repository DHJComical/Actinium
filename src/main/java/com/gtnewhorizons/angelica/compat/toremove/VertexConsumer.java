package com.gtnewhorizons.angelica.compat.toremove;

import javax.annotation.Nonnull;

public interface VertexConsumer {
    VertexConsumer vertex(double x, double y, double z);

    @Nonnull
    VertexConsumer color(int r, int g, int b, int a);

    @Nonnull
    VertexConsumer texture(float u, float v);

    @Nonnull
    VertexConsumer overlay(int u, int v);

    @Nonnull
    VertexConsumer light(int u, int v);

    @Nonnull
    VertexConsumer normal(float x, float y, float z);

    void next();
}
