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

        GnetumDebug.log("pass-begin current={} passText={} numberOfPasses={} maxFps={} rendering={} renderingCanceled={}",
                current,
                getPassText(),
                Gnetum.config.numberOfPasses,
                Gnetum.config.maxFps,
                Gnetum.rendering,
                Gnetum.renderingCanceled);
        Minecraft.getMinecraft().profiler.startSection(getPassText());
    }

    public void end() {
        Minecraft.getMinecraft().profiler.endSection();

        if (current > 0) {
            long elapsed = Gnetum.getTimeSource().getNanos() - passBeginNanos;
            currentPassDuration += elapsed;
            GnetumDebug.log("pass-end current={} elapsed={}ns accumulated={}ns", current, elapsed, currentPassDuration);
        }
        else {
            GnetumDebug.log("pass-end current=0 skipped");
        }
    }

    public void nextPass() {
        long nanos = Gnetum.getTimeSource().getNanos();
        int before = current;

        if (current == 0) {
            if (Gnetum.config.maxFps == GnetumConfig.UNLIMITED_FPS || nanos <= Gnetum.lastSwapNanos || nanos - Gnetum.lastSwapNanos >= NANOS_IN_A_SECOND / Gnetum.config.maxFps) {
                current = 1;
                FramebufferManager.getInstance().swapFramebuffers();
                Gnetum.lastSwapNanos = nanos;
                GnetumDebug.log("pass-next sleep->render before={} after={} swap=true lastSwapNanos={} nanos={}", before, current, Gnetum.lastSwapNanos, nanos);
            }
            else {
                GnetumDebug.log("pass-next sleep->sleep before={} after={} swap=false lastSwapNanos={} nanos={}", before, current, Gnetum.lastSwapNanos, nanos);
            }
        }
        else {
            if (durations == null || durations.length != Gnetum.config.numberOfPasses + 1) {
                durations = new long[Gnetum.config.numberOfPasses + 1][SAVED_DURATIONS];
            }
            long recordedDuration = currentPassDuration;
            durations[current][index] = recordedDuration;
            currentPassDuration = 0L;
            if (current == Gnetum.config.numberOfPasses) index++;
            if (index == SAVED_DURATIONS) index = 0;

            if (current++ == Gnetum.config.numberOfPasses) {
                if (Gnetum.config.maxFps != GnetumConfig.UNLIMITED_FPS && nanos > Gnetum.lastSwapNanos && nanos - Gnetum.lastSwapNanos < NANOS_IN_A_SECOND / Gnetum.config.maxFps) {
                    current = 0;
                    GnetumDebug.log("pass-next render->sleep before={} after={} swap=false duration={} index={} lastSwapNanos={} nanos={}",
                            before, current, recordedDuration, index, Gnetum.lastSwapNanos, nanos);
                }
                else {
                    current = 1;
                    FramebufferManager.getInstance().swapFramebuffers();
                    Gnetum.lastSwapNanos = nanos;
                    GnetumDebug.log("pass-next render->render before={} after={} swap=true duration={} index={} lastSwapNanos={} nanos={}",
                            before, current, recordedDuration, index, Gnetum.lastSwapNanos, nanos);
                }
            }
            else {
                GnetumDebug.log("pass-next render->render before={} after={} swap=false duration={} index={} lastSwapNanos={} nanos={}",
                        before, current, recordedDuration, index, Gnetum.lastSwapNanos, nanos);
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
            GnetumDebug.log("shouldRender-vanilla id={} current={} renderingCanceled=true result=false", vanillaOverlay, current);
            return false;
        }
        boolean uncached = Gnetum.uncachedElements.has(vanillaOverlay);
        if (uncached) {
            GnetumDebug.log("shouldRender-vanilla id={} current={} uncached=true result=false", vanillaOverlay, current);
            return false;
        }
        CacheSetting cacheSetting = Gnetum.getCacheSetting(vanillaOverlay);
        boolean result = cacheSetting.enabled.get() && current == cacheSetting.pass;
        GnetumDebug.log("shouldRender-vanilla id={} current={} pass={} enabled={} uncached={} renderingCanceled={} result={}",
                vanillaOverlay,
                current,
                cacheSetting.pass,
                cacheSetting.enabled.get(),
                uncached,
                Gnetum.renderingCanceled,
                result);
        return result;
    }

    public boolean cachingDisabled(String vanillaOverlay) {
        if (Gnetum.uncachedElements.has(vanillaOverlay)) {
            GnetumDebug.log("cachingDisabled-vanilla id={} uncached=true result=true", vanillaOverlay);
            return true;
        }
        boolean result = !Gnetum.getCacheSetting(vanillaOverlay).enabled.get();
        GnetumDebug.log("cachingDisabled-vanilla id={} uncached=false result={}", vanillaOverlay, result);
        return result;
    }

    public boolean shouldRender(String moddedOverlay, ElementType type) {
        boolean uncached = Gnetum.uncachedElements.has(moddedOverlay, type);
        if (uncached) {
            GnetumDebug.log("shouldRender-modded id={} type={} current={} uncached=true result=false", moddedOverlay, type, current);
            return false;
        }
        CacheSetting cacheSetting = Gnetum.getCacheSetting(moddedOverlay, type);
        boolean result = cacheSetting.enabled.get() && current == cacheSetting.pass;
        GnetumDebug.log("shouldRender-modded id={} type={} current={} pass={} enabled={} uncached={} result={}",
                moddedOverlay,
                type,
                current,
                cacheSetting.pass,
                cacheSetting.enabled.get(),
                uncached,
                result);
        return result;
    }

    public boolean cachingDisabled(String moddedOverlay, ElementType type) {
        if (Gnetum.uncachedElements.has(moddedOverlay, type)) {
            GnetumDebug.log("cachingDisabled-modded id={} type={} uncached=true result=true", moddedOverlay, type);
            return true;
        }
        boolean result = !Gnetum.getCacheSetting(moddedOverlay, type).enabled.get();
        GnetumDebug.log("cachingDisabled-modded id={} type={} uncached=false result={}", moddedOverlay, type, result);
        return result;
    }
}
