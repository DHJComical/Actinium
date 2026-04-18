package com.dhj.actinium.shader.pipeline;

import org.embeddedt.embeddium.impl.gl.shader.GlProgram;

final class ActiniumPostProgram {
    private final String name;
    private final GlProgram<ActiniumPostShaderInterface> program;
    private final int[] drawBuffers;

    ActiniumPostProgram(String name, GlProgram<ActiniumPostShaderInterface> program, int[] drawBuffers) {
        this.name = name;
        this.program = program;
        this.drawBuffers = drawBuffers;
    }

    public String name() {
        return this.name;
    }

    public GlProgram<ActiniumPostShaderInterface> program() {
        return this.program;
    }

    public int[] drawBuffers() {
        return this.drawBuffers;
    }
}
