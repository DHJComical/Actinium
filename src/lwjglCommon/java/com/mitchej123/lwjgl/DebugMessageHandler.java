package com.mitchej123.lwjgl;

@FunctionalInterface
public interface DebugMessageHandler {
    void handle(int source, int type, int id, int severity, String message, DebugExtension extension);
}
