package com.dhj.actinium.shader.pack;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public record ActiniumShaderPack(String name, @Nullable Path path, boolean builtin) {
}
