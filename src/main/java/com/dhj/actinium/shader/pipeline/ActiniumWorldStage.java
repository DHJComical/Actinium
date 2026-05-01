package com.dhj.actinium.shader.pipeline;

import org.jetbrains.annotations.Nullable;

enum ActiniumWorldStage {
    ENTITIES("gbuffers_entities"),
    SKY("gbuffers_skybasic"),
    SKY_TEXTURED("gbuffers_skytextured"),
    CLOUDS("gbuffers_clouds"),
    WEATHER("gbuffers_weather", "gbuffers_textured_lit");

    private final String programName;
    private final @Nullable String fallbackProgramName;

    ActiniumWorldStage(String programName) {
        this(programName, null);
    }

    ActiniumWorldStage(String programName, @Nullable String fallbackProgramName) {
        this.programName = programName;
        this.fallbackProgramName = fallbackProgramName;
    }

    public String programName() {
        return this.programName;
    }

    public @Nullable String fallbackProgramName() {
        return this.fallbackProgramName;
    }
}
