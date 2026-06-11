package me.decce.gnetum;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PerformanceAnalyzer {
    private final static double DURATION_TOO_LONG_CRITERIA = 1.6d;
    public static Result latestAnalysisResult;

    public static Result analyze() {
        Result result = new Result();
        analyzePass(result);
        analyzeDurations(result);
        latestAnalysisResult = result;
        return result;
    }

    private static void analyzePass(Result result) {
        boolean[] anyElement = new boolean[Gnetum.config.numberOfPasses + 1];
        Gnetum.config.mapVanillaElements.forEach((key, value) -> {
            if (value.enabled.get()) anyElement[value.pass] = true;
        });
        Gnetum.config.mapModdedElementsPre.forEach((key, value) -> {
            if (value.enabled.get()) anyElement[value.pass] = true;
        });
        Gnetum.config.mapModdedElementsPost.forEach((key, value) -> {
            if (value.enabled.get()) anyElement[value.pass] = true;
        });
        for (int i = 1; i < anyElement.length; i++) {
            if (!anyElement[i]) {
                result.messages.add(I18n.format("gnetum.config.analysis.noElement", i));
            }
        }
    }

    private static void analyzeDurations(Result result) {
        var durations = Gnetum.passManager.getDurations();
        if (durations == null) return;
        long total = Arrays.stream(durations).skip(1).sum();
        double avg = total * 1.0d / Gnetum.config.numberOfPasses;
        for (int i = 1; i < durations.length; i++) {
            if (durations[i] * 1.0d >= avg * DURATION_TOO_LONG_CRITERIA) {
                // do not warn if this pass is already only rendering one element
                long count = 0;
                int temp = i;
                count += Gnetum.config.mapModdedElementsPre.entrySet().stream().filter(entry -> entry.getValue().enabled.get() && entry.getValue().pass == temp).count();
                if (count <= 1) count += Gnetum.config.mapVanillaElements.entrySet().stream().filter(entry -> entry.getValue().enabled.get() && entry.getValue().pass == temp).count();
                if (count <= 1) count += Gnetum.config.mapModdedElementsPost.entrySet().stream().filter(entry -> entry.getValue().enabled.get() && entry.getValue().pass == temp).count();
                if (count <= 1) continue;
                result.messages.add(I18n.format("gnetum.config.analysis.longDuration", i));
            }
        }
        result.durations = durations;
    }

    public static class Result {
        private final List<String> messages;
        private long[] durations;
        private boolean outdated;

        private Result() {
            messages = new ArrayList<>(3);
        }

        public ResultIcon getIcon() {
            return outdated ? ResultIcon.QUESTION : (messages.isEmpty() ? ResultIcon.TICK : ResultIcon.EXCLAMATION);
        }

        public List<String> getMessages() {
            return messages;
        }

        public long[] getDurations() {
            return durations;
        }

        public boolean isOutdated() {
            return outdated;
        }

        public void markOutdated() {
            this.outdated = true;
        }
    }

    public enum ResultIcon {
        TICK,
        EXCLAMATION,
        QUESTION;

        public ResourceLocation icon() {
            return switch (this) {
                case TICK -> Icons.TICK;
                case EXCLAMATION -> Icons.EXCLAMATION;
                case QUESTION -> Icons.QUESTION;
            };
        }
    }
}
