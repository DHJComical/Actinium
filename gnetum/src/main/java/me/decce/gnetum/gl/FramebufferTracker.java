package me.decce.gnetum.gl;

public class FramebufferTracker {
    private static int currentlyBoundFbo;

    public static int getCurrentlyBoundFbo() {
        return currentlyBoundFbo;
    }

    public static void setCurrentlyBoundFbo(int currentlyBoundFbo) {
        FramebufferTracker.currentlyBoundFbo = currentlyBoundFbo;
    }
}