package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ActiniumRenderPipeline {
    public static final ActiniumRenderPipeline INSTANCE = new ActiniumRenderPipeline();

    private static final String[] SKY_PROGRAMS = {
            "gbuffers_skybasic",
            "gbuffers_skytextured",
            "gbuffers_clouds"
    };

    private static final String[] WEATHER_PROGRAMS = {
            "gbuffers_weather"
    };

    private static final String[] POST_PROGRAMS = {
            "prepare",
            "deferred",
            "composite",
            "composite1",
            "composite2",
            "composite3",
            "composite4",
            "composite5",
            "composite6",
            "composite7",
            "final"
    };

    private int observedReloadVersion = -1;
    private ActiniumRenderStage currentStage = ActiniumRenderStage.NONE;
    private boolean shadowProgramAvailable;
    private boolean skyProgramAvailable;
    private boolean weatherProgramAvailable;
    private boolean postProgramAvailable;
    private boolean loggedCapabilities;

    private ActiniumRenderPipeline() {
    }

    public void beginWorld() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.WORLD;
    }

    public void endWorld() {
        this.currentStage = ActiniumRenderStage.NONE;
    }

    public void beginSky() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.SKY;
    }

    public void endSky() {
        if (this.currentStage == ActiniumRenderStage.SKY) {
            this.currentStage = ActiniumRenderStage.WORLD;
        }
    }

    public void beginClouds() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.CLOUDS;
    }

    public void endClouds() {
        if (this.currentStage == ActiniumRenderStage.CLOUDS) {
            this.currentStage = ActiniumRenderStage.WORLD;
        }
    }

    public void beginWeather() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.WEATHER;
    }

    public void endWeather() {
        if (this.currentStage == ActiniumRenderStage.WEATHER) {
            this.currentStage = ActiniumRenderStage.WORLD;
        }
    }

    public void beginPost() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.POST;
    }

    public void endPost() {
        if (this.currentStage == ActiniumRenderStage.POST) {
            this.currentStage = ActiniumRenderStage.NONE;
        }
    }

    public ActiniumRenderStage getCurrentStage() {
        return this.currentStage;
    }

    public boolean hasShadowProgram() {
        this.syncReloadState();
        return this.shadowProgramAvailable;
    }

    public boolean hasSkyProgram() {
        this.syncReloadState();
        return this.skyProgramAvailable;
    }

    public boolean hasWeatherProgram() {
        this.syncReloadState();
        return this.weatherProgramAvailable;
    }

    public boolean hasPostProgram() {
        this.syncReloadState();
        return this.postProgramAvailable;
    }

    private void syncReloadState() {
        int reloadVersion = ActiniumShaderPackManager.getReloadVersion();

        if (this.observedReloadVersion == reloadVersion) {
            return;
        }

        this.observedReloadVersion = reloadVersion;
        this.currentStage = ActiniumRenderStage.NONE;
        this.shadowProgramAvailable = hasAnyStageProgram("shadow");
        this.skyProgramAvailable = hasAnyStageProgram(SKY_PROGRAMS);
        this.weatherProgramAvailable = hasAnyStageProgram(WEATHER_PROGRAMS);
        this.postProgramAvailable = hasAnyStageProgram(POST_PROGRAMS);
        this.loggedCapabilities = false;
        this.logCapabilities();
    }

    private void logCapabilities() {
        if (this.loggedCapabilities || !ActiniumShaderPackManager.areShadersEnabled()) {
            return;
        }

        this.loggedCapabilities = true;
        Set<String> capabilities = new LinkedHashSet<>();

        if (this.shadowProgramAvailable) {
            capabilities.add("shadow");
        }

        if (this.skyProgramAvailable) {
            capabilities.add("sky");
        }

        if (this.weatherProgramAvailable) {
            capabilities.add("weather");
        }

        if (this.postProgramAvailable) {
            capabilities.add("post");
        }

        if (capabilities.isEmpty()) {
            ActiniumShaders.logger().info("Active shader pack does not expose shadow/sky/weather/post programs yet");
        } else {
            ActiniumShaders.logger().info("Active shader pack stage programs detected: {}", String.join(", ", capabilities));
        }
    }

    private static boolean hasAnyStageProgram(String... programNames) {
        for (String programName : programNames) {
            if (hasProgram(programName)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasProgram(String programName) {
        return ActiniumShaderPackManager.getProgramSource(programName, ShaderType.VERTEX) != null
                || ActiniumShaderPackManager.getProgramSource(programName, ShaderType.FRAGMENT) != null;
    }
}
