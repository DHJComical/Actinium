package com.dhj.actinium.shader.pipeline;

import org.embeddedt.embeddium.impl.gl.shader.GlProgram;

final class ActiniumWorldProgram {
    private final String name;
    private final GlProgram<ActiniumWorldShaderInterface> program;
    private final int[] drawBuffers;

    ActiniumWorldProgram(String name, GlProgram<ActiniumWorldShaderInterface> program, int[] drawBuffers) {
        this.name = name;
        this.program = program;
        this.drawBuffers = drawBuffers;
    }

    public String name() {
        return this.name;
    }

    public GlProgram<ActiniumWorldShaderInterface> program() {
        return this.program;
    }

    public int[] drawBuffers() {
        return this.drawBuffers;
    }
}
