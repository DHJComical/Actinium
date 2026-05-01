package com.dhj.actinium.shader.pipeline;

enum ActiniumWorldStage {
    ENTITIES("gbuffers_entities"),
    PARTICLES("gbuffers_particles", "gbuffers_textured_lit", "gbuffers_textured", "gbuffers_basic"),
    SKY("gbuffers_skybasic"),
    SKY_TEXTURED("gbuffers_skytextured"),
    CLOUDS("gbuffers_clouds"),
    WEATHER("gbuffers_weather", "gbuffers_textured_lit", "gbuffers_textured", "gbuffers_basic");

    private final String programName;
    private final String[] fallbackProgramNames;

    ActiniumWorldStage(String programName) {
        this(programName, new String[0]);
    }

    ActiniumWorldStage(String programName, String... fallbackProgramNames) {
        this.programName = programName;
        this.fallbackProgramNames = fallbackProgramNames;
    }

    public String programName() {
        return this.programName;
    }

    public String[] fallbackProgramNames() {
        return this.fallbackProgramNames;
    }
}
