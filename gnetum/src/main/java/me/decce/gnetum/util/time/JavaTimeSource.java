package me.decce.gnetum.util.time;

public class JavaTimeSource implements TimeSource {
    @Override
    public long getNanos() {
        return System.nanoTime();
    }
}
