package com.gtnewhorizons.angelica;

/**
 * Build-time constants. {@link #VERSION} mirrors the build version (git tag or commit sha)
 * injected via the jar manifest's Implementation-Version attribute.
 */
public final class Tags {
    public static final String VERSION = readVersion();

    private static String readVersion() {
        final Package pkg = Tags.class.getPackage();
        final String implementationVersion = pkg != null ? pkg.getImplementationVersion() : null;
        return implementationVersion != null ? implementationVersion : "0.0.0-actinium";
    }

    private Tags() {
    }
}
