package me.decce.gnetum;

import net.minecraft.client.Minecraft;

public class PassManager {
    public static final long NANOS_IN_A_SECOND = 1_000_000_000L;
    private final int SAVED_DURATIONS = 30; // specifies how many previous frames should be used for analysis of time taken on each pass
    private String[] PASS_TEXT;
    private long[][] durations;
    private long currentPassDuration;
    private int index;
    private long passBeginNanos;
    public int current = 1; // Range: [1, numberOfPasses]

    public String getPassText() {
        if (PASS_TEXT == null || PASS_TEXT.length != Gnetum.config.numberOfPasses + 1) {
            PASS_TEXT = new String[Gnetum.config.numberOfPasses + 1];
            PASS_TEXT[0] = "sleep";
            for (int i = 1; i <= Gnetum.config.numberOfPasses; i++) {
                PASS_TEXT[i] = "pass" + i;
            }
        }
        return PASS_TEXT[current];
    }

    public void begin() {
        if (current > Gnetum.config.numberOfPasses) current = 1;

        if (current > 0) {
            passBeginNanos = Gnetum.getTimeSource().getNanos();
        }

        Minecraft.getMinecraft().profiler.startSection(getPassText());
    }

    public void end() {
        Minecraft.getMinecraft().profiler.endSection();

        if (current > 0) {
            currentPassDuration += Gnetum.getTimeSource().getNanos() - passBeginNanos;
        }
    }

    public void nextPass() {
        long nanos = Gnetum.getTimeSource().getNanos();

        if (current == 0) {
            if (Gnetum.config.maxFps == GnetumConfig.UNLIMITED_FPS || nanos <= Gnetum.lastSwapNanos || nanos - Gnetum.lastSwapNanos >= NANOS_IN_A_SECOND / Gnetum.config.maxFps) {
                current = 1;
                FramebufferManager.getInstance().swapFramebuffers();
                Gnetum.lastSwapNanos = nanos;
            }
        }
        else {
            if (durations == null || durations.length != Gnetum.config.numberOfPasses + 1) {
                durations = new long[Gnetum.config.numberOfPasses + 1][SAVED_DURATIONS];
            }
            durations[current][index] = currentPassDuration;
            currentPassDuration = 0L;
            if (current == Gnetum.config.numberOfPasses) index++;
            if (index == SAVED_DURATIONS) index = 0;

            if (current++ == Gnetum.config.numberOfPasses) {
                if (Gnetum.config.maxFps != GnetumConfig.UNLIMITED_FPS && nanos > Gnetum.lastSwapNanos && nanos - Gnetum.lastSwapNanos < NANOS_IN_A_SECOND / Gnetum.config.maxFps) {
                    current = 0;
                }
                else {
                    current = 1;
                    FramebufferManager.getInstance().swapFramebuffers();
                    Gnetum.lastSwapNanos = nanos;
                }
            }
        }
    }

    public long[] getDurations() {
        if (durations == null) return null;
        long[] ret = new long[durations.length];
        for (int i = 1; i < durations.length; i++) {
            long avg = 0;
            int t = 1;
            for (int j = 0; j < SAVED_DURATIONS; j++) {
                if (durations[i][j] <= 0L) continue;
                avg += (durations[i][j] - avg) / t++;
            }
            ret[i] = avg;
        }
        return ret;
    }

    public boolean shouldRender(String vanillaOverlay) {
        if (Gnetum.renderingCanceled) {
            return false;
        }
        if (Gnetum.uncachedElements.has(vanillaOverlay)) {
            return false;
        }
        CacheSetting cacheSetting = Gnetum.getCacheSetting(vanillaOverlay);
        return cacheSetting.enabled.get() && current == cacheSetting.pass;
    }

    public boolean cachingDisabled(String vanillaOverlay) {
        if (Gnetum.uncachedElements.has(vanillaOverlay)) {
            return true;
        }
        return !Gnetum.getCacheSetting(vanillaOverlay).enabled.get();
    }

    public boolean shouldRender(String moddedOverlay, ElementType type) {
        if (Gnetum.uncachedElements.has(moddedOverlay, type)) {
            return false;
        }
        CacheSetting cacheSetting = Gnetum.getCacheSetting(moddedOverlay, type);
        return cacheSetting.enabled.get() && current == cacheSetting.pass;
    }

    public boolean cachingDisabled(String moddedOverlay, ElementType type) {
        if (Gnetum.uncachedElements.has(moddedOverlay, type)) {
            return true;
        }
        return !Gnetum.getCacheSetting(moddedOverlay, type).enabled.get();
    }
}