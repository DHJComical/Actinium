package com.dhj.actinium.shader.pipeline;

enum ActiniumWorldStage {
    SKY("gbuffers_skybasic"),
    SKY_TEXTURED("gbuffers_skytextured"),
    CLOUDS("gbuffers_clouds"),
    WEATHER("gbuffers_weather");

    private final String programName;

    ActiniumWorldStage(String programName) {
        this.programName = programName;
    }

    public String programName() {
        return this.programName;
    }
}
