package com.gtnewhorizons.angelica.glsm.loading;

import java.util.List;

public final class DependencyVerifier {

    public record Check(String resourcePath, String errorMessage) {}

    public static void verify(Class<?> anchor, List<Check> checks) {
        for (Check check : checks) {
            if (anchor.getResource(check.resourcePath()) == null) {
                throw new RuntimeException(check.errorMessage());
            }
        }
    }

    private DependencyVerifier() {}
}
